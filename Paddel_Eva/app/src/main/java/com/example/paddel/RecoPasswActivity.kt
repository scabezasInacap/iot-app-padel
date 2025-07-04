package com.example.paddel

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paddel.ui.theme.PaddelTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import org.mindrot.jbcrypt.BCrypt
import androidx.compose.animation.core.animateFloatAsState
import java.util.*
import androidx.compose.animation.core.*

class RecoPasswActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaddelTheme(darkTheme = false) {
                RecuperarContrasenaScreen()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarContrasenaScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var step by remember { mutableStateOf(0) } // 0: correo, 1: código, 2: nueva contraseña
    var recoveryCode by remember { mutableStateOf("") }
    var recoveryActive by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(120) }

    var showErrorEmail by remember { mutableStateOf(false) }
    var showErrorCode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    val hasUpperCase = password.any { it.isUpperCase() }
    val hasNumber = password.any { it.isDigit() }
    val specialChars = "!@#\$%^&*()-_=+[]{};':\",.<>/?"
    val hasSpecial = password.any { specialChars.contains(it) }
    val hasLength = password.length >= 8
    val passwordsMatch = password == confirmPassword
    val requisitosCumplidos = hasUpperCase && hasNumber && hasSpecial && hasLength

    val coroutineScope = rememberCoroutineScope()

    val backgroundColors = listOf(
        Color(0xFFFFA726), // Naranja claro
        Color(0xFFFDD835), // Amarillo pastel
        Color(0xFFEF9A9A)  // Rojo suave
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
        ) {
            when (step) {
                0 -> {
                    Text("Recuperar Contraseña", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ingresa tu correo electrónico para recibir instrucciones.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { nuevo ->
                            email = nuevo
                            showErrorEmail = false
                        },
                        label = { Text("Correo Electrónico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        isError = showErrorEmail
                    )
                    if (showErrorEmail && email.isNotEmpty()) {
                        Text(
                            text = "Correo no registrado",
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                Toast.makeText(context, "Ingrese su correo", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            loading = true
                            val emailLower = email.lowercase()
                            firestore.collection("usuarios")
                                .whereEqualTo("email", emailLower)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    if (snapshot.isEmpty) {
                                        showErrorEmail = true
                                        loading = false
                                        return@addOnSuccessListener
                                    }

                                    loading = true
                                    val codeGenerated = (1000..9999).random().toString()
                                    recoveryCode = codeGenerated
                                    recoveryActive = true
                                    timeLeft = 120

                                    // Guardar en Firestore
                                    val userData = hashMapOf<String, Any>(
                                        "code_recovery" to codeGenerated,
                                        "state_recovery" to false
                                    )
                                    val userId = snapshot.documents[0].id
                                    firestore.collection("usuarios")
                                        .document(userId)
                                        .update(userData)
                                        .addOnSuccessListener {
                                            sendRecoveryCodeByEmail(context, emailLower, codeGenerated)
                                            Toast.makeText(context, "Código enviado", Toast.LENGTH_SHORT).show()
                                            step = 1
                                            // Iniciar temporizador
                                            coroutineScope.launch {
                                                while (timeLeft > 0 && recoveryActive) {
                                                    delay(1000)
                                                    timeLeft--
                                                }
                                                if (recoveryActive) {
                                                    Toast.makeText(context, "Código expirado", Toast.LENGTH_SHORT).show()
                                                    firestore.collection("usuarios")
                                                        .document(userId)
                                                        .update("code_recovery", "")
                                                    recoveryActive = false
                                                    step = 0
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Error al guardar código", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnCompleteListener {
                                            loading = false
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al verificar correo", Toast.LENGTH_SHORT).show()
                                    loading = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Enviar Código", color = Color.White)
                    }
                }

                1 -> {
                    Text("Verificar Código", style = MaterialTheme.typography.headlineMedium)
                    val formattedTime = "%d:%02d".format(timeLeft / 60, timeLeft % 60)
                    Text(
                        text = "Se ha enviado un código de 6 dígitos a tu correo. Ingresa el código para continuar.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Código válido por: $formattedTime",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { newText ->
                            if (newText.length <= 6 && newText.all { it.isLetterOrDigit() }) {
                                code = newText
                                showErrorCode = false
                            }
                        },
                        label = { Text("Código de Verificación") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (showErrorCode && code.isNotEmpty()) {
                        Text(text = "Código incorrecto", color = Color.Red, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (code != recoveryCode) {
                                showErrorCode = true
                                Toast.makeText(context, "Código incorrecto", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!recoveryActive) {
                                Toast.makeText(context, "El código ha expirado", Toast.LENGTH_SHORT).show()
                                step = 0
                                return@Button
                            }
                            step = 2
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verificar", color = Color.White)
                    }
                }

                2 -> {
                    Text("Nueva Contraseña", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ingresa tu nueva contraseña siguiendo los requisitos.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    RequisitoItems("Al menos 8 caracteres", hasLength, shake = false)
                    RequisitoItems("Una letra mayúscula", hasUpperCase, shake = false)
                    RequisitoItems("Un número", hasNumber, shake = false)
                    RequisitoItems("Un carácter especial", hasSpecial, shake = false)

                    var passwordVisible by remember { mutableStateOf(false) }
                    val visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = visualTransformation,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                        Text(text = "Las contraseñas no coinciden", color = Color.Red, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!requisitosCumplidos || !passwordsMatch) {
                                Toast.makeText(context, "Verifica los requisitos", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            loading = true
                            firestore.collection("usuarios")
                                .whereEqualTo("email", email.lowercase())
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    if (snapshot.isEmpty) {
                                        Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                                        loading = false
                                        return@addOnSuccessListener
                                    }
                                    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                                    val userId = snapshot.documents[0].id
                                    firestore.collection("usuarios")
                                        .document(userId)
                                        .update(
                                            "password_hash", hashedPassword,
                                            "code_recovery", "",
                                            "state_recovery", false
                                        )
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                                            context.startActivity(Intent(context, LoginActivity::class.java))
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Error al actualizar contraseña", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnCompleteListener {
                                            loading = false
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
                                    loading = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Cambiar Contraseña", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RequisitoItems(text: String, cumplido: Boolean, shake: Boolean) {
    val alpha by animateFloatAsState(if (shake) 0.9f else 1f)
    val scale by animateFloatAsState(if (shake) 1.05f else 1f)
    val textColor = when {
        cumplido -> Color(0xFF4CAF50)
        shake -> Color.Red
        else -> Color.Gray
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        Text(
            text = if (cumplido) "✓ $text" else if (shake) "⚠ $text" else "• $text",
            color = textColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

fun sendRecoveryCodeByEmail(context: Context, email: String, code: String) {
    val client = OkHttpClient()

    val jsonBody = JSONObject().apply {
        put("service_id", "service_9aadq5j")
        put("template_id", "template_1v77gyp")
        put("user_id", "Y0PggIiBi8Aiv2s24")

        val templateParams = JSONObject().apply {
            put("to_email", email)
            put("passcode", code)
            put("name", "Usuario"
            )
            put("time", getCurrentTimePlusMinutes(2))
        }

        put("template_params", templateParams)
    }

    val mediaType = "application/json".toMediaTypeOrNull()
    val body = RequestBody.create(mediaType, jsonBody.toString())

    val request = Request.Builder()
        .url("https://api.emailjs.com/api/v1.0/email/send")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "No se pudo enviar el código", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error al enviar correo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}

fun getCurrentTimePlusMinutes(minutes: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MINUTE, minutes)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    return "$hour:${String.format("%02d", minute)}"
}