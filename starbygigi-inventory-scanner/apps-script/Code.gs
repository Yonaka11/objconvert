/**
 * StarByGiGi Inventory Scanner — Apps Script backend.
 *
 * Bind this script to the Google Sheet you want to use as the inventory database, then deploy it
 * as a Web App (see apps-script/README.md for step-by-step instructions).
 *
 * Endpoints (all requests carry `token` which must match the API_TOKEN script property):
 *
 *   GET  ?action=lookup&token=...&barcode=...
 *     -> { found: false }
 *     -> { found: true, barcode, name, price, updatedAt, row }
 *
 *   POST body: { action: "create", token, barcode, name, price }
 *     -> { success: true, barcode, name, price }
 *
 *   POST body: { action: "update", token, barcode, price }
 *     -> { success: true, barcode, price }
 */

var SHEET_NAME = 'Inventory';
var HEADERS = ['Barcode', 'Name', 'Price', 'UpdatedAt'];

function doGet(e) {
  try {
    var params = (e && e.parameter) || {};
    checkToken_(params.token);

    var action = params.action;
    if (action === 'lookup') {
      return jsonResponse_(lookupBarcode_(params.barcode));
    }

    return jsonResponse_({ error: 'Unknown or missing action for GET request.' });
  } catch (err) {
    return jsonResponse_({ error: String(err && err.message ? err.message : err) });
  }
}

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);

    var body = parseBody_(e);
    checkToken_(body.token);

    var action = body.action;
    if (action === 'create') {
      return jsonResponse_(createItem_(body));
    }
    if (action === 'update') {
      return jsonResponse_(updateItem_(body));
    }

    return jsonResponse_({ error: 'Unknown or missing action for POST request.' });
  } catch (err) {
    return jsonResponse_({ error: String(err && err.message ? err.message : err) });
  } finally {
    try { lock.releaseLock(); } catch (ignored) {}
  }
}

function lookupBarcode_(barcode) {
  if (!barcode) throw new Error('Missing "barcode" parameter.');

  var sheet = getSheet_();
  var data = sheet.getDataRange().getValues();

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() === String(barcode).trim()) {
      return {
        found: true,
        row: i + 1,
        barcode: data[i][0],
        name: data[i][1],
        price: data[i][2],
        updatedAt: data[i][3]
      };
    }
  }

  return { found: false, barcode: barcode };
}

function createItem_(body) {
  if (!body.barcode) throw new Error('Missing "barcode".');
  if (body.price === undefined || body.price === null || body.price === '') {
    throw new Error('Missing "price".');
  }

  var sheet = getSheet_();
  var existing = lookupBarcode_(body.barcode);
  if (existing.found) {
    throw new Error('Barcode already exists on the sheet — use action "update" instead.');
  }

  var price = Number(body.price);
  if (isNaN(price)) throw new Error('"price" must be a number.');

  var name = body.name ? String(body.name) : '';
  var now = new Date();

  sheet.appendRow([String(body.barcode), name, price, now]);

  return { success: true, barcode: body.barcode, name: name, price: price };
}

function updateItem_(body) {
  if (!body.barcode) throw new Error('Missing "barcode".');
  if (body.price === undefined || body.price === null || body.price === '') {
    throw new Error('Missing "price".');
  }

  var price = Number(body.price);
  if (isNaN(price)) throw new Error('"price" must be a number.');

  var existing = lookupBarcode_(body.barcode);
  if (!existing.found) {
    throw new Error('Barcode not found — use action "create" instead.');
  }

  var sheet = getSheet_();
  var now = new Date();
  sheet.getRange(existing.row, 3).setValue(price);
  sheet.getRange(existing.row, 4).setValue(now);

  return { success: true, barcode: body.barcode, price: price };
}

function getSheet_() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(SHEET_NAME);

  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
  }

  if (sheet.getLastRow() === 0) {
    sheet.appendRow(HEADERS);
    sheet.setFrozenRows(1);
  }

  return sheet;
}

function checkToken_(token) {
  var expected = PropertiesService.getScriptProperties().getProperty('API_TOKEN');
  if (!expected) {
    throw new Error(
      'API_TOKEN script property is not set. Open Project Settings in the Apps Script editor and add it.'
    );
  }
  if (!token || token !== expected) {
    throw new Error('Invalid or missing token.');
  }
}

function parseBody_(e) {
  if (!e || !e.postData || !e.postData.contents) {
    throw new Error('Missing POST body.');
  }
  try {
    return JSON.parse(e.postData.contents);
  } catch (err) {
    throw new Error('POST body must be valid JSON.');
  }
}

function jsonResponse_(payload) {
  // Apps Script Web Apps always answer with HTTP 200; callers must check the JSON body
  // for an "error" field (or "found"/"success") to know what happened.
  var output = ContentService.createTextOutput(JSON.stringify(payload));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}
