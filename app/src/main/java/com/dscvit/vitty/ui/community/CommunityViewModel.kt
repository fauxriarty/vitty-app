package com.dscvit.vitty.ui.community

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dscvit.vitty.network.api.community.APICommunityRestClient
import com.dscvit.vitty.network.api.community.RetrofitFriendListListener
import com.dscvit.vitty.network.api.community.RetrofitFriendRequestListener
import com.dscvit.vitty.network.api.community.RetrofitSearchResultListener
import com.dscvit.vitty.network.api.community.RetrofitUserActionListener
import com.dscvit.vitty.network.api.community.responses.requests.RequestsResponse
import com.dscvit.vitty.network.api.community.responses.requests.RequestsResponseItem
import com.dscvit.vitty.network.api.community.responses.user.FriendResponse
import com.dscvit.vitty.network.api.community.responses.user.PostResponse
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import retrofit2.Call
import timber.log.Timber

class CommunityViewModel : ViewModel() {


    private val _friendList = MutableLiveData<FriendResponse?>()
    private val _friendRequest = MutableLiveData<RequestsResponse?>()
    private val _suggestedFriends = MutableLiveData<List<UserResponse>?>()
    private val _searchResult  = MutableLiveData<List<UserResponse>?>()

    private val _actionResponse = MutableLiveData<PostResponse?>()

    val friendList: MutableLiveData<FriendResponse?> = _friendList
    val friendRequest: MutableLiveData<RequestsResponse?> = _friendRequest
    val suggestedFriends: MutableLiveData<List<UserResponse>?> = _suggestedFriends
    val searchResult: MutableLiveData<List<UserResponse>?> = _searchResult
    val actionResponse: MutableLiveData<PostResponse?> = _actionResponse

    fun getFriendList(token: String, username: String) {
        APICommunityRestClient.instance.getFriendList(token, username,

            object : RetrofitFriendListListener {
                override fun onSuccess(call: Call<FriendResponse>?, response: FriendResponse?) {
                    _friendList.postValue(response)
                }

                override fun onError(call: Call<FriendResponse>?, t: Throwable?) {
                    _friendList.postValue(null)
                }
            })
    }

    fun getSearchResult(token: String, query: String) {
        APICommunityRestClient.instance.getSearchResult(token, query,

            object: RetrofitSearchResultListener{
                override fun onSuccess(call: Call<List<UserResponse>>?, response: List<UserResponse>?) {
                    Timber.d("SearchResult2: $response")
                    _searchResult.postValue(response)
                }

                override fun onError(call: Call<List<UserResponse>>?, t: Throwable?) {
                    Timber.d("SearchResult3: ${t?.message}")
                    _searchResult.postValue(null)
                }
            })
    }

    fun getFriendRequest(token: String) {
        APICommunityRestClient.instance.getFriendRequest(token,

            object : RetrofitFriendRequestListener {
                override fun onSuccess(call: Call<RequestsResponse>?, response: RequestsResponse?) {
                    Timber.d("FriendRequest--: $response")
                    _friendRequest.postValue(response)
                }

                override fun onError(call: Call<RequestsResponse>?, t: Throwable?) {
                    _friendRequest.postValue(null)
                }
            })
            }


    fun getSuggestedFriends(token: String){
        APICommunityRestClient.instance.getSuggestedFriends(token,
            object: RetrofitSearchResultListener{
                override fun onSuccess(call: Call<List<UserResponse>>?, response: List<UserResponse>?) {
                    Timber.d("SearchResult2Sugg: $response")
                    _suggestedFriends.postValue(response)
                }

                override fun onError(call: Call<List<UserResponse>>?, t: Throwable?) {
                    Timber.d("SearchResult3: ${t?.message}")
                    _suggestedFriends.postValue(null)
                }
            })
    }

    fun acceptRequest(token: String, userResponse: UserResponse) {
        APICommunityRestClient.instance.acceptRequest(token,userResponse.username,
            object: RetrofitUserActionListener{
                override fun onSuccess(call: Call<PostResponse>?, response: PostResponse?) {
                    Timber.d("AcceptRequest: $response")
                    _actionResponse.postValue(response)
                }

                override fun onError(call: Call<PostResponse>?, t: Throwable?) {
                    Timber.d("AcceptRequest: ${t?.message}")
                }
            })
    }

    fun rejectRequest(token:String, userResponse: UserResponse) {
        APICommunityRestClient.instance.rejectRequest(token,userResponse.username,
            object: RetrofitUserActionListener{
                override fun onSuccess(call: Call<PostResponse>?, response: PostResponse?) {
                    Timber.d("RejectRequest: $response")
                    _actionResponse.postValue(response)
                }

                override fun onError(call: Call<PostResponse>?, t: Throwable?) {
                    Timber.d("RejectRequest: ${t?.message}")
                }
            })
    }

    fun sendRequest(token:String,userResponse: UserResponse) {
        APICommunityRestClient.instance.sendRequest(token,userResponse.username,
            object: RetrofitUserActionListener{
                override fun onSuccess(call: Call<PostResponse>?, response: PostResponse?) {
                    Timber.d("SendRequest: $response")
                    _actionResponse.postValue(response)
                }

                override fun onError(call: Call<PostResponse>?, t: Throwable?) {
                    Timber.d("SendRequest: ${t?.message}")
                }
            })
    }
}