# WesCreation UPI Watcher (Android)

Companion app for the **WesCreation UPI for WooCommerce** plugin (v1.9.13+).
It watches the phone that receives your bank's "credited" SMS (and, optionally,
PhonePe / GPay / Paytm "received" notifications) and sends only those
messages to your store's SMS webhook, so the matching on-hold order is approved
automatically.

## Build the APK (Android Studio)
1. Install Android Studio (Koala or newer).
2. File → Open → choose this folder. Let Gradle sync finish (needs internet once).
3. Build → Build Bundle(s) / APK(s) → Build APK(s).
4. APK: `app/build/outputs/apk/debug/app-debug.apk` → copy to the phone and install.

## Set up
1. Plugin: WooCommerce → Settings → Payments → Manual UPI Payment →
   tick **Enable SMS Auto-Detect** → Save → copy the **Webhook URL**.
2. App: paste URL → **Send test** (should say "Connected").
3. Tap **Allow SMS access**. Optional: enter your bank's sender, e.g. `HDFCBK`.
4. Tap **Turn off battery optimisation** (important on Xiaomi / Oppo / Vivo / Realme: also allow Autostart).
5. Save. Place a small test order and pay it.

### Optional: UPI app notifications
Turn on the switch, then **Open notification access settings** and enable UPI Watcher.
On Android 13+ for a sideloaded app: Settings → Apps → UPI Watcher → ⋮ → **Allow restricted settings** first.
Default packages: PhonePe, Google Pay, Paytm, BHIM. Add others (e.g. business apps) by package name.

## No Android Studio? Build online (GitHub Actions)
1. Create a free account at github.com → New repository (Private) → create it.
2. Upload the *contents* of this folder (settings.gradle.kts must be at the repo root, plus the hidden `.github` folder).
3. Open the **Actions** tab → "Build APK" → **Run workflow**.
4. After ~5 minutes open the finished run → **Artifacts** → download `UPI-Watcher-APK` (zip containing app-debug.apk).
