# Dragons — WebView Android untuk BOTTOAI / layanan web lokal

App Android (Cordova) dengan layar awal berupa form "Alamat server" (persis
seperti mockup): masukkan `http://IP-LAN:PORT`, tekan **Hubungkan**, app akan
menavigasi penuh ke alamat itu di dalam WebView yang sama.

## Fitur

- **Layar awal (config screen)** — logo, input alamat server (tersimpan otomatis
  di `localStorage` HP lewat tombol Hubungkan), tombol **Reset** buat
  mengosongkan/menghapus alamat tersimpan.
- **Navigasi in-app** — begitu terhubung, seluruh browsing (link, redirect, dst)
  tetap di dalam app, tidak pernah dilempar ke browser luar (`allow-navigation
  href="*"` + `singleTask` launch mode). Tombol back HP otomatis mundur di
  riwayat WebView dulu sebelum keluar app.
- **TTS native — `window.BottoNativeTTS`** — plugin custom
  (`plugins/cordova-plugin-bottonativetts`) menyuntikkan objek ini ke SEMUA
  halaman yang dimuat WebView (bukan cuma `index.html`), lewat
  `addJavascriptInterface` Android native -- jadi tetap aktif walau halaman
  server yang dimuat tidak meng-include `cordova.js` sama sekali.
  ```js
  window.BottoNativeTTS.speak("halo, ini suara native Android");
  window.BottoNativeTTS.stop();
  ```
  Ini persis pola yang sudah dipakai di frontend project-project chat AI kamu
  (deteksi `'BottoNativeTTS' in window`), jadi tinggal plug-and-play.
- **File downloader** — plugin `cordova-plugin-webview-downloader`:
  - Link/download URL biasa (server mengirim `Content-Disposition: attachment`,
    atau file media) otomatis ditangkap `DownloadListener` dan diserahkan ke
    **Android DownloadManager** (notifikasi progres bawaan Android, file
    masuk ke folder **Downloads** publik).
  - Untuk file yang dibuat di sisi client (Blob/canvas, tidak lewat URL),
    halaman web bisa panggil langsung:
    ```js
    window.AndroidDownloader.saveBase64(base64String, "nama-file.pdf", "application/pdf");
    ```
    (support Android 10+ lewat MediaStore, dan versi lama lewat penulisan file
    langsung ke folder Downloads publik.)

## Struktur project

```
config.xml                    -- konfigurasi Cordova (id, nama, icon, permission)
package.json
www/index.html                -- layar awal (config screen)
www/css/style.css
www/js/index.js               -- simpan alamat server, tombol Hubungkan/Reset
www/img/logo.png               -- logo Dragons
res/icon/android/*.png        -- icon app semua densitas + adaptive icon
plugins/cordova-plugin-bottonativetts/      -- plugin TTS native
plugins/cordova-plugin-webview-downloader/  -- plugin downloader
.github/workflows/android-build.yml         -- build APK otomatis di GitHub Actions
```

## Build APK

### Otomatis (GitHub Actions)

1. Push/upload folder ini sebagai repo GitHub baru.
2. Buka tab **Actions** → workflow **"Build Android APK (Cordova)"** akan
   otomatis jalan tiap push ke `main`/`master`, atau jalankan manual lewat
   tombol **Run workflow**.
3. Setelah selesai, unduh APK dari bagian **Artifacts** run tersebut
   (nama artifact: `dragons-webview-debug-apk`).

APK yang dihasilkan adalah **debug build** (ditandatangani dengan debug
keystore bawaan) — cukup untuk instal & pakai sendiri lewat "Install dari
sumber tidak dikenal". Untuk rilis ke Play Store, perlu keystore rilis sendiri
dan langkah signing tambahan (belum disertakan di workflow ini).

### Manual (lokal, kalau punya Android SDK + Node)

```bash
npm install -g cordova
cordova platform add android
cordova build android
# APK ada di platforms/android/app/build/outputs/apk/debug/app-debug.apk
```

## Catatan

- `AndroidInsecureFileModeEnabled` & `usesCleartextTraffic="true"` diaktifkan
  supaya bisa konek ke server `http://` polos di LAN (bukan HTTPS) — wajar
  untuk skenario ini, tapi jangan dipakai untuk browsing ke internet umum.
- Package ID: `com.dragons.webview` — ganti di `config.xml` (atribut `id`)
  kalau mau publish sebagai app terpisah.
