package com.dscvit.vitty.network.api.community.responses.user

import com.dscvit.vitty.network.api.community.responses.timetable.TimetableResponse

data class UserResponse(
    var email: String,
    var friend_status: String,
    var friends_count: Int,
    var mutual_friends_count: Int,
    var name: String,
    var picture: String,
    var timetable: TimetableResponse?,
    var username: String
)
