package com.dscvit.vitty.ui.community

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dscvit.vitty.R
import com.dscvit.vitty.adapter.FriendAdapter
import com.dscvit.vitty.adapter.PeriodAdapter
import com.dscvit.vitty.databinding.FragmentCommunityBinding
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.Quote
import timber.log.Timber

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var communityViewModel: CommunityViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        communityViewModel = ViewModelProvider(this)[CommunityViewModel::class.java]

        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //get token and username from shared preferences
        val sharedPreferences = activity?.getSharedPreferences(Constants.USER_INFO, Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString(Constants.COMMUNITY_TOKEN, "") ?: ""
        val username = sharedPreferences?.getString(Constants.COMMUNITY_USERNAME, "") ?: ""
        Timber.d("TokenComm: $token")
        communityViewModel.getFriendList(token, username)
        getData()
        binding.communityToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.requests -> {
                    requireView().findNavController()
                        .navigate(R.id.action_navigation_community_to_navigation_requests)
                    true
                }
                else -> {
                    false
                }
            }
        }


        return root
    }

    private fun getData() {
        communityViewModel.friendList.observe(viewLifecycleOwner) {
            if (it != null) {
                val allFriends = it.data
                binding.apply {
                    if (!allFriends.isNullOrEmpty()) {
                        friendsList.scheduleLayoutAnimation()
                        friendsList.adapter = FriendAdapter(allFriends)
                        friendsList.layoutManager = LinearLayoutManager(context)
                        noFriends.visibility = View.INVISIBLE
                    } else {
                        /*binding.quoteLine.text = try {
                            Quote.getLine(context)
                        } catch (_: Exception) {
                            Constants.DEFAULT_QUOTE
                        }*/
                        noFriends.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}