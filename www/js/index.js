(function () {
  "use strict";

  var STORAGE_KEY = "dragons_server_url";

  var input = document.getElementById("serverUrl");
  var connectBtn = document.getElementById("connectBtn");
  var resetBtn = document.getElementById("resetBtn");
  var errorMsg = document.getElementById("errorMsg");

  function showError(msg) {
    errorMsg.textContent = msg;
    errorMsg.hidden = false;
  }

  function hideError() {
    errorMsg.hidden = true;
  }

  function normalizeUrl(raw) {
    var v = (raw || "").trim();
    if (!v) return null;
    if (!/^https?:\/\//i.test(v)) {
      v = "http://" + v;
    }
    try {
      // validasi dasar
      new URL(v);
      return v;
    } catch (e) {
      return null;
    }
  }

  function loadSavedUrl() {
    try {
      var saved = window.localStorage.getItem(STORAGE_KEY);
      if (saved) input.value = saved;
    } catch (e) {
      /* localStorage tidak tersedia, abaikan */
    }
  }

  function saveUrl(url) {
    try {
      window.localStorage.setItem(STORAGE_KEY, url);
    } catch (e) {
      /* abaikan */
    }
  }

  function connect() {
    hideError();
    var url = normalizeUrl(input.value);
    if (!url) {
      showError("Alamat server tidak valid. Contoh: http://192.168.1.100:2020");
      return;
    }
    saveUrl(url);
    // Navigasi penuh (bukan iframe) supaya halaman tujuan berjalan sebagai
    // top-level document -- ini WAJIB supaya window.BottoNativeTTS dan
    // window.AndroidDownloader (native interface) tetap terlihat oleh
    // JS di halaman tujuan.
    window.location.href = url;
  }

  function resetField() {
    input.value = "";
    hideError();
    try {
      window.localStorage.removeItem(STORAGE_KEY);
    } catch (e) {
      /* abaikan */
    }
    input.focus();
  }

  connectBtn.addEventListener("click", connect);
  resetBtn.addEventListener("click", resetField);
  input.addEventListener("keydown", function (ev) {
    if (ev.key === "Enter") connect();
  });

  document.addEventListener(
    "deviceready",
    function () {
      loadSavedUrl();

      // Tombol back hardware Android: kalau lagi di halaman config ini,
      // biarkan perilaku default (keluar app). Cordova sudah menutup app
      // secara default saat backbutton ditekan di halaman pertama history,
      // jadi tidak perlu override apa pun di sini. Kalau user sedang di
      // halaman remote (server), backbutton bawaan WebView akan otomatis
      // mundur ke halaman ini dulu sebelum keluar app.
    },
    false
  );

  // fallback kalau dijalankan di browser biasa tanpa cordova.js
  if (!window.cordova) {
    document.addEventListener("DOMContentLoaded", loadSavedUrl);
  }
})();
