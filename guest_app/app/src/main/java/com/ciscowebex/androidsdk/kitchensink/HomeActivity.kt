package com.ciscowebex.androidsdk.kitchensink

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.ciscowebex.androidsdk.CompletionHandler
import com.ciscowebex.androidsdk.auth.OAuthWebViewAuthenticator
import com.ciscowebex.androidsdk.auth.TokenAuthenticator
import com.ciscowebex.androidsdk.auth.UCLoginServerConnectionStatus
import com.ciscowebex.androidsdk.kitchensink.auth.LoginActivity
import com.ciscowebex.androidsdk.kitchensink.calling.CallActivity
import com.ciscowebex.androidsdk.kitchensink.cucm.UCLoginActivity
import com.ciscowebex.androidsdk.kitchensink.databinding.ActivityHomeBinding
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import com.ciscowebex.androidsdk.kitchensink.extras.ExtrasActivity
import com.ciscowebex.androidsdk.kitchensink.firebase.FirebaseDBManager
import com.ciscowebex.androidsdk.kitchensink.messaging.MessagingActivity
import com.ciscowebex.androidsdk.kitchensink.messaging.spaces.detail.MessageDetailsDialogFragment
import com.ciscowebex.androidsdk.kitchensink.person.PersonDialogFragment
import com.ciscowebex.androidsdk.kitchensink.person.PersonViewModel
import com.ciscowebex.androidsdk.kitchensink.search.SearchActivity
import com.ciscowebex.androidsdk.kitchensink.setup.SetupActivity
import com.ciscowebex.androidsdk.kitchensink.utils.CallObjectStorage
import com.ciscowebex.androidsdk.kitchensink.utils.Constants
import com.ciscowebex.androidsdk.kitchensink.utils.FileUtils
import com.ciscowebex.androidsdk.kitchensink.utils.SharedPrefUtils
import com.ciscowebex.androidsdk.kitchensink.utils.SharedPrefUtils.clearLoginTypePref
import com.ciscowebex.androidsdk.kitchensink.utils.SharedPrefUtils.saveLoginTypePref
import com.ciscowebex.androidsdk.kitchensink.utils.extensions.hideKeyboard
import com.ciscowebex.androidsdk.kitchensink.webhooks.WebhooksActivity
import com.ciscowebex.androidsdk.message.LocalFile
import com.ciscowebex.androidsdk.phone.Phone
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import org.koin.android.ext.android.inject
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.regex.Matcher
import java.util.regex.Pattern


class HomeActivity : BaseActivity() {

    lateinit var binding: ActivityHomeBinding
    private val personViewModel : PersonViewModel by inject()
    var isAllFieldsChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tag = "HomeActivity"

        val authenticator = webexViewModel.webex.authenticator

        webexViewModel.setLogLevel(webexViewModel.logFilter)
        webexViewModel.enableConsoleLogger(webexViewModel.isConsoleLoggerEnabled)
        webexViewModel.setOnInitialSpacesSyncCompletedListener()

        if(SharedPrefUtils.isAppBackgroundRunningPreferred(this)) {
            KitchenSinkForegroundService.startForegroundService(this)
        }

        Log.d(tag, "Service URls METRICS: ${webexViewModel.getServiceUrl(Phone.ServiceUrlType.METRICS)}" +
                "\nCLIENT_LOGS: ${webexViewModel.getServiceUrl(Phone.ServiceUrlType.CLIENT_LOGS)}" +
                "\nKMS: ${webexViewModel.getServiceUrl(Phone.ServiceUrlType.KMS)}")

        authenticator?.let {
            when (it) {
                is OAuthWebViewAuthenticator -> {
                    saveLoginTypePref(this, LoginActivity.LoginType.OAuth)
                }
                is TokenAuthenticator -> {
                    saveLoginTypePref(this, LoginActivity.LoginType.AccessToken)
                    webexViewModel.setOnTokenExpiredListener()
                }
                else -> {
                    saveLoginTypePref(this, LoginActivity.LoginType.JWT)
                }
            }
        }

