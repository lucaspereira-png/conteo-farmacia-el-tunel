package com.eltunel.conteorapido;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 7301;
    private static final int SAVE_FILE_REQUEST = 7302;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private byte[] pendingDownloadBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        setContentView(webView);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDatabaseEnabled(true);

        webView.addJavascriptInterface(new DownloadBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !url.startsWith("https://appassets.androidplatform.net/assets/");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Intercepta descargas creadas con Blob (CSV/Excel) y las envía al selector nativo de archivos.
                String script = "(function(){if(window.__nativeDownloadHookInstalled)return;window.__nativeDownloadHookInstalled=true;" +
                        "document.addEventListener('click',function(ev){var a=ev.target&&ev.target.closest?ev.target.closest('a[download]'):null;" +
                        "if(!a||!a.href||a.href.indexOf('blob:')!==0)return;ev.preventDefault();ev.stopImmediatePropagation();" +
                        "var name=a.getAttribute('download')||'conteo_exportado.xlsx';var href=a.href;var originalRevoke=URL.revokeObjectURL;URL.revokeObjectURL=function(u){if(u===href){URL.revokeObjectURL=originalRevoke;setTimeout(function(){originalRevoke.call(URL,u);},60000);}else{originalRevoke.call(URL,u);}};" +
                        "fetch(href).then(function(r){return r.blob()}).then(function(b){return b.arrayBuffer().then(function(buf){" +
                        "var bytes=new Uint8Array(buf),bin='';for(var i=0;i<bytes.length;i+=0x8000){bin+=String.fromCharCode.apply(null,bytes.subarray(i,i+0x8000));}" +
                        "AndroidBridge.saveBase64File(name,btoa(bin),b.type||'application/octet-stream');});})" +
                        ".catch(function(){alert('No se pudo preparar el archivo para guardar. Intentá exportar nuevamente.');});},true);})();";
                view.evaluateJavascript(script, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "No se encontró una aplicación para seleccionar archivos.", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private class DownloadBridge {
        @JavascriptInterface
        public void saveBase64File(String fileName, String base64, String mimeType) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                runOnUiThread(() -> {
                    pendingDownloadBytes = bytes;
                    String safeName = (fileName == null || fileName.trim().isEmpty()) ? "conteo_exportado" : fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
                    String type = (mimeType == null || mimeType.trim().isEmpty()) ? "application/octet-stream" : mimeType;
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(type);
                    intent.putExtra(Intent.EXTRA_TITLE, safeName);
                    try {
                        startActivityForResult(intent, SAVE_FILE_REQUEST);
                    } catch (ActivityNotFoundException e) {
                        pendingDownloadBytes = null;
                        Toast.makeText(MainActivity.this, "No se encontró una aplicación para guardar archivos.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IllegalArgumentException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudo preparar el archivo exportado.", Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            if (webView != null) webView.requestFocus();
            return;
        }
        if (requestCode == SAVE_FILE_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingDownloadBytes != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out == null) throw new java.io.IOException("No se pudo abrir el destino");
                    out.write(pendingDownloadBytes);
                    out.flush();
                    Toast.makeText(this, "Archivo guardado correctamente.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "No se pudo guardar el archivo. Intentá nuevamente.", Toast.LENGTH_LONG).show();
                }
            }
            pendingDownloadBytes = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.removeJavascriptInterface("AndroidBridge");
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}
