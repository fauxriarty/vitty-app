package com.dscvit.vitty.network.api.community.requests

data class AuthRequestBody(
    val reg_no: String,
    val username: String,
    val uuid: String
)