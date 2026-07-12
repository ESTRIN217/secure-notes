package com.example.ui

class Navigator(
    val onNavigateTo: (Screen) -> Unit,
    val onNavigateBack: (Screen) -> Unit
)
