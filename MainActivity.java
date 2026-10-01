package com.hassan.prostudy;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

// Firebase & Google Imports
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends ComponentActivity {
    private static final String ALLOWED_ORIGIN = "https://hassan4082827.github.io/ProStudy/";
    private static final String ALLOWED_STORAGE_RAW = "https://raw.githubusercontent.com/hassan4082827/ProStudyStorage/";
    private static final String TAG = "ProStudy";
    public WebView webView;
    private SQLiteDatabase db;
    private FirebaseAnalytics firebaseAnalytics;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private GoogleSignInClient googleSignInClient;
    public volatile String currentToken = "pending";

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> handleGoogleSignInResult(result.getData()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Initialize Firebase & App Check
        FirebaseApp.initializeApp(this);
        initializeFirebase();
        
        setupGoogleSignIn();
        initializeDatabase();

        webView = new WebView(this);
        configureWebView();
        setupNetworkListener();
        handlePermissionsAndBack();

        setContentView(webView);

        if (isNetworkAvailable()) {
            showToast("🚀 Starting Fresh Session");
        } else {
            showToast("⚠️ No Internet! Using Offline Cache");
        }

        webView.loadUrl(ALLOWED_ORIGIN);
    }

    private void initializeFirebase() {
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FirebaseMessaging.getInstance().subscribeToTopic("all_users");
        
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                currentToken = task.getResult();
                showToast("✅ Connected To ProStudy Server");
                Bundle b = new Bundle();
                b.putString("status", "server_connected");
                firebaseAnalytics.logEvent("connection_success", b);
            }
        });
    }

    private void setupGoogleSignIn() {
        String webClientId = "954776695822-o0na9r7vosemb7h58ofkmlftabiu2p9q.apps.googleusercontent.com";
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void initializeDatabase() {
        db = openOrCreateDatabase("ProStudyDB", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS quizzes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "data TEXT," +
                "time DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        s.setOffscreenPreRaster(false);
        s.setLoadsImagesAutomatically(true);

        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        
        webView.setOnLongClickListener(v -> true);

        // Secure Native Download Handler for homework assets via GitHub Raw
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (!url.startsWith(ALLOWED_STORAGE_RAW)) {
                showToast("⛔ Blocked: Untrusted download origin");
                return;
            }

            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            String lower = fileName.toLowerCase();

            if (lower.endsWith(".apk") || lower.endsWith(".dex") || lower.endsWith(".sh") || lower.endsWith(".bat")) {
                showToast("⛔ Executable files not permitted via study storage");
                return;
            }

            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("ProStudy Study Material");
                request.setTitle(fileName);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                File proStudyDirectory = new File("/sdcard/ProStudyData");
                if (!proStudyDirectory.exists() && !proStudyDirectory.mkdirs()) {
                    showToast("❌ Could not create ProStudyData folder");
                    return;
                }
                request.setDestinationUri(Uri.fromFile(new File(proStudyDirectory, fileName)));

                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    showToast("📥 Downloading to /sdcard/ProStudyData/...");
                }
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                showToast("❌ Download error: " + e.getMessage());
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Whitelist check: Allow ProStudy origin AND about:blank
                if (url.startsWith(ALLOWED_ORIGIN) || url.equals("about:blank")) {
                    return false;
                }

                // Allow GitHub Raw storage requests to pass to DownloadListener
                if (url.startsWith(ALLOWED_STORAGE_RAW)) {
                    return false;
                }
                
                // Open external links in a real browser
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
        webView.addJavascriptInterface(new ProStudyInterface(), "ProStudyApp");
    }

    private void setupNetworkListener() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    webView.reload();
                    showToast("✅ Reconnected!");
                });
            }
            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> showToast("⚠️ No Internet!"));
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private void handlePermissionsAndBack() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                if (webView.canGoBack()) webView.goBack();
                else finish();
            });
        }

        // Main Web Workspace Master Storage Runtime Verification Guard Trigger
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 102);
            }
        }
    }


    private boolean isNetworkAvailable() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    private void handleGoogleSignInResult(Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(Exception.class);
            if (account == null) {
                showToast("❌ Google Sign-In Failed");
                return;
            }
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null && currentUser.isAnonymous()) {
                currentUser.linkWithCredential(credential)
                        .addOnSuccessListener(authResult -> {
                            showToast("🚀 Account Upgraded!");
                            syncUserToFirebase(account);
                        })
                        .addOnFailureListener(e -> {
                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                    .addOnSuccessListener(authResult -> syncUserToFirebase(account));
                        });
            } else {
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener(authResult -> syncUserToFirebase(account))
                        .addOnFailureListener(e -> showToast("❌ Login Error: " + e.getMessage()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Sign-In Error", e);
            showToast("❌ Error: " + e.getMessage());
        }
    }

    private void syncUserToFirebase(GoogleSignInAccount account) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        userRef.child("GoogleLinked").setValue(true);
        userRef.child("FirebaseToken").setValue(currentToken);
        userRef.child("lastLogin").setValue(System.currentTimeMillis());

        userRef.child("Username").get().addOnCompleteListener(task -> {
            runOnUiThread(() -> {
                if (task.isSuccessful() && task.getResult().exists() && task.getResult().getValue() != null) {
                    String username = task.getResult().getValue().toString();
                    webView.evaluateJavascript(String.format("recoverAccount(%s);", JSONObject.quote(username)), null);
                    showToast("✅ Welcome Back, " + username);
                } else {
                    webView.evaluateJavascript("recoverAccount(null);", null);
                    showToast("🆕 Choose Your Username");
                }
            });
        });
    }

    public class ProStudyInterface {
        private boolean isValidOrigin() {
            String currentUrl = webView.getUrl();
            return currentUrl != null && currentUrl.startsWith(ALLOWED_ORIGIN);
        }

        @JavascriptInterface public String getVersionCode() { return "310"; }
        @JavascriptInterface public String getVersionName() { return "3.1.0"; }

        @JavascriptInterface
        public String getStorageInfo() {
            if (!isValidOrigin()) return "{}";

            try {
                StatFs storage = new StatFs(Environment.getDataDirectory().getPath());
                long totalBytes = storage.getTotalBytes();
                long freeBytes = storage.getAvailableBytes();
                File downloadsDirectory = new File("/sdcard/ProStudyData");
                long downloadsBytes = directorySize(downloadsDirectory);
                long quizzesBytes = directorySize(getDatabasePath("ProStudyDB").getParentFile());
                long cacheBytes = directorySize(getCacheDir());
                long appFilesBytes = directorySize(getFilesDir());
                long appBytes = downloadsBytes + quizzesBytes + cacheBytes + appFilesBytes;
                int downloadCount = fileCount(downloadsDirectory);
                int quizCount = 0;

                try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM quizzes", null)) {
                    if (cursor.moveToFirst()) quizCount = cursor.getInt(0);
                }

                JSONObject info = new JSONObject();
                info.put("totalBytes", totalBytes);
                info.put("freeBytes", freeBytes);
                info.put("appBytes", appBytes);
                info.put("downloadsBytes", downloadsBytes);
                info.put("downloadCount", downloadCount);
                info.put("quizzesBytes", quizzesBytes);
                info.put("quizCount", quizCount);
                info.put("cacheBytes", cacheBytes);
                return info.toString();
            } catch (Exception e) {
                Log.e(TAG, "Storage info error", e);
                return "{}";
            }
        }

        private long directorySize(File directory) {
            if (directory == null || !directory.exists()) return 0;
            if (directory.isFile()) return directory.length();

            long size = 0;
            File[] children = directory.listFiles();
            if (children == null) return 0;

            for (File child : children) {
                size += directorySize(child);
            }
            return size;
        }

        private int fileCount(File directory) {
            if (directory == null || !directory.exists()) return 0;
            if (directory.isFile()) return 1;

            int count = 0;
            File[] children = directory.listFiles();
            if (children == null) return 0;

            for (File child : children) {
                count += fileCount(child);
            }
            return count;
        }

        @JavascriptInterface
        public void startGoogleSignIn() {
            if (!isValidOrigin()) return;
            runOnUiThread(() -> {
                showToast("🔄 Connecting to Google...");
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
            });
        }

        @JavascriptInterface
        public void checkUserRecovery() {
            if (!isValidOrigin()) return;
            webView.postDelayed(() -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
                    userRef.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            Object nameObj = task.getResult().child("Username").getValue();
                            if (nameObj != null) {
                                String name = nameObj.toString();
                                runOnUiThread(() -> webView.evaluateJavascript(String.format("recoverAccount(%s);", JSONObject.quote(name)), null));
                                userRef.child("FirebaseToken").setValue(currentToken);
                            } else {
                                runOnUiThread(() -> webView.evaluateJavascript("recoverAccount(null);", null));
                            }
                        } else {
                            runOnUiThread(() -> webView.evaluateJavascript("recoverAccount(null);", null));
                        }
                    });
                } else {
                    runOnUiThread(() -> webView.evaluateJavascript("recoverAccount(null);", null));
                }
            }, 300);
        }

        @JavascriptInterface
        public void saveUserIdentity(String n) {
            if (!isValidOrigin() || n == null || n.trim().isEmpty()) return;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                FirebaseAuth.getInstance().signInAnonymously()
                        .addOnSuccessListener(authResult -> syncToRTDB(n))
                        .addOnFailureListener(e -> showToast("❌ Auth Error"));
            } else {
                syncToRTDB(n);
            }
        }

        private void syncToRTDB(String n) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.child("Username").setValue(n);
            userRef.child("FirebaseToken").setValue(currentToken);
            userRef.child("lastLogin").setValue(System.currentTimeMillis());
            showToast("🚀 Session Secured, " + n);
        }

        @JavascriptInterface
        public void saveFullQuiz(String j) {
            if (!isValidOrigin() || j == null || j.isEmpty()) return;
            ContentValues cv = new ContentValues();
            cv.put("data", j);
            db.insert("quizzes", null, cv);
            showToast("💾 Quiz Saved Locally");
            runOnUiThread(() -> webView.evaluateJavascript("loadHistory();", null));
        }

        @JavascriptInterface
        public String loadAllQuizzes() {
            if (!isValidOrigin()) return "[]";
            JSONArray array = new JSONArray();
            try (Cursor c = db.rawQuery("SELECT id,data FROM quizzes ORDER BY id DESC", null)) {
                while (c.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", c.getInt(0));
                    obj.put("data", c.getString(1));
                    array.put(obj);
                }
            } catch (Exception e) { Log.e(TAG, "DB Error", e); }
            return array.toString();
        }

        @JavascriptInterface
        public void downloadAndInstallUpdate(String downloadUrl) {
            new Thread(() -> {
                try {
                    URL url = new URL(downloadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    int fileLength = conn.getContentLength();
                    File cacheDir = new File(getCacheDir(), "updates");
                    if (!cacheDir.exists()) cacheDir.mkdirs();

                    File outputFile = new File(cacheDir, "ProStudy-update.apk");
                    if (outputFile.exists()) outputFile.delete();

                    InputStream input = conn.getInputStream();
                    OutputStream output = new FileOutputStream(outputFile);

                    byte[] data = new byte[4096];
                    long total = 0;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        if (fileLength > 0) {
                            final int progress = (int) (total * 100 / fileLength);
                            runOnUiThread(() -> {
                                webView.evaluateJavascript("if(window.onNativeDownloadProgress) onNativeDownloadProgress(" + progress + ");", null);
                            });
                        }
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();

                    runOnUiThread(() -> launchApkInstaller(outputFile));

                } catch (Exception e) {
                    Log.e("ProStudyUpdate", "Update error", e);
                    showToast("❌ Download Failed: " + e.getMessage());
                }
            }).start();
        }
    }

    private void launchApkInstaller(File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                showToast("⚠️ Please enable 'Install Unknown Apps' for ProStudy");
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }
        }

        Uri apkUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                apkFile
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT < 33 && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) connectivityManager.unregisterNetworkCallback(networkCallback);
        if (db != null && db.isOpen()) db.close();
    }
}

