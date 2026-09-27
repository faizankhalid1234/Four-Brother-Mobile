package com.fine.trade

data class Protector(
    val id: String,
    val protectorName: String,
    val mobileModels: List<String>
)

data class MobileModel(
    val id: String,
    val name: String
)
