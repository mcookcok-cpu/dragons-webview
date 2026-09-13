package com.dragons.webview.plugin;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.apache.cordova.CordovaPlugin;

import android.os.Bundle;
import java.util.Locale;

/**
 * Menyuntikkan window.BottoNativeTTS = { speak(text), stop() } ke WebView
 * lewat addJavascriptInterface -- ini bekerja untuk SEMUA halaman yang
 * dimuat WebView (file:// maupun http:// hasil navigasi ke server lokal),
 * bukan cuma halaman yang meng-include cordova.js. Cocok dipakai
 * antarmuka chat AI yang mengecek `'BottoNativeTTS' in window`.
 */
public class BottoNativeTtsPlugin extends CordovaPlugin {

    private static final String JS_INTERFACE_NAME = "BottoNativeTTS";
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;

    @Override
    protected void pluginInitialize() {
        final android.content.Context ctx = cordova.getActivity().getApplicationContext();

        tts = new TextToSpeech(ctx, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(new Locale("id", "ID"));
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.getDefault());
                    }
                    ttsReady = true;
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { }
            @Override public void onDone(String utteranceId) { }
            @Override public void onError(String utteranceId) { }
        });

        View view = webView.getView();
        if (view instanceof WebView) {
            final WebView wv = (WebView) view;
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    wv.addJavascriptInterface(new JsBridge(), JS_INTERFACE_NAME);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /**
     * Objek yang diekspos ke JS sebagai window.BottoNativeTTS.
     * Method di sini dipanggil di thread WebView/JS milik Android, jadi
     * pekerjaan TTS-nya sendiri dilempar ke UI thread lewat runOnUiThread.
     */
    private class JsBridge {
        @JavascriptInterface
        public void speak(final String text) {
            if (text == null || text.trim().isEmpty()) return;
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tts == null) return;
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), "botto_utt");
                }
            });
        }

        @JavascriptInterface
        public void stop() {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tts != null) tts.stop();
                }
            });
        }

        @JavascriptInterface
        public boolean isReady() {
            return ttsReady;
        }
    }
}
