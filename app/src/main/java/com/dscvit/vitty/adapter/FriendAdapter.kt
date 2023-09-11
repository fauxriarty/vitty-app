package com.dscvit.vitty.adapter

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FriendAdapter(private val dataSet: List<UserResponse>) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>() {


    class ViewHolder(private val binding: CardFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val friend_name = binding.friendName
        val friend_class = binding.friendClass
        val friend_status = binding.friendStatus
        val friend_image = binding.icon

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


    }

    override fun getItemCount() = dataSet.size
}
