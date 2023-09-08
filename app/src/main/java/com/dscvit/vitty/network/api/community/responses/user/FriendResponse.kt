package com.dscvit.vitty.network.api.community.responses.user

data class FriendResponse(
    val `data`: List<UserResponse>?,
    val friend_status: String
)