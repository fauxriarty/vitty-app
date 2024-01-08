package com.dscvit.vitty.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dscvit.vitty.network.api.community.APICommunityRestClient
import com.dscvit.vitty.network.api.community.RetrofitCommunitySignInListener
import com.dscvit.vitty.network.api.community.RetrofitSelfUserListener
import com.dscvit.vitty.network.api.community.RetrofitUserActionListener
import com.dscvit.vitty.network.api.community.responses.user.PostResponse
import com.dscvit.vitty.network.api.community.responses.user.SignInResponse
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import retrofit2.Call
import timber.log.Timber

class AuthViewModel : ViewModel() {


    private val _usernameValidity = MutableLiveData<PostResponse?>()
    private val _signInResponse = MutableLiveData<SignInResponse?>()
    private val _user = MutableLiveData<UserResponse?>()

    val usernameValidity: MutableLiveData<PostResponse?> = _usernameValidity
    val signInResponse: MutableLiveData<SignInResponse?> = _signInResponse
    val user: MutableLiveData<UserResponse?> = _user

    //user response has timetable as well

    fun signInAndGetTimeTable(username: String, regno: String, uuid: String) {
        APICommunityRestClient.instance.signInWithUsernameRegNo(username, regno, uuid,
            object : RetrofitCommunitySignInListener {

                override fun onSuccess(call: Call<SignInResponse>?, response: SignInResponse?) {
                    Timber.d("username: $username regno: $regno uuid: $uuid")
                    Timber.d("Response: ${response}")
                    _signInResponse.postValue(response)

                }

                override fun onError(call: Call<SignInResponse>?, t: Throwable?) {
                    Timber.d("Error: ${t?.message}")
                    _signInResponse.postValue(null)

                }
            },
            object : RetrofitSelfUserListener {
                override fun onSuccess(call: Call<UserResponse>?, response: UserResponse?) {
                    _user.postValue(response)
                }

                override fun onError(call: Call<UserResponse>?, t: Throwable?) {
                    _user.postValue(null)
                }
            })
    }

    fun checkUsername(username: String) {
        APICommunityRestClient.instance.checkUsername(username,
            object : RetrofitUserActionListener {
                override fun onSuccess(call: Call<PostResponse>?, response: PostResponse?) {
                    _usernameValidity.postValue(response)
                    Timber.d("Response: ${response}")
                }

                override fun onError(call: Call<PostResponse>?, t: Throwable?) {
                    Timber.d("Error: ${t?.message}")
                }
            })
    }

    fun getUserWithTimeTable(token: String, username: String) {
        APICommunityRestClient.instance.getUserWithTimeTable(token, username,

            object : RetrofitSelfUserListener {
                override fun onSuccess(call: Call<UserResponse>?, response: UserResponse?) {
                    _user.postValue(response)
                }

                override fun onError(call: Call<UserResponse>?, t: Throwable?) {
                    _user.postValue(null)
                }
            })
    }

}