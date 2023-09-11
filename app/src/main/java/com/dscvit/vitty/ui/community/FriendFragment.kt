package com.dscvit.vitty.ui.community

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.dscvit.vitty.BuildConfig
import com.dscvit.vitty.R
import com.dscvit.vitty.adapter.DayAdapter
import com.dscvit.vitty.databinding.FragmentFriendBinding
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.util.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import timber.log.Timber
import java.util.Calendar


class FriendFragment : Fragment() {


    private lateinit var binding: FragmentFriendBinding
    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private lateinit var communityViewModel: CommunityViewModel
    private lateinit var token: String
    private lateinit var sharedPrefs : SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFriendBinding.inflate(inflater, container, false)
        communityViewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        sharedPrefs = requireActivity().getSharedPreferences(Constants.USER_INFO, Context.MODE_PRIVATE)
        val name = arguments?.getString("name")
        val username = arguments?.getString("username")
        val picture = arguments?.getString("profile_picture")
        val friendStatus = arguments?.getString("friend_status")
        token = sharedPrefs.getString(Constants.COMMUNITY_TOKEN, null) ?: ""

        setUpPage(name, username, picture, friendStatus)
        Timber.d("FriendFragment: $name $username $picture $friendStatus")
        return binding.root


    }

    private fun setUpPage(name:String?, username:String?, picture:String?, friendStatus:String?) {

        binding.friendTimetableName.text = name ?: "Friend"
        binding.friendTimetableUsername.text = username ?: "username"
        binding.profileImage.load(picture) {
            crossfade(true)
            placeholder(R.drawable.ic_gdscvit)
            error(R.drawable.ic_gdscvit)
        }

        binding.back.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        setUpWithStatus(username, friendStatus)





    }

    private fun setUpWithStatus(username: String?, friendStatus: String?) {
        when (friendStatus) {
            "received" -> {
                requestReceivedLayout()
            }
            "sent" -> {
                requestSentLayout()
            }
            "none" -> {
                sendRequestLayout()
            }
            else -> {
                friendLayout(username)
            }
        }

        setupActions(username, friendStatus)

    }

    private fun requestReceivedLayout() {
        binding.acceptRejectLayout.visibility = View.VISIBLE
        binding.sendRequest.visibility = View.GONE
        binding.requestSent.visibility = View.GONE
        binding.unfriend.visibility = View.GONE
        binding.tabsLayout.visibility = View.GONE
        binding.pager.visibility = View.GONE
        binding.notFriend.visibility = View.VISIBLE
    }

    private fun requestSentLayout() {
        binding.acceptRejectLayout.visibility = View.GONE
        binding.sendRequest.visibility = View.GONE
        binding.requestSent.visibility = View.VISIBLE
        binding.unfriend.visibility = View.GONE
        binding.tabsLayout.visibility = View.GONE
        binding.pager.visibility = View.GONE
        binding.notFriend.visibility = View.VISIBLE
    }

    private fun sendRequestLayout() {
        binding.acceptRejectLayout.visibility = View.GONE
        binding.sendRequest.visibility = View.VISIBLE
        binding.requestSent.visibility = View.GONE
        binding.unfriend.visibility = View.GONE

        binding.tabsLayout.visibility = View.GONE
        binding.pager.visibility = View.GONE
        binding.notFriend.visibility = View.VISIBLE
    }

    private fun setupActions(username: String?, friendStatus: String?) {
        if(username == null) return
        binding.accept.setOnClickListener {
            acceptRequest(username)
            friendLayout(username)
        }
        binding.reject.setOnClickListener {
            rejectRequest(username)
            sendRequestLayout()
        }
        binding.sendRequest.setOnClickListener {
            sendRequest(username)
            requestSentLayout()
        }
        binding.unfriend.setOnClickListener {
            handleUnfriendAction(username)

        }

    }

    private fun handleUnfriendAction(username: String) {

        val v: View = LayoutInflater
            .from(context)
            .inflate(R.layout.dialog_setup_complete, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(v)
            .setBackground(
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.color.transparent
                )
            )
            .create()
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()

        val skip = v.findViewById<Button>(R.id.skip)
        val next = v.findViewById<Button>(R.id.next)
        val title = v.findViewById<TextView>(R.id.title)
        val desc = v.findViewById<TextView>(R.id.description)

        title.text = "Unfriend"
        desc.text = "Are you sure you want to unfriend this person?"
        skip.text = "Cancel"
        next.text = "Unfriend"

        skip.setOnClickListener {
            dialog.dismiss()
        }

        next.setOnClickListener {
            unfriend(username)
            sendRequestLayout()
            dialog.dismiss()
        }

    }

    private fun friendLayout(username: String?) {
        binding.acceptRejectLayout.visibility = View.GONE
        binding.sendRequest.visibility = View.GONE
        binding.requestSent.visibility = View.GONE
        binding.unfriend.visibility = View.VISIBLE
        binding.notFriend.visibility = View.GONE
        binding.tabsLayout.visibility = View.VISIBLE
        binding.pager.visibility = View.VISIBLE
        setUpTimeTable(username)
    }

    private fun setUpTimeTable(username: String?) {

        val calendar: Calendar = Calendar.getInstance()
        val d = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val pagerAdapter = DayAdapter(this, username)
        binding.pager.adapter = pagerAdapter
        TabLayoutMediator(
            binding.tabs, binding.pager
        ) { tab, position -> tab.text = days[position] }
            .attach()

        binding.pager.currentItem = d

    }


    private fun acceptRequest(username: String) {
        communityViewModel.acceptRequest(token, username)
    }

    private fun rejectRequest(username: String) {
        communityViewModel.rejectRequest(token, username)
    }

    private fun sendRequest(username: String) {
        communityViewModel.sendRequest(token, username)
    }

    private fun unfriend(username: String) {
        communityViewModel.unfriend(token, username)
    }
}