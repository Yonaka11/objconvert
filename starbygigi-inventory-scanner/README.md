# StarByGiGi Inventory Scanner

A quick Android app + Google Sheets backend for compiling store inventory data by scanning barcodes.

> **Note on where this lives:** this was built inside the `objconvert` repository because that is the
> repo this Cloud Agent session had write access to — the `Yonaka11/StarbyGiGi` repo was not linked to
> this agent run, so it could not push there directly. Everything needed is self-contained in this
> `starbygigi-inventory-scanner/` folder. Copy (or `git mv`) this whole folder into the `StarbyGiGi`
> repository, or re-run a Cloud Agent task from inside that repo and point it at this PR/branch to move
> the code over.

## How it works

1. You open the Android app and point the camera at a product barcode.
2. The app looks up the barcode in a Google Sheet (via a small Google Apps Script web app that acts as
   the API).
3. **If the barcode is not on the sheet yet:** the app asks you to type in the price, then saves the
   barcode + price (and an optional name) as a new row.
4. **If the barcode already exists:** the app shows "Item exists — current price $X. Update price?" and,
   if you say yes, asks for the new price and updates that row.
5. You keep scanning — the app returns to the camera view after every save so you can plow through a
   shelf of items quickly.

```
 ┌────────────┐   scan barcode    ┌────────────────────┐   HTTPS (JSON)   ┌───────────────┐
 │  Android   │ ────────────────▶ │ Google Apps Script  │ ───────────────▶ │  Google Sheet │
 │  app       │ ◀──────────────── │ Web App (Code.gs)   │ ◀─────────────── │  (Inventory)  │
 └────────────┘   found/price     └────────────────────┘   read/write row  └───────────────┘
```

## Project layout

- `apps-script/` — the Google Apps Script backend that reads/writes the Google Sheet. This is the
  "server" side; deploy it once as a Web App bound to your sheet.
- `android-app/` — a standalone Android Studio (Kotlin) project. Scans barcodes with CameraX + ML Kit
  and talks to the Apps Script Web App over HTTPS.

## Setup order

1. **Deploy the Sheets backend** — follow `apps-script/README.md`. You'll end up with a Web App URL and
   a secret token.
2. **Open `android-app/` in Android Studio**, let it sync, and run it on a device (camera access is
   needed, so an emulator with a webcam passthrough or a real phone works best). Or grab a prebuilt APK
   — see below.
3. **On first launch**, tap the gear/settings icon and paste in the Web App URL + token from step 1.
4. Start scanning.

## Getting an APK without Android Studio

A GitHub Actions workflow (`.github/workflows/android-apk.yml`) builds a debug APK automatically on
every push/PR that touches `android-app/`, and can also be run manually:

1. Go to the repo's **Actions** tab → **Build StarByGiGi Scanner APK** → **Run workflow** (or just wait
   for it to run on a push).
2. Once the run finishes, open it and download the **starbygigi-scanner-debug-apk** artifact from the
   run summary.
3. Unzip it, copy `app-debug.apk` to your phone, and install it (you'll need to allow "install from
   unknown sources" since this isn't a Play Store build).

This is a debug build (unsigned, not optimized) — fine for personal/in-store use, but not something to
publish to the Play Store as-is.

## Data model

The sheet uses one tab (default name `Inventory`) with these columns:

| Barcode | Name | Price | UpdatedAt |
|---|---|---|---|

The backend creates this header row automatically the first time it runs if the sheet is empty.

## Security note

The Web App is deployed with "Execute as: Me" / "Who has access: Anyone", which is what lets a phone
call it without doing a full Google OAuth flow. The shared token in `Code.gs` (`API_TOKEN` script
property) is the only thing standing between "anyone with the URL" and your sheet, so:

- Treat the Web App URL + token like a password — don't post them publicly.
- Rotate the `API_TOKEN` script property any time you suspect it's leaked (Project Settings → Script
  properties in the Apps Script editor).
