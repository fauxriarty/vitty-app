package com.dscvit.vitty.network.api.community.responses.timetable

data class Data(
    val Friday: List<Course>?,
    val Monday: List<Course>?,
    val Thursday: List<Course>?,
    val Tuesday: List<Course>?,
    val Wednesday: List<Course>?
)