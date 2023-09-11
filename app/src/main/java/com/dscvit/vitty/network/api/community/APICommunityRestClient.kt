package com.dscvit.vitty.network.api.community

import com.dscvit.vitty.network.api.community.requests.AuthRequestBody
import com.dscvit.vitty.network.api.community.requests.UsernameRequestBody
import com.dscvit.vitty.network.api.community.responses.requests.RequestsResponse
import com.dscvit.vitty.network.api.community.responses.requests.RequestsResponseItem
import com.dscvit.vitty.network.api.community.responses.user.FriendResponse
import com.dscvit.vitty.network.api.community.responses.user.PostResponse
import com.dscvit.vitty.network.api.community.responses.user.SignInResponse
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class APICommunityRestClient {



    companion object {
        val instance = APICommunityRestClient()
    }

    private var mApiUser: APICommunity? = null
    val retrofit = CommunityNetworkClient.retrofitClientCommunity


    fun signInWithUsernameRegNo(
        username: String,
        regno: String,
        uuid: String,
        retrofitCommunitySignInListener: RetrofitCommunitySignInListener,
        retrofitSelfUserListener: RetrofitSelfUserListener
    ){

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)



        val requestBody = AuthRequestBody(
            reg_no = regno,
            username = username,
            uuid = uuid
        )

        val apiSignInCall = mApiUser!!.signInInfo(requestBody)

        apiSignInCall.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(call: Call<SignInResponse>, response: Response<SignInResponse>) {
                retrofitCommunitySignInListener.onSuccess(call, response.body())
                val token = response.body()?.token.toString()
                val res_username = response.body()?.username.toString()

                getUserWithTimeTable( token, res_username, retrofitSelfUserListener,)

            }

            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                retrofitCommunitySignInListener.onError(call, t)

            }
        })
    }

    fun getUserWithTimeTable(token: String, username: String, retrofitSelfUserListener: RetrofitSelfUserListener) {
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiUserCall =  mApiUser!!.getUser(bearerToken, username)
        apiUserCall.enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                retrofitSelfUserListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                retrofitSelfUserListener.onError(call, t)
            }
        })

    }


    fun getFriendList(token: String, username: String, retrofitFriendListListener: RetrofitFriendListListener ){
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiFriendListCall = mApiUser!!.getFriendList(bearerToken, username)
        apiFriendListCall.enqueue(object : Callback<FriendResponse> {
            override fun onResponse(call: Call<FriendResponse>, response: Response<FriendResponse>) {
                retrofitFriendListListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<FriendResponse>, t: Throwable) {
                retrofitFriendListListener.onError(call, t)
            }
        })
    }

    fun getSearchResult(
        token: String,
        query: String,
        retrofitSearchResultListener: RetrofitSearchResultListener
    ){
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiSearchResultCall = mApiUser!!.searchUsers(bearerToken,query)
        apiSearchResultCall.enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                Timber.d("SearchResult4: $response")
                retrofitSearchResultListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                retrofitSearchResultListener.onError(call, t)
            }
        })
    }

    fun getSuggestedFriends(
        token: String,
        retrofitSearchResultListener: RetrofitSearchResultListener
    ){
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiSuggestedResultCall = mApiUser!!.getSuggestedFriends(bearerToken)
        apiSuggestedResultCall.enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                Timber.d("SearchResult4: $response")
                retrofitSearchResultListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                retrofitSearchResultListener.onError(call, t)
            }
        })
    }

    fun getFriendRequest(
        token: String,
        retrofitFriendRequestListener: RetrofitFriendRequestListener
    ){
        val bearerToken = "Bearer $token"
        Timber.d("FriendReqToken--: $bearerToken")
        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiFriendRequestCall = mApiUser!!.getFriendRequests(bearerToken)
        apiFriendRequestCall.enqueue(object : Callback<RequestsResponse> {
            override fun onResponse(call: Call<RequestsResponse>, response: Response<RequestsResponse>) {
                Timber.d("FriendRequest--: $response")
                retrofitFriendRequestListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<RequestsResponse>, t: Throwable) {
                Timber.d("FriendRequestError--: ${t.message}")
                retrofitFriendRequestListener.onError(call, t)
            }
        }
        )
    }

    fun acceptRequest(token: String, username: String, retrofitUserActionListener: RetrofitUserActionListener) {
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiAcceptRequestCall = mApiUser!!.acceptRequest(bearerToken, username)
        apiAcceptRequestCall.enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                retrofitUserActionListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                retrofitUserActionListener.onError(call, t)
            }
        })

    }

    fun rejectRequest(token: String, username: String, retrofitUserActionListener: RetrofitUserActionListener) {
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiRejectRequestCall = mApiUser!!.declineRequest(bearerToken, username)
        apiRejectRequestCall.enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                retrofitUserActionListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                retrofitUserActionListener.onError(call, t)
            }
        })

    }

    fun sendRequest(token: String, username: String, retrofitUserActionListener: RetrofitUserActionListener) {
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiSendRequestCall = mApiUser!!.sendRequest(bearerToken, username)
        apiSendRequestCall.enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                retrofitUserActionListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                retrofitUserActionListener.onError(call, t)
            }
        })

    }

    fun unfriend(token: String,username: String, retrofitUserActionListener: RetrofitUserActionListener){
        val bearerToken = "Bearer $token"

        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val apiUnfriendCall = mApiUser!!.deleteFriend(bearerToken, username)
        apiUnfriendCall.enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                retrofitUserActionListener.onSuccess(call, response.body())
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                retrofitUserActionListener.onError(call, t)
            }
        })
    }

    fun checkUsername(username: String, retrofitUserActionListener: RetrofitUserActionListener) {
        mApiUser = retrofit.create<APICommunity>(APICommunity::class.java)
        val usernameRequestBody = UsernameRequestBody(username)
        val apiCheckUsernameCall = mApiUser!!.checkUsername(usernameRequestBody)
        apiCheckUsernameCall.enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                if(response.isSuccessful) {
                    retrofitUserActionListener.onSuccess(call, response.body())
                } else {
                    val gson = Gson()
                    val errorString = response.errorBody()?.string()
                    Timber.d("ResponseV: $errorString")
                    try {
                        val errorResponse = gson.fromJson(errorString, PostResponse::class.java)
                        retrofitUserActionListener.onSuccess(call, errorResponse)
                    } catch (e: JsonSyntaxException) {
                        // Handle any JSON parsing errors.
                        val errorMessage = "Username is not valid/available."
                        retrofitUserActionListener.onSuccess(call, PostResponse(errorMessage))
                    }
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                Timber.d("ErrorV: ${t.message}")
                retrofitUserActionListener.onError(call, t)
            }
        })


    }


}