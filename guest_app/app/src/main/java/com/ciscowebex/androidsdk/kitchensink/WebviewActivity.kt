package com.ciscowebex.androidsdk.kitchensink

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ciscowebex.androidsdk.kitchensink.auth.OAuthWebLoginActivity
import com.ciscowebex.androidsdk.kitchensink.calling.CallActivity
import com.ciscowebex.androidsdk.kitchensink.calling.CallControlsFragment
import com.ciscowebex.androidsdk.kitchensink.messaging.spaces.ReplyMessageModel
import com.ciscowebex.androidsdk.kitchensink.utils.Constants
import kotlinx.android.synthetic.main.activity_webview.*


class WebviewActivity : AppCompatActivity() {

    companion object {

        fun getIntent(
            context: Context,
            accessToken: String,
            email:String
        ): Intent {
            val intent = Intent(context, WebviewActivity::class.java)
            intent.putExtra(Constants.Intent.ACCESS_TOKEN, accessToken)
            intent.putExtra(Constants.Intent.EMAIL_ID, email)
            return intent
        }

    }

    lateinit var mEditText : EditText
    lateinit var mButtonSend : Button
    private lateinit var accessToken: String
    private lateinit var email: String
    private val mFilePath = "file:///android_asset/sampleweb.html"
   var  filePathCallback: ValueCallback<Array<Uri>>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        accessToken = intent.getStringExtra(Constants.Intent.ACCESS_TOKEN) ?: ""
        email = intent.getStringExtra(Constants.Intent.EMAIL_ID) ?: ""

        webViewSample.settings.javaScriptEnabled=true

        webViewSample.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }
        }
        webViewSample.loadUrl("https://fuchsia-jewel-giraffe.glitch.me/widgetCustomer.html")

//        webViewSample.settings.javaScriptEnabled = true
//        exitButton.rotation=180.0F
//        webViewSample.loadUrl("https://fuchsia-jewel-giraffe.glitch.me/widget.html/")
//        // webViewSample.addJavascriptInterface(JSBridge(this,editInput),"JSBridge")
//        //webViewSample.loadUrl(mFilePath)
//        WebView.setWebContentsDebuggingEnabled(true);
//        Log.e("accessToken", accessToken)
//        webViewSample.setWebViewClient(object : WebViewClient() {
//
//            override fun onPageFinished(view: WebView, url: String) {
//
//                //Here you want to use .loadUrl again
//                //on the webview object and pass in
//                //"javascript:<your javaScript function"
//
////                webViewSample.loadUrl("javascript: " +"updateFromNative(\"" + accessToken + "\",\""+email+"\")") //if passing in an object. Mapping may need to take place
//               webViewSample.loadUrl("https://fuchsia-jewel-giraffe.glitch.me/widget.html/")
//            }
//        })
//
        webViewSample.setWebChromeClient(object: WebChromeClient(){
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                startActivityForResult(fileChooserParams?.createIntent()!!, 100)
                this@WebviewActivity.filePathCallback = filePathCallback
                return true
            }

        })
        webViewSample.addJavascriptInterface( JavaScriptInterface(applicationContext),"Android")
        webViewSample.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if(url.startsWith("blob")){
               webViewSample.evaluateJavascript(JavaScriptInterface.getBase64StringFromBlobUrl(url,mimetype),null)
                return@setDownloadListener
            }
          val downloadRequest =  DownloadManager.Request(Uri.parse(url))
            downloadRequest.addRequestHeader("cookie",CookieManager.getInstance().getCookie(url))
            downloadRequest.addRequestHeader("User-Agent",userAgent)
            downloadRequest.setTitle(URLUtil.guessFileName(url,contentDisposition,mimetype))
            downloadRequest.setDescription("The File is Downloading..")
            downloadRequest.allowScanningByMediaScanner()
            downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,contentDisposition,mimetype))
         val dm : DownloadManager =  getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(downloadRequest)
            Toast.makeText(applicationContext,"Downloading has been started",Toast.LENGTH_LONG).show()
        }
        exitButton.setOnClickListener{
            startActivity(Intent(this@WebviewActivity, CallControlsFragment::class.java))
        }



//        mButtonSend = findViewById(R.id.sendData)
//        mButtonSend.setOnClickListener {
//            sendDataToWebView()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         if(requestCode==100) {
             filePathCallback?.onReceiveValue(
                 WebChromeClient.FileChooserParams.parseResult(
                     resultCode,
                     data
                 )
             )
             filePathCallback = null
         }
        super.onActivityResult(requestCode, resultCode, data)
    }

//    class JSBridge(val context: Context, val editTextInput: EditText){
//        @JavascriptInterface
//        fun showMessageInNative(message:String){
//            Toast.makeText(context,message, Toast.LENGTH_LONG).show()
//            editTextInput.setText(message)
//        }
//    }

}