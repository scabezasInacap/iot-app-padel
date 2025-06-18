package com.example.paddel

import androidx.compose.ui.Alignment
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.paddel.ui.theme.PaddelTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaddelTheme(darkTheme = false){
                LoginScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var rut by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }


    val scale by animateFloatAsState(if (cargando) 0.95f else 1f)
    val rotate by animateFloatAsState(if (cargando) 10f else 0f)

    // Degradado de fondo
    val backgroundColors = listOf(
        Color(0xFF0D47A1), // Azul oscuro
        Color(0xFF1B5E20)  // Verde oscuro
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo degradado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(backgroundColors))
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(24.dp)
                .padding(top = 100.dp)
        ) {
            Text(
                text = "Iniciar Sesión",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Campo de RUT
            TextField(
                value = rut,
                onValueChange = { nuevoRut ->
                    rut = nuevoRut.filter { it.isDigit() || it == '.' || it == '-' || it == 'K' || it == 'k' }
                    if (rut.length > 12) rut = rut.take(12)
                },
                label = { Text("RUT (ej: 123456789-1)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Contraseña
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = Color.Gray
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (rut.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Ingrese RUT y contraseña", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val rutLimpiado = cleaneRUT(rut)
                    val esValido = validareRUT(rutLimpiado)
                    if (!esValido) {
                        Toast.makeText(context, "El RUT no es válido", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    cargando = true
                    db.collection("usuarios")
                        .whereEqualTo("rut", rutLimpiado)
                        .get()
                        .addOnSuccessListener { documents ->
                            cargando = false
                            if (!documents.isEmpty) {
                                val userDoc = documents.documents[0]
                                val email = userDoc.getString("email") ?: ""
                                if (email.isNotEmpty()) {
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(context, "Inicio exitoso", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(context, MainMenuActivity::class.java)
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Sin correo asociado al RUT", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "RUT no registrado", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            cargando = false
                            Toast.makeText(context, "Error al buscar RUT", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .scale(scale)
                    .rotate(rotate),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D47A1),
                    contentColor = Color.White
                ),
                enabled = !cargando
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Iniciar Sesión")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = {
                    val intent = Intent(context, RegistroActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Text("Registrarse")
                }
                TextButton(onClick = {
                    val intent = Intent(context, RecoPasswActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Text("Recuperar Contraseña")
                }
            }
        }
    }
}

fun validareRUT(rut: String): Boolean {
    val cleanRut = rut.filter { it.isDigit() || it == 'K' || it == 'k' }
    if (cleanRut.length < 8) return false
    val body = cleanRut.dropLast(1)
    val dvInput = cleanRut.last().toString().uppercase()
    var suma = 0
    var factor = 2
    for (i in body.length - 1 downTo 0) {
        suma += body[i].digitToInt() * factor
        factor = if (factor == 7) 2 else factor + 1
    }
    val dvEsperado = 11 - (suma % 11)
    val dvFormateado = when (dvEsperado) {
        11 -> "0"
        10 -> "K"
        else -> dvEsperado.toString()
    }
    return dvInput == dvFormateado
}

fun cleaneRUT(rut: String): String {
    return rut
        .filter { it.isDigit() || it == 'K' || it == 'k' }
        .replace("k", "K")
}