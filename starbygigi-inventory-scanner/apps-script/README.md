# Apps Script backend — deployment steps

This turns a Google Sheet into a tiny JSON API the Android app can call.

1. **Open (or create) the Google Sheet** you want to use as your inventory database.
2. In the Sheet, go to **Extensions → Apps Script**. This opens the script editor already bound to
   your sheet.
3. Delete the default `Code.gs` contents and paste in the contents of this folder's `Code.gs`.
4. (Optional but recommended) Click the gear icon → project settings, and in the "Show 'appsscript.json'
   manifest file" checkbox, enable it, then replace its contents with this folder's `appsscript.json`.
5. **Set the secret token:**
   - In the script editor, click the gear icon (**Project Settings**).
   - Scroll to **Script properties** → **Add script property**.
   - Key: `API_TOKEN`, Value: any long random string (e.g. generate one with `openssl rand -hex 16`).
   - Save. You'll paste this same value into the Android app's settings screen.
6. **Deploy as a Web App:**
   - Click **Deploy → New deployment**.
   - Click the gear next to "Select type" and choose **Web app**.
   - Description: anything, e.g. "StarByGiGi inventory API".
   - Execute as: **Me**.
   - Who has access: **Anyone**. (This is what lets the phone call it without a Google sign-in flow —
     the `API_TOKEN` is what protects it instead. See the security note in the main README.)
   - Click **Deploy**, authorize the requested permissions (it needs to read/write the spreadsheet),
     and copy the **Web app URL** it gives you — it looks like
     `https://script.google.com/macros/s/XXXXXXXX/exec`.
7. Paste the Web app URL and the `API_TOKEN` value into the Android app's settings screen (gear icon
   on the scanner screen, on first launch it will prompt you automatically).

## Re-deploying after editing `Code.gs`

Google Apps Script Web Apps are versioned — editing the script does **not** automatically update the
live Web App. After changing `Code.gs`, go to **Deploy → Manage deployments**, click the pencil/edit
icon on your existing deployment, and choose **New version** under "Version", then **Deploy**. The
Web App URL stays the same, so you don't need to update the Android app.

## Testing it without the app

You can sanity-check the deployment with `curl` once it's live:

```bash
# Look up a barcode (replace URL/TOKEN)
curl "https://script.google.com/macros/s/XXXXXXXX/exec?action=lookup&token=YOUR_TOKEN&barcode=012345678905"

# Create a new item
curl -X POST "https://script.google.com/macros/s/XXXXXXXX/exec" \
  -H "Content-Type: application/json" \
  -d '{"action":"create","token":"YOUR_TOKEN","barcode":"012345678905","name":"Edge Control 4oz","price":8.99}'

# Update an existing item's price
curl -X POST "https://script.google.com/macros/s/XXXXXXXX/exec" \
  -H "Content-Type: application/json" \
  -d '{"action":"update","token":"YOUR_TOKEN","barcode":"012345678905","price":9.49}'
```
