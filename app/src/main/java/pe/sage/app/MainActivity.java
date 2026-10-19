package pe.sage.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String SAGE_URL = "https://jayrodaniel20.github.io/registro-ocurrencias/";
    private static final int FILE_CHOOSER = 1001;
    private static final int AUDIO_PERMISSION = 1002;

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " SAGE-Android-Fallback/1.0");

        webView.addJavascriptInterface(new SpeechBridge(), "SAGEAndroidSpeech");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                injectSpeechPolyfill();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    if (request.getOrigin() != null &&
                        request.getOrigin().toString().startsWith("https://jayrodaniel20.github.io")) {
                        request.grant(request.getResources());
                    } else {
                        request.deny();
                    }
                });
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(SAGE_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
        }
    }

    private void injectSpeechPolyfill() {
        String js =
            "(function(){"+
            "if(window.SpeechRecognition||window.webkitSpeechRecognition)return;"+
            "class NativeSpeechRecognition{"+
            "constructor(){this.lang='es-PE';this.continuous=false;this.interimResults=false;"+
            "this.onresult=null;this.onerror=null;this.onend=null;window.__sageNativeSpeech=this;}"+
            "start(){try{SAGEAndroidSpeech.start(this.lang||'es-PE')}catch(e){if(this.onerror)this.onerror({error:String(e)})}}"+
            "stop(){try{SAGEAndroidSpeech.stop()}catch(e){}}"+
            "__result(t){if(this.onresult){this.onresult({resultIndex:0,results:[[{transcript:t,confidence:1}]]});}if(this.onend)this.onend();}"+
            "__error(e){if(this.onerror)this.onerror({error:e});if(this.onend)this.onend();}"+
            "}"+
            "window.SpeechRecognition=NativeSpeechRecognition;window.webkitSpeechRecognition=NativeSpeechRecognition;"+
            "})();";
        webView.evaluateJavascript(js, null);
    }

    public class SpeechBridge {
        @JavascriptInterface
        public void start(String lang) {
            runOnUiThread(() -> startNativeSpeech(lang));
        }

        @JavascriptInterface
        public void stop() {
            runOnUiThread(() -> {
                if (speechRecognizer != null) speechRecognizer.stopListening();
            });
        }
    }

    private void startNativeSpeech(String lang) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            sendSpeechError("Permiso de micrófono no concedido");
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendSpeechError("Reconocimiento de voz no disponible");
            return;
        }

        if (speechRecognizer != null) speechRecognizer.destroy();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) { sendSpeechError("Error de reconocimiento: " + error); }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) sendSpeechResult(list.get(0));
                else sendSpeechError("No se reconoció texto");
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, (lang == null || lang.isEmpty()) ? "es-PE" : lang);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechRecognizer.startListening(intent);
    }

    private void sendSpeechResult(String text) {
        String safe = org.json.JSONObject.quote(text);
        webView.evaluateJavascript(
            "if(window.__sageNativeSpeech&&window.__sageNativeSpeech.__result){window.__sageNativeSpeech.__result("+safe+");}", null);
    }

    private void sendSpeechError(String error) {
        String safe = org.json.JSONObject.quote(error);
        webView.evaluateJavascript(
            "if(window.__sageNativeSpeech&&window.__sageNativeSpeech.__error){window.__sageNativeSpeech.__error("+safe+");}", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i=0;i<count;i++) results[i]=data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
