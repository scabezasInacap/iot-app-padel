package com.example.paddel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.paddel.LoginActivity
import com.example.paddel.ui.theme.MarcadorActivity
import com.example.paddel.ui.theme.HistorialActivity
import com.example.paddel.ui.theme.PaddelTheme

class MainMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaddelTheme {
                MainMenuScreen()
            }
        }
    }
}

@Composable
fun MainMenuScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bienvenido al menú",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        }) {
            Text("Regresar")
        }

        Button(onClick = {
            val intent = Intent(context, MarcadorActivity::class.java)
            context.startActivity(intent)
        }) {
            Text("Partido en vivo")
        }

        Button(onClick = {
            val intent = Intent(context, HistorialActivity::class.java)
            context.startActivity(intent)
        }) {
            Text("Ver Historial")
        }
    }
}
