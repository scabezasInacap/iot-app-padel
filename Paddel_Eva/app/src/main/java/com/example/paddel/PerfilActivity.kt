package com.example.paddel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


class PerfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Pantalla de Perfil")
        }
    }
}

@Composable
fun PerfilScreen(userName: String) {
    Text(text = "Bienvenido, $userName")
}
