package com.example.paddel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.paddel.LoginActivity
import com.example.paddel.RecoPasswActivity

class PerfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PerfilScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PerfilScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    val userNamePrefs = prefs.getString("nombre", "") ?: ""
    var nombre by remember { mutableStateOf(userNamePrefs) }
    var email by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    // Copias para edición
    var editNombre by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editTelefono by remember { mutableStateOf("") }
    var editDireccion by remember { mutableStateOf("") }

    var editMode by remember { mutableStateOf(false) }
    var claveActual by remember { mutableStateOf("") }
    var showWarningDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var deleteStep by remember { mutableStateOf(0) }
    var cargando by remember { mutableStateOf(false) }

    // Feedback visual
    var feedback by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }

    // Cargar datos desde Firestore
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val rutPrefs = prefs.getString("rut", null)
        if (rutPrefs != null) {
            firestore.collection("usuarios")
                .whereEqualTo("rut", rutPrefs)
                .get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val user = docs.documents[0]
                        nombre = user.getString("nombre") ?: ""
                        email = user.getString("email") ?: ""
                        rut = user.getString("rut") ?: ""
                        telefono = user.getString("telefono") ?: ""
                        direccion = user.getString("direccion") ?: ""
                    }
                }
        }
    }

    // Fondo degradado animado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF43EA7B), Color(0xFF1B8D4A), Color(0xFF0D47A1)),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(1000f, 2000f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = showFeedback,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                Surface(
                    color = Color(0xFF1B8D4A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = feedback,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Contenedor de datos personales
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.95f)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (editMode) {
                        OutlinedTextField(
                            value = editNombre,
                            onValueChange = { editNombre = it },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = rut,
                            onValueChange = {},
                            label = { Text("RUT") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editTelefono,
                            onValueChange = { editTelefono = it },
                            label = { Text("Teléfono") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editDireccion,
                            onValueChange = { editDireccion = it },
                            label = { Text("Dirección") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Nombre: $nombre", style = MaterialTheme.typography.titleMedium)
                        Text("Email: $email", style = MaterialTheme.typography.bodyLarge)
                        Text("RUT: $rut", style = MaterialTheme.typography.bodyLarge)
                        Text("Teléfono: $telefono", style = MaterialTheme.typography.bodyLarge)
                        Text("Dirección: $direccion", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = editMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                }
            ) { editing ->
                if (editing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                // Validaciones
                                if (editNombre.isBlank() || editEmail.isBlank() || editTelefono.isBlank() || editDireccion.isBlank()) {
                                    feedback = "Ningún campo puede estar vacío"
                                    showFeedback = true
                                    return@Button
                                }
                                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(editEmail).matches()) {
                                    feedback = "Email inválido"
                                    showFeedback = true
                                    return@Button
                                }
                                if (editTelefono.length < 8) {
                                    feedback = "Teléfono inválido"
                                    showFeedback = true
                                    return@Button
                                }
                                cargando = true
                                val firestore = FirebaseFirestore.getInstance()
                                val rutPrefs = prefs.getString("rut", null)
                                if (rutPrefs != null) {
                                    firestore.collection("usuarios")
                                        .whereEqualTo("rut", rutPrefs)
                                        .get()
                                        .addOnSuccessListener { docs ->
                                            if (!docs.isEmpty) {
                                                val userId = docs.documents[0].id
                                                firestore.collection("usuarios").document(userId)
                                                    .update(
                                                        mapOf(
                                                            "nombre" to editNombre,
                                                            "email" to editEmail,
                                                            "telefono" to editTelefono,
                                                            "direccion" to editDireccion
                                                        )
                                                    )
                                                    .addOnSuccessListener {
                                                        nombre = editNombre
                                                        email = editEmail
                                                        telefono = editTelefono
                                                        direccion = editDireccion
                                                        feedback = "Datos actualizados"
                                                        showFeedback = true
                                                        editMode = false
                                                        cargando = false
                                                        prefs.edit().putString("nombre", editNombre).apply()
                                                    }
                                                    .addOnFailureListener {
                                                        feedback = "Error al actualizar"
                                                        showFeedback = true
                                                        cargando = false
                                                    }
                                            }
                                        }
                                }
                            },
                            enabled = !cargando
                        ) {
                            Text("Guardar")
                        }
                        OutlinedButton(
                            onClick = {
                                editMode = false
                                showFeedback = false
                            }
                        ) {
                            Text("Descartar")
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                // Inicializar campos de edición con los valores actuales
                                editNombre = nombre
                                editEmail = email
                                editTelefono = telefono
                                editDireccion = direccion
                                editMode = true
                                showFeedback = false
                            },
                            modifier = Modifier
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                            Spacer(Modifier.width(8.dp))
                            Text("Editar")
                        }
                    }
                }
            }

            // Botón Cambiar contraseña
            AnimatedVisibility(visible = !editMode) {
                Button(
                    onClick = {
                        val intent = Intent(context, RecoPasswActivity::class.java)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Cambiar contraseña")
                    Spacer(Modifier.width(8.dp))
                    Text("Cambiar contraseña")
                }
            }

            // Botón Salir de la cuenta
            AnimatedVisibility(visible = !editMode) {
                Button(
                    onClick = {
                        prefs.edit().clear().apply()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Salir")
                    Spacer(Modifier.width(8.dp))
                    Text("Salir de la cuenta")
                }
            }

            // Botón Eliminar cuenta
            AnimatedVisibility(visible = !editMode) {
                Button(
                    onClick = { showWarningDialog = true; deleteStep = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar cuenta")
                }
            }
        }

        // Diálogo de advertencias para eliminar cuenta
        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = { showWarningDialog = false },
                title = { Text("Advertencia") },
                text = {
                    Text(
                        when (deleteStep) {
                            0 -> "¿Estás seguro que quieres eliminar tu cuenta? Esta acción es irreversible."
                            1 -> "Perderás todos tus datos y no podrás recuperarlos. ¿Continuar?"
                            else -> ""
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deleteStep == 0) {
                            deleteStep = 1
                        } else {
                            showWarningDialog = false
                            showPasswordDialog = true
                        }
                    }) { Text("Continuar") }
                },
                dismissButton = {
                    TextButton(onClick = { showWarningDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Diálogo para confirmar contraseña antes de eliminar
        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text("Confirmar eliminación") },
                text = {
                    Column {
                        Text("Introduce tu contraseña para confirmar la eliminación de la cuenta.")
                        OutlinedTextField(
                            value = claveActual,
                            onValueChange = { claveActual = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val firestore = FirebaseFirestore.getInstance()
                        val rutPrefs = prefs.getString("rut", null)
                        if (rutPrefs != null) {
                            firestore.collection("usuarios")
                                .whereEqualTo("rut", rutPrefs)
                                .get()
                                .addOnSuccessListener { docs ->
                                    if (!docs.isEmpty) {
                                        val user = docs.documents[0]
                                        val hash = user.getString("password_hash") ?: ""
                                        if (org.mindrot.jbcrypt.BCrypt.checkpw(claveActual, hash)) {
                                            firestore.collection("usuarios").document(user.id)
                                                .delete()
                                                .addOnSuccessListener {
                                                    prefs.edit().clear().apply()
                                                    feedback = "Cuenta eliminada"
                                                    showFeedback = true
                                                    val intent = Intent(context, LoginActivity::class.java)
                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    context.startActivity(intent)
                                                }
                                        } else {
                                            feedback = "Contraseña incorrecta"
                                            showFeedback = true
                                        }
                                    }
                                }
                        }
                        showPasswordDialog = false
                    }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}