        webexViewModel.signOutListenerLiveData.observe(this@HomeActivity, Observer {
            it?.let {
                if (it) {
                    clearLoginTypePref(this)
                    (application as KitchenSinkApp).unloadKoinModules()
                    KitchenSinkForegroundService.stopForegroundService(this)
                    finish()
                }
//                else {
//                    binding.progressLayout.visibility = View.GONE
//                }
            }
        })


        webexViewModel.ucLiveData.observe(this@HomeActivity, Observer {
            if (it != null) {
                when (WebexRepository.UCCallEvent.valueOf(it.first.name)) {
                    WebexRepository.UCCallEvent.OnUCServerConnectionStateChanged -> {
                        updateUCData()
                    }
                    else -> {}
                }
            }
        })

        webexViewModel.incomingListenerLiveData.observe(this@HomeActivity, Observer {
            it?.let {
                Log.d(tag, "incomingListenerLiveData: ${it.getCallId()}")
                val callId = it.getCallId()
                if(callId != null){
                    if(CallObjectStorage.getCallObject(callId) != null){
                        if(!it.isWebexCallingOrWebexForBroadworks() && !it.isCUCMCall()) {
                            // For Webex Calling call is notified in FCM service with accept decline button even for foreground case
                            // So not notifying here in home screen
                            Handler(Looper.getMainLooper()).post {
                                startActivity(CallActivity.getIncomingIntent(this, it.getCallId()))
                            }
                        }
                    }
                }
            }
        })

