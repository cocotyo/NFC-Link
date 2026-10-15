package com.nfclink.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.nfc.*;
import android.nfc.tech.*;
import android.os.Build;
import android.os.Bundle;
import android.webkit.*;
import org.json.JSONObject;

public class MainActivity extends Activity {
    // GANTI dengan alamat halaman profil yang sudah online (HTTPS). Isi tag NFC akan membuka alamat ini.
    static final String BASE_URL = "https://alternative-advances-protecting-continuous.trycloudflare.com";

    WebView web;
    NfcAdapter nfc;
    volatile String pendingUrl = null;
    ValueCallback<Uri[]> filePathCb;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        nfc = NfcAdapter.getDefaultAdapter(this);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("file:")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) {}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePathCb != null) filePathCb.onReceiveValue(null);
                filePathCb = cb;
                try { startActivityForResult(p.createIntent(), 1); } catch (Exception e) { filePathCb = null; return false; }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int req, int res, Intent d) {
        if (req == 1 && filePathCb != null) {
            filePathCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, d));
            filePathCb = null;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (nfc != null && nfc.isEnabled()) {
            int fl = Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), fl);
            nfc.enableForegroundDispatch(this, pi, null, null);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (nfc != null) try { nfc.disableForegroundDispatch(this); } catch (Exception e) {}
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        final Tag tag = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final String url = pendingUrl;
        if (tag == null || url == null) return;
        new Thread(() -> {
            String err = null;
            try {
                NdefMessage m = new NdefMessage(NdefRecord.createUri(url));
                Ndef nd = Ndef.get(tag);
                if (nd != null) {
                    nd.connect();
                    if (!nd.isWritable()) throw new Exception("Tag terkunci (read-only)");
                    if (nd.getMaxSize() < m.toByteArray().length) throw new Exception("Memori tag terlalu kecil, pakai NTAG215/216");
                    nd.writeNdefMessage(m);
                    nd.close();
                } else {
                    NdefFormatable f = NdefFormatable.get(tag);
                    if (f == null) throw new Exception("Tag tidak mendukung NDEF");
                    f.connect(); f.format(m); f.close();
                }
            } catch (Exception e) { err = e.getMessage() == null ? e.toString() : e.getMessage(); }
            final boolean ok = err == null;
            final String msg = ok ? "Berhasil ditulis ✓ Sekarang tag bisa di-scan." : "Gagal menulis: " + err;
            if (ok) pendingUrl = null;
            runOnUiThread(() -> web.evaluateJavascript("window.onNfcResult&&window.onNfcResult(" + ok + "," + JSONObject.quote(msg) + ")", null));
        }).start();
    }

    class Bridge {
        @JavascriptInterface public boolean hasNfc() { return nfc != null; }
        @JavascriptInterface public boolean nfcEnabled() { return nfc != null && nfc.isEnabled(); }
        @JavascriptInterface public String baseUrl() { return BASE_URL; }
        @JavascriptInterface public void writeNfc(String url) {
            if (nfc == null || !nfc.isEnabled()) {
                runOnUiThread(() -> { startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                    web.evaluateJavascript("window.onNfcResult&&window.onNfcResult(false,'Nyalakan NFC di pengaturan, lalu coba lagi.')", null); });
                return;
            }
            pendingUrl = url;
            runOnUiThread(() -> onResume());
        }
        @JavascriptInterface public void copy(String t) {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("nfc", t));
        }
    }
}
