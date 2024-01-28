package com.dscvit.vitty.ui.community

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dscvit.vitty.R
import com.dscvit.vitty.adapter.SearchAdapter
import com.dscvit.vitty.databinding.FragmentAllRequestBinding
import com.dscvit.vitty.network.api.community.responses.requests.RequestsResponse
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.util.Constants
import timber.log.Timber


class AllRequestFragment : Fragment() {

    private lateinit var binding: FragmentAllRequestBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var communityViewModel: CommunityViewModel
    private lateinit var searchAdapter: SearchAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAllRequestBinding.inflate(inflater, container, false)
        communityViewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        val requestList = binding.requestList

        prefs = requireContext().getSharedPreferences(Constants.USER_INFO, 0)
        val token = prefs.getString(Constants.COMMUNITY_TOKEN, null)
        if (token != null) {
            communityViewModel.getFriendRequest(token)
        }
        binding.reqToolbar.setNavigationIcon(R.drawable.ic_back)
        binding.reqToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }


        communityViewModel.friendRequest.observe(viewLifecycleOwner) {
            updateViewVisibility(binding.noRequests, it)
            Timber.d("FriendRequestList--: $it")
            if (it != null && token != null && it.isNotEmpty()) {
                val requestListParsed = getRequestList(it)
                if (requestListParsed.isNotEmpty()) {
                    requestList.visibility = View.VISIBLE
                    requestList.scheduleLayoutAnimation()
                    searchAdapter = SearchAdapter(
                        requestListParsed,
                        token, communityViewModel, isSearchMode = false, isAllReqPage = true
                    )
                    requestList.adapter = searchAdapter

                    requestList.layoutManager = LinearLayoutManager(context)
                }

            } else {
                requestList.visibility = View.GONE
                binding.noRequests.visibility = View.VISIBLE
            }
        }

        communityViewModel.actionResponse.observe(viewLifecycleOwner) {
            if (it != null) {
                Timber.d("ActionResponse: $it")
                Toast.makeText(context, it.detail, Toast.LENGTH_SHORT).show()
            }
        }


        return binding.root
    }

    private fun getRequestList(it: RequestsResponse): List<UserResponse> {
        val requestList = mutableListOf<UserResponse>()
        for (i in it) {
            requestList.add(i.from)
        }
        return requestList
    }

    private fun updateViewVisibility(view: View, friendRequestData: RequestsResponse?) {
        if (friendRequestData.isNullOrEmpty()) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

}