        webexViewModel.initialSpacesSyncCompletedLiveData.observe(this@HomeActivity) {
            Log.d(tag, getString(R.string.initial_spaces_sync_completed))
            Snackbar.make(binding.root, getString(R.string.initial_spaces_sync_completed), Snackbar.LENGTH_LONG).show()
        }

        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)
                .also { binding = it }
                .apply {

//                    binding.version.text = "Version : "+BuildConfig.VERSION_NAME

//                    ivStartCall.setOnClickListener {
//                        startActivity(Intent(this@HomeActivity, SearchActivity::class.java))
//                    }
//                    ivRequestCallback.setOnClickListener{
//
//                    }
//
//                    ivMessaging.setOnClickListener {
//                        startActivity(Intent(this@HomeActivity, MessagingActivity::class.java))
//                    }
//
//                    ivUcLogin.setOnClickListener {
//                        startActivity(UCLoginActivity.getIntent(this@HomeActivity))
//                    }
//
                    ivAddFab.setOnClickListener {
                        showCreateCategoryDialog()
                    }

                    ivLogout.setOnClickListener {
//                        progressLayout.visibility = View.VISIBLE
                        webexViewModel.signOut()
                    }

                    ivGetMe.setOnClickListener {
                        PersonDialogFragment().show(supportFragmentManager, getString(R.string.person_detail))
                    }

//                    ivFeedback.setOnClickListener {
//                        val fileUri = webexViewModel.getlogFileUri(false)
//                        val recipient = "webex-mobile-sdk@cisco.com"
//                        val subject = resources.getString(R.string.feedbackLogsSubject)
//
//                        val emailIntent = Intent().apply {
//                            action = Intent.ACTION_SEND
//                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                            type = "text/plain"
////                            data = Uri.parse("mailto:")
//                            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
//                            putExtra(Intent.EXTRA_SUBJECT, subject)
//                            putExtra(Intent.EXTRA_STREAM, fileUri)
//                        }
//
//                        try {
//                            startActivity(Intent.createChooser(emailIntent, "Send mail..."))
//                        }
//                        catch (e: Exception) {
//                            Log.e(tag, "Send mail exception: $e")
//                        }
//                    }
//
//                    ivSetup.setOnClickListener {
//                        startActivity(Intent(this@HomeActivity, SetupActivity::class.java))
//                    }
//
//                    ivExtras.setOnClickListener {
//                        startActivity(Intent(this@HomeActivity, ExtrasActivity::class.java))
//                    }
                }

        //used some delay because sometimes it gives empty stuff in personDetails
        Handler().postDelayed(Runnable {
            personViewModel.getMe()
        }, 1000)
        observeData()
        showMessageIfCameFromNotification()
        webexViewModel.setSpaceObserver()
        webexViewModel.setMembershipObserver()
        webexViewModel.setMessageObserver()
        webexViewModel.setCalendarMeetingObserver()

        // UC Login
        webexViewModel.startUCServices()
        observeUCLoginData()
    }

    fun showCreateCategoryDialog() {
        var selection: String = "other"
        var spaceID: String? = null
        val context = this
        val messageTo: String? = null
        val builder = AlertDialog.Builder(context)
        // alert = builder.create()
        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.another_view1, null)

        val nameText = view.findViewById(R.id.textName) as TextInputEditText
        val buttonPopup = view.findViewById<ImageButton>(R.id.button_popup)

        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.show()
        val window: Window? = alertDialog.getWindow()
        window?.setGravity(Gravity.AXIS_Y_SHIFT)

        val info = FirebaseDBManager.dataMap.keys.map { it }
        // create an array adapter and pass the required parameter
        // in our case pass the context, drop down layout , and array.
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line, info
        )

        buttonPopup.setOnClickListener {
            FirebaseDBManager.writeData(nameText.toString(),"rkanthet@cisco.com")
            // Dismiss the popup window
                Log.e("selectionName", "in alpian main" )
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
            val view1 = layoutInflater.inflate(R.layout.request_accepted, null)
            builder.setView(view1)
            val alertDialog1 = builder.create()
            alertDialog1.show()
            val window1: Window? = alertDialog1.getWindow()
            window1?.setGravity(Gravity.AXIS_Y_SHIFT)
            val close:Button = view1.findViewById(R.id.button_close)
            close.setOnClickListener{
                alertDialog1.dismiss();
            }
        }


    }

    private fun CheckAllFields(
        nameText: TextInputEditText,
        selection: String,
        autoTextView: AutoCompleteTextView,
        textPhone: TextInputEditText,
        textEmail: TextInputEditText
    ): Boolean {
        if (nameText.getText().toString().length == 0) {
            nameText.setError("Name is required!")
            return false
        }
        if (selection == "other") {
            autoTextView.setError("Regarding what you are looking for is required!")
            return false
        }
        if (!PhoneNumberUtils.isGlobalPhoneNumber(
                textPhone.getText().toString()
            ) && textPhone.getText().toString().length < 10
        ) {
            textPhone.setError("Please enter a valid phone number!")
            return false
        }
        val pattern: Pattern
        val matcher: Matcher
        val EMAIL_PATTERN =
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
        pattern = Pattern.compile(EMAIL_PATTERN)
        matcher = pattern.matcher(textEmail.getText())
        Log.e("emailMatcher", matcher.matches().toString())
        if (!matcher.matches()) {
            textEmail.setError("Please enter a valid email!")
            return false
        }

        // after all validation return true.
        return true
    }

    private fun observeUCLoginData() {
        webexViewModel.ucLiveData.observe(this@HomeActivity, Observer {
            Log.d(tag, "uc login observer called : ${it.first.name}")
            if (it != null) {
                when (WebexRepository.UCCallEvent.valueOf(it.first.name)) {
                    WebexRepository.UCCallEvent.OnUCLoggedIn, WebexRepository.UCCallEvent.OnUCServerConnectionStateChanged -> {
                        updateUCData()
                    }
                    WebexRepository.UCCallEvent.ShowSSOLogin -> {
                        startActivity(UCLoginActivity.getIntent(this@HomeActivity,
                            UCLoginActivity.Companion.OnActivityStartAction.ShowSSOLogin.name,
                            it.second))
                    }

                    WebexRepository.UCCallEvent.ShowNonSSOLogin -> {
                        startActivity(UCLoginActivity.getIntent(this@HomeActivity, UCLoginActivity.Companion.OnActivityStartAction.ShowNonSSOLogin.name))
                    }
                    else -> {
                    }
                }
            }
        })
    }

    private fun showMessageIfCameFromNotification() {

        if("ACTION" == intent?.action){
            val messageId = intent?.getStringExtra(Constants.Bundle.MESSAGE_ID)
            MessageDetailsDialogFragment.newInstance(messageId.orEmpty()).show(supportFragmentManager, "MessageDetailsDialogFragment")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        val messageId = intent?.getStringExtra(Constants.Bundle.MESSAGE_ID)
        MessageDetailsDialogFragment.newInstance(messageId.orEmpty()).show(supportFragmentManager, "MessageDetailsDialogFragment")
        super.onNewIntent(intent)
    }

    private fun observeData() {
        personViewModel.person.observe(this, Observer { person ->
            person?.let {
                webexViewModel.getFCMToken(it)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        updateUCData()
        webexViewModel.setIncomingListener()
        addVirtualBackground()
        checkForInitialSpacesSync()
    }

    private fun checkForInitialSpacesSync() {
        if (!webexViewModel.isSpacesSyncCompleted()) {
            Snackbar.make(binding.root, getString(R.string.syncing_spaces), Snackbar.LENGTH_SHORT).show()
        }
    }


    private fun updateUCData() {
        Log.d(tag, "updateUCData isUCServerLoggedIn: ${webexViewModel.repository.isUCServerLoggedIn} ucServerConnectionStatus: ${webexViewModel.repository.ucServerConnectionStatus}")
        if (webexViewModel.isUCServerLoggedIn) {
//            binding.ucLoginStatusTextView.visibility = View.VISIBLE
            if(webexViewModel.getCallingType() == Phone.CallingType.WebexCalling)  {
//                binding.ucLoginStatusTextView.text = getString(R.string.wxc_loggedIn)
            } else if(webexViewModel.getCallingType() == Phone.CallingType.WebexForBroadworks)  {
//                binding.ucLoginStatusTextView.text = getString(R.string.webexforbroadworks_loggedIn)
            } else if (webexViewModel.getCallingType() == Phone.CallingType.CUCM){
//                binding.ucLoginStatusTextView.text = getString(R.string.uc_loggedIn)
            }
        } else {
//            binding.ucLoginStatusTextView.visibility = View.GONE
        }

        when (webexViewModel.ucServerConnectionStatus) {
            UCLoginServerConnectionStatus.Failed -> {
                val text = resources.getString(R.string.phone_service_failed) + " " + webexViewModel.ucServerConnectionFailureReason
//                binding.ucServerConnectionStatusTextView.text = text
//                binding.ucServerConnectionStatusTextView.visibility = View.VISIBLE
            }
            UCLoginServerConnectionStatus.Connected, UCLoginServerConnectionStatus.Connecting, UCLoginServerConnectionStatus.Disconnected -> {
                val text = resources.getString(R.string.phone_services_connection_status) + webexViewModel.ucServerConnectionStatus.name
//                binding.ucServerConnectionStatusTextView.text = text
//                binding.ucServerConnectionStatusTextView.visibility = View.VISIBLE
            }
            else -> {
//                binding.ucServerConnectionStatusTextView.visibility = View.GONE
            }
        }
    }

    private fun addVirtualBackground() {
        if (SharedPrefUtils.isVirtualBgAdded(this)) {
            Log.d(tag, "Virtual Bg is already added")
        } else {

            val thumbnailFile = FileUtils.getFileFromResource(this, "nature-thumb")
            val file = FileUtils.getFileFromResource(this, "nature")
            val thumbnail = LocalFile.Thumbnail(thumbnailFile, null,
                resources.getInteger(R.integer.virtual_bg_thumbnail_width),
                resources.getInteger(R.integer.virtual_bg_thumbnail_height))

            val localFile = LocalFile(file, null, thumbnail, null)
            webexViewModel.addVirtualBackground(localFile, CompletionHandler {
                if (it.isSuccessful && it.data != null) {
                    SharedPrefUtils.setVirtualBgAdded(this, true)
                }
            })
        }
    }

    override fun dump(
        prefix: String,
        fd: FileDescriptor?,
        writer: PrintWriter,
        args: Array<out String>?
    ) {
        super.dump(prefix, fd, writer, args)
        writer.println(" ")
        writer.println("Dump logs: ")
        webexViewModel.printObservers(writer)
    }
}