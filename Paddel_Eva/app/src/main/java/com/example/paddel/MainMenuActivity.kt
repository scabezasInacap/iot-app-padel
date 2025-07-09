package com.example.paddel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paddel.ui.theme.PaddelTheme
import androidx.compose.ui.geometry.Offset

class MainMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = prefs.getString("nombre", "Usuario") ?: "Usuario"
        setContent {
            PaddelTheme {
                MainMenuScreen(userName)
            }
        }
    }
}

@Composable
fun MainMenuScreen(userName: String) {
    val context = LocalContext.current

    val backgroundColors = listOf(
        Color(0xFF43EA7B),
        Color(0xFF1B8D4A),
        Color(0xFF0D47A1)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = backgroundColors,
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 2000f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.85f))
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally // Centra todo el contenido
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(context, PerfilActivity::class.java)
                            context.startActivity(intent)
                        }
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = Color(0xFF1B8D4A),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Perfil",
                        color = Color(0xFF1B8D4A),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "¡Bienvenido!",
                        color = Color(0xFF0D47A1),
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = userName,
                        color = Color(0xFF1B5E20),
                        fontSize = 17.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            Button(
                onClick = { /* No hace nada */ },
                modifier = Modifier
                    .padding(top = 32.dp)
            ) {
                Text("Botón Pruebas")
            }
        }
    }
}