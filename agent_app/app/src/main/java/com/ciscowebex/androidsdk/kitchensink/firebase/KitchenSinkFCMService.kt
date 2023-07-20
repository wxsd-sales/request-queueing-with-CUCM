package com.ciscowebex.androidsdk.kitchensink.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ciscowebex.androidsdk.CompletionHandler
import com.ciscowebex.androidsdk.kitchensink.HomeActivity
import com.ciscowebex.androidsdk.kitchensink.KitchenSinkApp
import com.ciscowebex.androidsdk.kitchensink.R
import com.ciscowebex.androidsdk.kitchensink.WebexRepository
import com.ciscowebex.androidsdk.kitchensink.calling.CallActivity
import com.ciscowebex.androidsdk.kitchensink.calling.CucmCallActivity
import com.ciscowebex.androidsdk.kitchensink.firebase.KitchenSinkFCMService.WebhookResources.CALL_MEMBERSHIPS
import com.ciscowebex.androidsdk.kitchensink.firebase.KitchenSinkFCMService.WebhookResources.MESSAGES
import com.ciscowebex.androidsdk.kitchensink.utils.Base64Utils
import com.ciscowebex.androidsdk.kitchensink.utils.Constants
import com.ciscowebex.androidsdk.kitchensink.utils.decryptPushRESTPayload
import com.ciscowebex.androidsdk.message.Message
import com.ciscowebex.androidsdk.phone.Call
import com.ciscowebex.androidsdk.phone.NotificationCallType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import org.json.JSONObject
import org.koin.android.ext.android.inject
import kotlin.random.Random


class KitchenSinkFCMService : FirebaseMessagingService() {

    private val repository: WebexRepository by inject()

