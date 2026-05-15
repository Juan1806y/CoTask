package com.uni.colabtasks.domain.model

sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
}
