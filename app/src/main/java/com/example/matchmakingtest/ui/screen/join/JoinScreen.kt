package com.example.matchmakingtest.ui.screen.join

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun JoinScreen (
    navController: NavController,
    viewModel: JoinViewModel = hiltViewModel()
){
    Column {

        Button(onClick = { viewModel.handleClick() }) {
            Text(text = "click me")
        }

    }
}