package com.ciscowebex.androidsdk.kitchensink.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.ciscowebex.androidsdk.kitchensink.HomeActivity
import com.ciscowebex.androidsdk.kitchensink.KitchenSinkApp
import com.ciscowebex.androidsdk.kitchensink.R
import com.ciscowebex.androidsdk.kitchensink.databinding.ActivityLoginWithTokenBinding
import com.ciscowebex.androidsdk.kitchensink.utils.showDialogWithMessage
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class JWTLoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginWithTokenBinding
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityLoginWithTokenBinding>(this, R.layout.activity_login_with_token)
                .also { binding = it }
                .apply {
//                    title.text = getString(R.string.login_jwt)
//                    progressLayout.visibility = View.VISIBLE
//                    loginButton.setOnClickListener {
//                        binding.loginFailedTextView.visibility = View.GONE
//                        if (tokenText.text.isEmpty()) {
//                            showDialogWithMessage(this@JWTLoginActivity, R.string.error_occurred, resources.getString(R.string.login_token_empty_error))
//                        }
//                        else {
//                            binding.loginButton.visibility = View.GONE
//                            progressLayout.visibility = View.VISIBLE
//                            val token = tokenText.text.toString()
//                            loginViewModel.loginWithJWT(token)
//                        }
//                    }

                    progressLayout.visibility = View.VISIBLE
                    val key="SrjG27M9XzNoxTTeXrWVPBhFYyFzL8ZOhseZmL2Kzas="
                    val seckey = Keys.hmacShaKeyFor((Decoders.BASE64.decode(key)))
                    //Log.e("Secret encoded",seckey.toString())
                    val now = Date()
                    val jwt = Jwts.builder()
                        .setHeaderParam("typ", "JWT")
                        .setHeaderParam("alg","HS256")
                        .claim("sub", "guest90")
                        .claim("name","Test")
                        .claim("iss","Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi84OTc4M2Y2Yi04ZjdkLTRhYzEtYTAyYS1kYzA0NGFhNTc4NDU")
                        .setExpiration(Date(now.time + 2 * 1000 * 60 * 60))
                        .signWith(seckey)
                        .compact()
                    Log.e("JWTToken",jwt)
                    ///binding.loginButton.visibility = View.GONE
                    //progressLayout.visibility = View.VISIBLE
                    val token = jwt
                    loginViewModel.loginWithJWT(token)

                    loginViewModel.isAuthorized.observe(this@JWTLoginActivity, Observer { isAuthorized ->
                        progressLayout.visibility = View.GONE
                        isAuthorized?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
                                onLoginFailed()
                            }
                        }
                    })

                    loginViewModel.isAuthorizedCached.observe(this@JWTLoginActivity, Observer { isAuthorizedCached ->
                        progressLayout.visibility = View.GONE
                        isAuthorizedCached?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
//                                tokenText.visibility = View.VISIBLE
//                                loginButton.visibility = View.VISIBLE
//                                loginFailedTextView.visibility = View.GONE
                            }
                        }
                    })

                    loginViewModel.errorData.observe(this@JWTLoginActivity, Observer { errorMessage ->
                        progressLayout.visibility = View.GONE
                        onLoginFailed(errorMessage)
                    })

                    loginViewModel.initialize()
                }
    }

    override fun onBackPressed() {
        (application as KitchenSinkApp).closeApplication()
    }

    private fun onLoggedIn() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun onLoginFailed(failureMessage: String = getString(R.string.jwt_login_failed)) {
//        binding.loginButton.visibility = View.VISIBLE
//        binding.loginFailedTextView.visibility = View.VISIBLE
//        binding.loginFailedTextView.text = failureMessage
    }
}