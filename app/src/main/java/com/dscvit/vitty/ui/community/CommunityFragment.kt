package com.dscvit.vitty.ui.community

import android.content.Context
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
import com.dscvit.vitty.R
import com.dscvit.vitty.adapter.FriendAdapter
import com.dscvit.vitty.adapter.PeriodAdapter
import com.dscvit.vitty.adapter.PinnedFriendAdapterListener
import com.dscvit.vitty.databinding.FragmentCommunityBinding
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.Quote
import timber.log.Timber

class CommunityFragment : Fragment(), PinnedFriendAdapterListener{

    private var _binding: FragmentCommunityBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var communityViewModel: CommunityViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var friendAdapter: FriendAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        communityViewModel = ViewModelProvider(this)[CommunityViewModel::class.java]

        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //get token and username from shared preferences
        sharedPreferences = requireActivity().getSharedPreferences(Constants.USER_INFO, Context.MODE_PRIVATE)
        val token = sharedPreferences.getString(Constants.COMMUNITY_TOKEN, "") ?: ""
        val username = sharedPreferences.getString(Constants.COMMUNITY_USERNAME, "") ?: ""
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
                        friendAdapter = FriendAdapter(allFriends, this@CommunityFragment)
                        friendsList.adapter = friendAdapter
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

    override fun pinFriend(username: String): Boolean {

        val friend1 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_1, null)
        val friend2 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_2, null)
        val friend3 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_3, null)
        if(friend1 == null){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_1, username).apply()
        }else if(friend2 == null){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_2, username).apply()
        }else if(friend3 == null){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_3, username).apply()
        }else{
            Toast.makeText(context, "You can pin only 3 friends", Toast.LENGTH_SHORT).show()
            return false
        }

        Toast.makeText(context, "Pinned $username", Toast.LENGTH_SHORT).show()
        return true

    }

    override fun getPinnedFriends(): List<String> {
        val friend1 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_1, null)
        val friend2 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_2, null)
        val friend3 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_3, null)

        val pinnedFriends = mutableListOf<String>()
        if(friend1 != null){
            pinnedFriends.add(friend1)
        }
        if(friend2 != null){
            pinnedFriends.add(friend2)
        }
        if(friend3 != null){
            pinnedFriends.add(friend3)
        }

        return pinnedFriends
    }

    override fun unPinFriend(username: String): Boolean {
        val friend1 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_1, null)
        val friend2 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_2, null)
        val friend3 = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_3, null)
        if(friend1 == username){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_1, null).apply()
        }else if(friend2 == username){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_2, null).apply()
        }else if(friend3 == username){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_3, null).apply()
        }else{
            Toast.makeText(context, "Error in unpinning $username", Toast.LENGTH_SHORT).show()
            return false
        }

        //sort pins
        val friend1New = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_1, null)
        val friend2New = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_2, null)
        val friend3New = sharedPreferences.getString(Constants.COMMUNITY_PINNED_FRIEND_3, null)
        if(friend1New == null && friend2New != null){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_1, friend2New).apply()
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_2, null).apply()
        }
        if(friend2New == null && friend3New != null){
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_2, friend3New).apply()
            sharedPreferences.edit().putString(Constants.COMMUNITY_PINNED_FRIEND_3, null).apply()
        }

        Toast.makeText(context, "Unpinned $username", Toast.LENGTH_SHORT).show()
        return true
    }


}