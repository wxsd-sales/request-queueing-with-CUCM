package com.ciscowebex.androidsdk.kitchensink

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.ciscowebex.androidsdk.CompletionHandler
import com.ciscowebex.androidsdk.auth.OAuthWebViewAuthenticator
import com.ciscowebex.androidsdk.auth.TokenAuthenticator
import com.ciscowebex.androidsdk.kitchensink.auth.LoginActivity
import com.ciscowebex.androidsdk.kitchensink.databinding.ActivityHomeBinding
import com.ciscowebex.androidsdk.kitchensink.messaging.MessagingActivity
import com.ciscowebex.androidsdk.kitchensink.cucm.UCLoginActivity
import com.ciscowebex.androidsdk.kitchensink.messaging.spaces.detail.MessageDetailsDialogFragment
import com.ciscowebex.androidsdk.kitchensink.person.PersonDialogFragment
import com.ciscowebex.androidsdk.kitchensink.person.PersonViewModel
import com.ciscowebex.androidsdk.kitchensink.utils.Constants
import com.ciscowebex.androidsdk.kitchensink.utils.SharedPrefUtils.clearLoginTypePref
import com.ciscowebex.androidsdk.kitchensink.utils.SharedPrefUtils.saveLoginTypePref
import com.ciscowebex.androidsdk.kitchensink.webhooks.WebhooksActivity
import com.ciscowebex.androidsdk.auth.UCLoginServerConnectionStatus
import com.ciscowebex.androidsdk.kitchensink.calling.CallActivity
import com.ciscowebex.androidsdk.kitchensink.extras.ExtrasActivity
import com.ciscowebex.androidsdk.kitchensink.search.SearchActivity
import com.ciscowebex.androidsdk.kitchensink.setup.SetupActivity
import com.ciscowebex.androidsdk.kitchensink.utils.FileUtils
import com.ciscowebex.androidsdk.kitchensink.utils.SharedPrefUtils
import com.ciscowebex.androidsdk.message.LocalFile
import com.ciscowebex.androidsdk.phone.Phone
import org.koin.android.ext.android.inject

class HomeActivity : BaseActivity() {

    lateinit var binding: ActivityHomeBinding
    private val personViewModel : PersonViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tag = "HomeActivity"

        val authenticator = webexViewModel.webex.authenticator

        webexViewModel.enableBackgroundConnection(webexViewModel.enableBgConnectiontoggle)
        webexViewModel.setLogLevel(webexViewModel.logFilter)
        webexViewModel.enableConsoleLogger(webexViewModel.isConsoleLoggerEnabled)

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
                else {
                    binding.progressLayout.visibility = View.GONE
                }
            }
        })


        webexViewModel.cucmLiveData.observe(this@HomeActivity, Observer {
            if (it != null) {
                when (WebexRepository.CucmEvent.valueOf(it.first.name)) {
                    WebexRepository.CucmEvent.OnUCServerConnectionStateChanged -> {
                        updateUCData()
                    }
                    else -> {}
                }
            }
        })

        webexViewModel.incomingListenerLiveData.observe(this@HomeActivity, Observer {
            it?.let {
                Log.d(tag, "incomingListenerLiveData: ${it.getCallId()}")
                Handler(Looper.getMainLooper()).post {
                    startActivity(CallActivity.getIncomingIntent(this, it.getCallId()))
                }
            }
        })

        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)
                .also { binding = it }
                .apply {

                    ivStartCall.setOnClickListener {
                        startActivity(Intent(this@HomeActivity, SearchActivity::class.java))
//                        startActivity(it.context?.let { ctx -> CallActivity.getOutgoingIntent(ctx, "25a9dcc6-c8d3-3b8f-a402-13c65b62e519@appid.ciscospark.com") })
                    }

                    ivLogout.setOnClickListener {
                        progressLayout.visibility = View.VISIBLE
                        webexViewModel.signOut()
                    }
                    ivGetMe.setOnClickListener{
                        ivNoRequestText.visibility=View.GONE
                        infoText.visibility=View.VISIBLE
                        cardView.visibility=View.VISIBLE
                    }
                    ivStartCallReceived.setOnClickListener {
                        ivNoRequestText.visibility=View.VISIBLE
                        infoText.visibility=View.GONE
                        cardView.visibility=View.GONE
                        startActivity(it.context?.let { ctx -> CallActivity.getOutgoingIntent(ctx, "25a9dcc6-c8d3-3b8f-a402-13c65b62e519@appid.ciscospark.com") })
                    }

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
        startActivity(UCLoginActivity.getIntent(this@HomeActivity))
    }

    private fun observeUCLoginData() {
        webexViewModel.cucmLiveData.observe(this@HomeActivity, Observer {
            Log.d(tag, "uc login observer called : ${it.first.name}")
            if (it != null) {
                when (WebexRepository.CucmEvent.valueOf(it.first.name)) {
                    WebexRepository.CucmEvent.OnUCLoggedIn, WebexRepository.CucmEvent.OnUCServerConnectionStateChanged -> {
                        updateUCData()
                    }
                    WebexRepository.CucmEvent.ShowSSOLogin -> {
                        startActivity(UCLoginActivity.getIntent(this@HomeActivity,
                            UCLoginActivity.Companion.OnActivityStartAction.ShowSSOLogin.name,
                            it.second))
                    }

                    WebexRepository.CucmEvent.ShowNonSSOLogin -> {
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
    }

    private fun updateUCData() {
        Log.d(tag, "updateUCData isCUCMServerLoggedIn: ${webexViewModel.repository.isCUCMServerLoggedIn} ucServerConnectionStatus: ${webexViewModel.repository.ucServerConnectionStatus}")
        if (webexViewModel.isCUCMServerLoggedIn) {
//            binding.ucLoginStatusTextView.visibility = View.VISIBLE
        } else {
//            binding.ucLoginStatusTextView.visibility = View.GONE
        }

        when (webexViewModel.ucServerConnectionStatus) {
            UCLoginServerConnectionStatus.Failed -> {
                val text = resources.getString(R.string.phone_service_failed) + " " + webexViewModel.ucServerConnectionFailureReason
//                binding.ucServerConnectionStatusTextView.text = text
//                binding.ucServerConnectionStatusTextView.visibility = View.VISIBLE
            }
            UCLoginServerConnectionStatus.Connected, UCLoginServerConnectionStatus.Connecting -> {
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
}