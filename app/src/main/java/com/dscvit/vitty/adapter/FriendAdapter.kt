package com.dscvit.vitty.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.adapters.ViewBindingAdapter.setOnLongClickListener
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dscvit.vitty.R
import com.dscvit.vitty.databinding.CardFriendBinding
import com.dscvit.vitty.databinding.CardPeriodBinding
import com.dscvit.vitty.model.PeriodDetails
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.util.Effects.vibrateOnClick
import com.dscvit.vitty.util.RemoteConfigUtils
import com.dscvit.vitty.util.UtilFunctions.copyItem
import com.dscvit.vitty.util.VITMap
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FriendAdapter(dataList: List<UserResponse>, private val pinnedFriendAdapterListener: PinnedFriendAdapterListener) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>() {


    private val dataSet = pinFriendsOnTop(dataList).toMutableList()
    class ViewHolder(private val binding: CardFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val friend_name = binding.friendName
        val friend_class = binding.friendClass
        val friend_status = binding.friendStatus
        val friend_image = binding.icon
        val pin = binding.pin

        fun bind(data: UserResponse) {
            binding.friendDetails = data
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.card_friend,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[holder.adapterPosition]
        holder.bind(item)


       /* val startTime: Date = item.startTime.toDate()
        val simpleDateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val sTime: String = simpleDateFormat.format(startTime).uppercase(Locale.ROOT)

        val endTime: Date = item.endTime.toDate()
        val eTime: String = simpleDateFormat.format(endTime).uppercase(Locale.ROOT)*/

        /*val now = Calendar.getInstance()
        val s = Calendar.getInstance()
        s.time = startTime
        val start = Calendar.getInstance()
        start[Calendar.HOUR_OF_DAY] = s[Calendar.HOUR_OF_DAY]
        start[Calendar.MINUTE] = s[Calendar.MINUTE]
        val e = Calendar.getInstance()
        e.time = endTime
        val end = Calendar.getInstance()
        end[Calendar.HOUR_OF_DAY] = e[Calendar.HOUR_OF_DAY]
        end[Calendar.MINUTE] = e[Calendar.MINUTE]*/

        holder.friend_status.text = item.current_status?.venue ?: "Free"
        val status = item.current_status?.status
        val course = item.current_status?.`class`

        holder.friend_class.text = if (status == null || status.lowercase() == "free" || status.lowercase() == "unknown") {
            "Not in a class right now"
        } else {
            "$course"
        }
        val pinnedFriends = pinnedFriendAdapterListener.getPinnedFriends()
        if(pinnedFriends.contains(item.username)){
            holder.pin.visibility = View.VISIBLE
        }else{
            holder.pin.visibility = View.GONE
        }

        holder.friend_image.load(item.picture) {
            crossfade(true)
            placeholder(R.drawable.ic_gdscvit)
            error(R.drawable.ic_gdscvit)
        }

        holder.itemView.apply {
            setOnClickListener {
                val bundle = Bundle()
                bundle.putString("username", item.username)
                bundle.putString("name", item.name)
                bundle.putString("profile_picture", item.picture)
                bundle.putString("friend_status", item.friend_status)

                findNavController().navigate(R.id.action_navigation_community_to_friendFragment, bundle)
            }
        }

        holder.itemView.apply {
            setOnLongClickListener {
                vibrateOnClick(context)
                val updatedPinnedFriends = pinnedFriendAdapterListener.getPinnedFriends()
                if(updatedPinnedFriends.contains(item.username)){
                    if(pinnedFriendAdapterListener.unPinFriend(item.username)){
                        holder.pin.visibility = View.GONE
                        notifyItemMoved(holder.adapterPosition, pinnedFriendAdapterListener.getPinnedFriends().size)
                        Timber.d("Pinned Friends: ${pinnedFriendAdapterListener.getPinnedFriends()}")
                    }
                }else{
                    if(pinnedFriendAdapterListener.pinFriend(item.username)){
                        holder.pin.visibility = View.VISIBLE
                        notifyItemMoved(holder.adapterPosition, updatedPinnedFriends.size )
                        Timber.d("Pinned Friends: ${pinnedFriendAdapterListener.getPinnedFriends()}")
                    }

                }
                true
            }
        }


    }

    override fun getItemCount() = dataSet.size

    private fun pinFriendsOnTop(dataSet: List<UserResponse>) : List<UserResponse> {
        val pinnedFriends = pinnedFriendAdapterListener.getPinnedFriends()
        val pinnedFriendsList = mutableListOf<UserResponse>()
        val otherFriendsList = mutableListOf<UserResponse>()
        for(i in pinnedFriends){
            for(j in dataSet){
                if(i == j.username){
                    pinnedFriendsList.add(j)
                    continue
                }
            }
        }
        for (i in dataSet){
            if(!pinnedFriends.contains(i.username)){
                pinnedFriendsList.add(i)
            }
        }

        return pinnedFriendsList + otherFriendsList
    }
}



interface PinnedFriendAdapterListener {
    fun pinFriend(username: String): Boolean
    
    fun unPinFriend(username: String): Boolean

    fun getPinnedFriends(): List<String>
}
