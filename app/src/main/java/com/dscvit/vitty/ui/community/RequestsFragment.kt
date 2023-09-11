package com.dscvit.vitty.ui.community

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.dscvit.vitty.R
import com.dscvit.vitty.adapter.FriendAdapter
import com.dscvit.vitty.adapter.SearchAdapter
import com.dscvit.vitty.databinding.FragmentRequestsBinding
import com.dscvit.vitty.network.api.community.responses.requests.RequestsResponse
import com.dscvit.vitty.network.api.community.responses.requests.RequestsResponseItem
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.util.Constants
import timber.log.Timber

class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val communityViewModel = ViewModelProvider(this)[CommunityViewModel::class.java]

        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        prefs = requireContext().getSharedPreferences(Constants.USER_INFO, 0)
        val token  = prefs.getString(Constants.COMMUNITY_TOKEN, null)
        if(token != null){
            communityViewModel.getFriendRequest(token)
            communityViewModel.getSuggestedFriends(token)
        }
        val requestLayout = binding.requestLayout
        val suggestedList = binding.suggestedList


        binding.scheduleToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.close -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    true
                }
                else -> {
                    false
                }
            }
        }

        binding.searchFriendsText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_search,
            0,
            0,
            0
        )

        binding.searchFriendsText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                requireView().findNavController()
                    .navigate(R.id.action_navigation_requests_to_searchFragment)
            }
        }

        communityViewModel.friendRequest.observe(viewLifecycleOwner) {
            updateViewVisibility(binding.noRequests, it, communityViewModel.suggestedFriends.value)
            Timber.d("FriendRequestList--: $it")
            if (it != null) {
                val requestListParsed = getRequestList(it)
                if(requestListParsed.isNotEmpty()) {
                    binding.pendingRequestsTextView.visibility = View.VISIBLE
                    requestLayout.visibility = View.VISIBLE
                    binding.icon.load(requestListParsed[requestListParsed.size-1].picture){
                        crossfade(true)
                        placeholder(R.drawable.ic_gdscvit)
                    }
                    binding.superscriptTextView.text = requestListParsed.size.toString()
                }

            }
        }

        binding.requestLayout.setOnClickListener {
            requireView().findNavController().navigate(R.id.action_navigation_requests_to_allRequestFragment)
        }



        communityViewModel.suggestedFriends.observe(viewLifecycleOwner) {
            updateViewVisibility(binding.noRequests, communityViewModel.friendRequest.value, it)
            if (it != null) {
                binding.suggestedFriendsTextView.visibility = View.VISIBLE
                suggestedList.visibility = View.VISIBLE
                suggestedList.scheduleLayoutAnimation()
                suggestedList.adapter =
                    token?.let { token -> SearchAdapter(it, token, communityViewModel, false, false) }
                suggestedList.layoutManager = LinearLayoutManager(context)
            }
        }

        communityViewModel.actionResponse.observe(viewLifecycleOwner){
            if(it!=null){
                Timber.d("ActionResponse: $it")
                Toast.makeText(context, it.detail, Toast.LENGTH_SHORT).show()
            }
        }


        return root
    }

    private fun getRequestList(it: RequestsResponse): List<UserResponse> {
        val requestList = mutableListOf<UserResponse>()
        for (i in it){
            requestList.add(i.from)
        }
        return requestList
    }

    private fun updateViewVisibility(view: View, friendRequestData: RequestsResponse?, suggestedFriendsData: List<UserResponse>?) {
        if (friendRequestData.isNullOrEmpty() && suggestedFriendsData.isNullOrEmpty()) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}