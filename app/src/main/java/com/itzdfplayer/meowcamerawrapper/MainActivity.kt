package com.itzdfplayer.meowcamerawrapper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedDispatcher
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.itzdfplayer.meowcamerawrapper.databinding.ActivityMainBinding
import android.util.Base64
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class MainActivity : Activity() {
    private val userAgent =
        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.5615.135 Mobile Safari/537.36"
    private val siteUrl = "https://meow.camera/viewer/"
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var swipeLayout: SwipeRefreshLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webView = binding.webView
        swipeLayout = binding.swipeRefreshLayout

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("#000000")

        webView.settings.userAgentString = userAgent
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebViewInterface(this, webView), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(
                    """
                    var button = document.getElementById('player-action-snap');
                    var processing = false;
                    button.addEventListener('click', function() {
                        if (processing) {
                            return;
                        }
                        processing = true;
                        URL.createObjectURL = (function(_super) {
                            return function(blob) {
                                var reader = new FileReader();
                                reader.readAsDataURL(blob);
                                reader.onloadend = function() {
                                    var base64data = reader.result;
                                    Android.onDataUrlReady(base64data);
                                    processing = false;
                                }
                                return _super.call(this, blob);
                            }
                        })(URL.createObjectURL);
                    });
                """, null)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("hellopetlinkshare://")) {
                    try {
                        view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            applicationContext,
                            "Hello Street Cat is not installed!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return true
                }
                return false
            }
        }

        swipeLayout.setOnRefreshListener {
            webView.reload()
        }
        webView.loadUrl(siteUrl)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        if (webView.canGoBack() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            webView.goBack()
        else
            super.onBackPressed()
    }

    private class WebViewInterface(private val context: Context, private val webView: WebView) {

        @JavascriptInterface
        fun onDataUrlReady(dataUrl: String) {

            // Get the MIME type and base64 encoded data from the data URL
            val dataSplit = dataUrl.split(",")
            val mimeSplit = dataSplit[0].split(";")
            val mimeType = mimeSplit[0].split(":")[1]
            val base64Data = dataSplit[1]

            // Convert the base64 encoded data to binary data
            val data = Base64.decode(base64Data, Base64.DEFAULT)

            // Create a unique file name
            val fileName = "Screenshot-${Calendar.getInstance().timeInMillis}.${MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)}"

            // Get the external downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // Create a file in the downloads directory
            val file = File(downloadsDir, fileName)

            // Write the data to the file
            try {
                val fos = FileOutputStream(file)
                fos.write(data)
                fos.close()
                // Notify the user that the file has been downloaded
                Toast.makeText(context, "File saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Handle the exception
                e.printStackTrace()
            }
            webView.post { webView.reload() }
        }
    }
}
