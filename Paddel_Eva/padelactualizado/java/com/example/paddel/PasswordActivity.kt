/*
package com.example.paddel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paddel.ui.theme.PaddelTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaddelTheme {
                PasswordScreen()
            }
        }
    }
}

@Composable
fun PasswordScreen() {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var passwordRepeat by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var shakeTrigger by remember { mutableStateOf(false) }
    var shakeMismatch by remember { mutableStateOf(false) }

    val hasUpperCase = password.any { it.isUpperCase() }
    val hasNumber = password.any { it.isDigit() }
    val hasSpecial = password.any { "!@#$%^&*()_+-=[]{};':\",.<>/?".contains(it) }
    val hasLength = password.length >= 8

    val requisitosCumplidos = hasUpperCase && hasNumber && hasSpecial && hasLength
    val contraseñasIguales = password == passwordRepeat

    val shakeOffsetGeneral by animateFloatAsState(
        targetValue = if (shakeTrigger) 10f else 0f,
        animationSpec = repeatable(3, tween(50, easing = LinearEasing), RepeatMode.Reverse),
        label = "ShakeGeneral"
    )

    val shakeOffsetMismatch by animateFloatAsState(
        targetValue = if (shakeMismatch) 10f else 0f,
        animationSpec = repeatable(3, tween(50, easing = LinearEasing), RepeatMode.Reverse),
        label = "ShakeMismatch"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF4A90E2))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF7ED321), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("Logo", color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Contraseña", color = Color.White, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Para la creación de su contraseña, debe constar de al menos 8 caracteres, con una letra mayúscula, número y al menos un carácter especial",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = shakeOffsetGeneral
            }) {
            RequisitoItem("Al menos 8 caracteres", hasLength, shakeTrigger)
            RequisitoItem("Una letra mayúscula", hasUpperCase, shakeTrigger)
            RequisitoItem("Un número", hasNumber, shakeTrigger)
            RequisitoItem("Un carácter especial", hasSpecial, shakeTrigger)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Campo contraseña
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (!contraseñasIguales) translationX = shakeOffsetMismatch
                },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (!contraseñasIguales && password.isNotEmpty()) Color.Red else Color(0xFF4A90E2)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordRepeat,
            onValueChange = { passwordRepeat = it },
            label = { Text("Repita Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (!contraseñasIguales) translationX = shakeOffsetMismatch
                },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (!contraseñasIguales && passwordRepeat.isNotEmpty()) Color.Red else Color(0xFF4A90E2)
            )
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = Color.Red, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botones
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    context.startActivity(Intent(context, ConfirmationActivity::class.java))
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = 0.15f), size = size.copy(height = 6.dp.toPx()))
                    },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF357ABD))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Regresar", color = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (!requisitosCumplidos || !contraseñasIguales) {
                        errorMessage = when {
                            !requisitosCumplidos -> "La contraseña no cumple con todos los requisitos."
                            !contraseñasIguales -> "Las contraseñas no coinciden."
                            else -> ""
                        }
                        shakeTrigger = true
                        shakeMismatch = !contraseñasIguales
                        coroutineScope.launch {
                            delay(300)
                            shakeTrigger = false
                            shakeMismatch = false
                        }
                    } else {
                        errorMessage = ""
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = 0.15f), size = size.copy(height = 6.dp.toPx()))
                    },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF357ABD))
            ) {
                Text("Definir contraseña", color = Color.White)
            }
        }
    }
}

@Composable
fun RequisitoItem(text: String, cumplido: Boolean, shake: Boolean) {
    val color = when {
        cumplido -> Color(0xFF4CAF50)
        shake -> Color.Red
        else -> Color.Gray
    }
    Text(
        text = "• $text",
        color = color,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}


 */