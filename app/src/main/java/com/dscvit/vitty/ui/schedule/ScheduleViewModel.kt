package com.dscvit.vitty.ui.schedule

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dscvit.vitty.network.api.community.APICommunityRestClient
import com.dscvit.vitty.network.api.community.RetrofitSelfUserListener
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import retrofit2.Call

class ScheduleViewModel : ViewModel() {


    private val _user = MutableLiveData<UserResponse?>()

    val user: MutableLiveData<UserResponse?> = _user
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