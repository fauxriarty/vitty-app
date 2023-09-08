package com.dscvit.vitty.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.adapters.ViewBindingAdapter.setOnLongClickListener
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dscvit.vitty.R
import com.dscvit.vitty.databinding.CardFriendBinding
import com.dscvit.vitty.databinding.CardPeriodBinding
import com.dscvit.vitty.databinding.CardRequestBinding
import com.dscvit.vitty.model.PeriodDetails
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.ui.community.CommunityViewModel
import com.dscvit.vitty.util.Effects.vibrateOnClick
import com.dscvit.vitty.util.RemoteConfigUtils
import com.dscvit.vitty.util.UtilFunctions.copyItem
import com.dscvit.vitty.util.VITMap
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SearchAdapter(dataSet: List<UserResponse>, private val token:String, private val communityViewModel: CommunityViewModel) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    val mutableDataSet = dataSet.toMutableList()

    class ViewHolder(private val binding: CardRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val name = binding.name
        val pendingRequestLayout = binding.acceptRejectLayout
        val sendRequestLayout = binding.sendRequestLayout
        val sentLayout = binding.sentLayout
        val accept = binding.accept
        val reject = binding.reject
        val sendRequest = binding.sendRequest
        val image = binding.icon
        val username = binding.username
        fun bind(data: UserResponse) {
            binding.personDetails = data
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.card_request,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mutableDataSet[holder.adapterPosition]
        holder.bind(item)
        when (item.friend_status) {
            "received" -> {
                holder.pendingRequestLayout.visibility = View.VISIBLE
                holder.sendRequestLayout.visibility = View.GONE
                holder.sentLayout.visibility = View.GONE
            }
            "sent" -> {
                holder.pendingRequestLayout.visibility = View.GONE
                holder.sendRequestLayout.visibility = View.GONE
                holder.sentLayout.visibility = View.VISIBLE
            }
            else -> {
                holder.pendingRequestLayout.visibility = View.GONE
                holder.sendRequestLayout.visibility = View.VISIBLE
                holder.sentLayout.visibility = View.GONE
            }
        }

        holder.image.load(item.picture) {
            crossfade(true)
            placeholder(R.drawable.ic_gdscvit)
            error(R.drawable.ic_gdscvit)
        }


        holder.accept.apply {
            setOnClickListener {
                communityViewModel.acceptRequest(token,item)
                mutableDataSet.removeAt(holder.adapterPosition)
                notifyItemRemoved(holder.adapterPosition)
                notifyItemRangeChanged(holder.adapterPosition, mutableDataSet.size)
            }
        }

        holder.reject.apply {
            setOnClickListener {
                communityViewModel.rejectRequest(token, item)
                mutableDataSet.removeAt(holder.adapterPosition)
                notifyItemRemoved(holder.adapterPosition)
                notifyItemRangeChanged(holder.adapterPosition, mutableDataSet.size)
            }
        }

        holder.sendRequest.apply {
            setOnClickListener {
                holder.sendRequestLayout.visibility = View.GONE
                holder.sentLayout.visibility = View.VISIBLE
                communityViewModel.sendRequest(token,item)
            }
        }


        holder.itemView.apply {
            setOnClickListener {

            }

        }


    }

    override fun getItemCount() = mutableDataSet.size
}
