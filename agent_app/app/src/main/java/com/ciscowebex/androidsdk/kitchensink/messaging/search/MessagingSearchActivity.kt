package com.ciscowebex.androidsdk.kitchensink.messaging.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ciscowebex.androidsdk.kitchensink.R

/**
 * Simple search activity that has Search Fragment inside
 */
class MessagingSearchActivity : AppCompatActivity() {
    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, MessagingSearchActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_messaging)
    }
}