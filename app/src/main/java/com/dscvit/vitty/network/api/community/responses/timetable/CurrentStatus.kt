package com.dscvit.vitty.network.api.community.responses.timetable


data class CurrentStatus(
    val status: String,
    val `class`: String?,
    val slot: String?,
    val venue: String?
)