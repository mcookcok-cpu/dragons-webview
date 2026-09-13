package com.dragons.webview.plugin;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import org.apache.cordova.CordovaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Downloader file untuk WebView, dua jalur:
 *
 *  1) DownloadListener bawaan WebView -- otomatis menangkap navigasi/klik
 *     yang di-server-side ditandai sebagai file download (header
 *     Content-Disposition: attachment, atau link langsung ke file media),
 *     lalu diserahkan ke Android DownloadManager (muncul notifikasi
 *     progres bawaan Android, tersimpan di folder Downloads publik).
 *
 *  2) window.AndroidDownloader.saveBase64(base64, filename, mimeType) --
 *     dipanggil manual dari JS halaman web untuk file yang dibuat di sisi
 *     client (mis. Blob/canvas) yang tidak lewat URL biasa sehingga tidak
 *     tertangkap DownloadListener.
 */
public class WebViewDownloaderPlugin extends CordovaPlugin {

    @Override
    protected void pluginInitialize() {
        View view = webView.getView();
        if (!(view instanceof WebView)) return;
        final WebView wv = (WebView) view;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wv.setDownloadListener(new DownloadListener() {
                    @Override
                    public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                                 String mimetype, long contentLength) {
                        startUrlDownload(url, userAgent, contentDisposition, mimetype);
                    }
                });
                wv.addJavascriptInterface(new BlobBridge(), "AndroidDownloader");
            }
        });
    }

    private void startUrlDownload(String url, String userAgent, String contentDisposition, String mimetype) {
        try {
            Context ctx = cordova.getActivity().getApplicationContext();
            String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("cookie", cookie);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            request.setTitle(filename);
            request.setDescription("Mengunduh file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                toast("Mengunduh: " + filename);
            }
        } catch (Exception e) {
            toast("Gagal mengunduh: " + e.getMessage());
        }
    }

    private void toast(final String msg) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cordova.getActivity().getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveBase64Internal(String base64Data, String filename, String mimeType) {
        try {
            // buang prefix data URL kalau ada, mis. "data:...;base64,"
            String cleaned = base64Data;
            int comma = base64Data.indexOf(",");
            if (base64Data.startsWith("data:") && comma != -1) {
                cleaned = base64Data.substring(comma + 1);
            }
            byte[] data = Base64.decode(cleaned, Base64.DEFAULT);
            Context ctx = cordova.getActivity().getApplicationContext();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE,
                        mimeType == null || mimeType.isEmpty() ? "application/octet-stream" : mimeType);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri itemUri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (itemUri == null) throw new Exception("Tidak bisa membuat entri MediaStore.");
                OutputStream out = ctx.getContentResolver().openOutputStream(itemUri);
                if (out == null) throw new Exception("Tidak bisa membuka output stream.");
                out.write(data);
                out.close();
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            }
            toast("Tersimpan di Downloads: " + filename);
        } catch (Exception e) {
            toast("Gagal menyimpan file: " + e.getMessage());
        }
    }

    private class BlobBridge {
        @JavascriptInterface
        public void saveBase64(final String base64Data, final String filename, final String mimeType) {
            if (base64Data == null || filename == null) return;
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    saveBase64Internal(base64Data, filename, mimeType);
                }
            });
        }
    }
}