    private fun processFCMMessage(remoteMessage: RemoteMessage){
        var notificationData: FCMPushModel?
        if (remoteMessage.data.isNotEmpty()) {
            // CUCM Push Rest Flow
            val map = remoteMessage.data
            val pushRestPayload = map["body"]
            if (!pushRestPayload.isNullOrEmpty()) {
                Handler(Looper.getMainLooper()).post{
                    if(repository.webex.authenticator?.isAuthorized() == false) {
                        repository.webex.initialize { result ->
                            if (result.error == null) {
                                Log.d(TAG, "Starting UC services in FCM service")
                                repository.webex.startUCServices()
                            }
                        }
                    }else{
                        Log.d(TAG, "Starting UC services in FCM service")
                        repository.webex.startUCServices()
                    }
                }

                Log.d(TAG, "Payload from PushREST : $pushRestPayload")
                val decryptedPayload = decryptPushRESTPayload(pushRestPayload)
                Log.d(TAG, "Decrypted payload : $decryptedPayload")
                val pushRestPayloadJson = getPushRestPayloadModel(decryptedPayload)
                buildCallNotification(pushRestPayloadJson)
            } else {
                // FCM triggered via webhook from push notification server
                val data = map["data"]
                data?.let {
                    val jsonObject = JSONObject(it)
                    Log.d(TAG, "Message data payload: remoteMessage.data -> $jsonObject")
                    notificationData = getFCMModel(jsonObject.toString())
                    when (notificationData?.resource) {
                        MESSAGES.value -> {
                            buildMessageNotification(notificationData)
                        }
                        CALL_MEMBERSHIPS.value -> {
                            //send call notification
                            notificationData?.let { data ->
                                buildCallNotification(data)
                            }
                        }
                        else -> {
                            Log.d(TAG, "Unknown resource found : Resource: ${notificationData?.resource}")
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.from)
        Log.d(TAG, "APP chg isInForeground: " + KitchenSinkApp.inForeground)

        if (KitchenSinkApp.inForeground) return

        if(!(application as KitchenSinkApp).loadModules()){
            Log.w(TAG, "Login type unknown")
            return
        }
        processFCMMessage(remoteMessage)
    }

    private fun buildCallNotification(data: FCMPushModel) {
        val callId = Base64Utils.decodeString(data.data?.callId) //locus sessionId returned
        Handler(Looper.getMainLooper()).postDelayed({
            val actualCallId = repository.getCallIdByNotificationId(callId, NotificationCallType.Webex)
            val callInfo = repository.getCall(actualCallId)
            Log.d(TAG, "CallInfo ${callInfo?.getCallId()} title ${callInfo?.getTitle()}")
            sendCallNotification(callInfo)
        }, 100)
    }

    private fun buildCallNotification(data: PushRestPayloadModel) {
        Handler(Looper.getMainLooper()).postDelayed({
            if(data.pushid != null){
                Log.d(TAG, "Pushid is "+data.pushid); //CUCM flow
                if (data.type == "incomingcall") //data.type = incomingcall,missedcall
                    sendCucmCallNotification(data.pushid, data.displaynumber)
            }else {
                Log.d(TAG, "Push id is null")
            }

        }, 10)
    }

    private fun sendCallNotification(callInfo: Call?, caller: String? = null) {
        val notificationId = Random.nextInt(10000)
        val requestCode = Random.nextInt(10000)
        val intent = CallActivity.getIncomingIntent(this)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(Constants.Intent.CALL_ID, callInfo?.getCallId())
        intent.action = Constants.Action.WEBEX_CALL_ACTION

        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT)
        val channelId: String = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_notification_icon)
                .setContentTitle("$caller is calling")
                .setContentText(getString(R.string.call_description))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    WEBEX_CALL_CHANNEL,
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager?.createNotificationChannel(channel)
        }
        notificationManager?.notify(notificationId, notificationBuilder.build())
    }

    private fun sendCucmCallNotification(pushId: String?, caller: String? = null) {
        val notificationId = Random.nextInt(10000)
        val requestCode = Random.nextInt(10000)
        val intent = CucmCallActivity.getIncomingIntent(this, pushId)


        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT)
        val channelId: String = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_notification_icon)
                .setContentTitle("$caller is calling")
                .setContentText(getString(R.string.call_description))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    WEBEX_CALL_CHANNEL,
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager?.createNotificationChannel(channel)
        }
        notificationManager?.notify(notificationId, notificationBuilder.build())
    }

    private fun buildMessageNotification(notificationData: FCMPushModel?) {
        val roomId = Base64Utils.decodeString(notificationData?.data?.roomId)
        repository.listMessages(roomId, CompletionHandler {
            Log.d(TAG, "message size: ${it.data?.size}")
            val size = it.data?.size ?: 0
            if (size > 0) {
                val message = it.data?.get(size - 1)
                Log.d(TAG, "last message: ${message?.getText()}")

                Log.d(TAG, "Fetching person details")
                repository.getPerson(Base64Utils.decodeString(notificationData?.data?.personId), CompletionHandler { personResult ->
                    Log.d(TAG, "Fetching space details")
                    repository.getSpace(Base64Utils.decodeString(notificationData?.data?.roomId), CompletionHandler { spaceResult ->
                        sendNotification(personResult.data?.displayName.orEmpty(), spaceResult.data?.title.orEmpty(), message)
                    })
                })
            } else {
                Log.d(TAG, "message not found")
            }
        })
    }

    private fun getFCMModel(data: String): FCMPushModel {
        return Gson().fromJson(data, FCMPushModel::class.java)
    }

    private fun getPushRestPayloadModel(data: String): PushRestPayloadModel {
        return Gson().fromJson(data, PushRestPayloadModel::class.java)
    }

    /**
     * Called if FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve
     * the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     */
    private fun sendNotification(personTitle: String, spaceTitle: String, message: Message?) {
        val notificationId = Random.nextInt(10000)
        val requestCode = Random.nextInt(10000)
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(Constants.Bundle.MESSAGE_ID, message?.getId().orEmpty())
        intent.action = Constants.Action.MESSAGE_ACTION

        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT)
        val channelId: String = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_notification_icon)
                .setContentTitle(spaceTitle)
                .setContentText(personTitle)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setStyle(
                        NotificationCompat.BigTextStyle()
                                .bigText(Html.fromHtml(message?.getText().orEmpty(), Html.FROM_HTML_MODE_LEGACY))
                )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    MESSAGE_CHANNEL,
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager?.createNotificationChannel(channel)
        }
        notificationManager?.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "PUSHREST"
        private const val WEBEX_CALL_CHANNEL = "WebexCallChannel"
        private const val CUCM_CALL_CHANNEL = "CUCMCallChannel"
        private const val MESSAGE_CHANNEL = "MessageChannel"
    }

    enum class WebhookResources(var value: String) {
        MESSAGES("messages"), CALL_MEMBERSHIPS("callMemberships")
    }
}