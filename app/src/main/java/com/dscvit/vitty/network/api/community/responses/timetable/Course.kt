package com.dscvit.vitty.network.api.community.responses.timetable

data class Course(
    val code: String,
    val end_time: String,
    val name: String,
    val slot: String,
    val start_time: String,
    val type: String,
    val venue: String
)