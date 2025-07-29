package com.example.bluelink.models

data class BleTestCasesModel(
    val id: String,
    val title: String,
    val description: String,
    var isSelected: Boolean = false
)