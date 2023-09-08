package com.dscvit.vitty.network.api.community.responses.user

data class SignInResponse(
    val name: String,
    val picture: String,
    val role: String,
    val token: String,
    val username: String
)