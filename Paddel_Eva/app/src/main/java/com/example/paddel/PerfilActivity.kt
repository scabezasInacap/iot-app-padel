
package com.example.paddel

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class PerfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PerfilScreenV3()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PerfilScreenV3() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    val rutPrefs = prefs.getString("rut", null) ?: ""
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    // Estados de carga y éxito
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var datos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editDatos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editMode by remember { mutableStateOf(false) }
    var modifiedFields by remember { mutableStateOf(setOf<String>()) }
    // Estados para selects
    var countryList by remember { mutableStateOf(listOf<String>()) }
    var regionList by remember { mutableStateOf(listOf<String>()) }
    var communeList by remember { mutableStateOf(listOf<String>()) }
    var loadingCountry by remember { mutableStateOf(false) }
    var loadingRegion by remember { mutableStateOf(false) }
    var loadingCommune by remember { mutableStateOf(false) }

    // Cargar datos usuario
    LaunchedEffect(Unit) {
        loading = true
        firestore.collection("usuarios")
            .whereEqualTo("rut", rutPrefs)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc != null) {
                    val map = mapOf(
                        "nombre" to (doc.getString("nombre") ?: ""),
                        "rut" to (doc.getString("rut") ?: ""),
                        "email" to (doc.getString("email") ?: ""),
                        "pais" to (doc.getString("pais") ?: ""),
                        "region" to (doc.getString("region") ?: ""),
                        "comuna" to (doc.getString("comuna") ?: ""),
                        "direccion" to (doc.getString("direccion") ?: ""),
                        "numero" to (doc.getString("numero") ?: ""),
                        "genero" to (doc.getString("genero") ?: ""),
                        "fecha_nacimiento" to (doc.getString("fecha_nacimiento") ?: "")
                    )
                    datos = map
                    editDatos = map
                }
                loading = false
            }
    }

    // Cargar países
    LaunchedEffect(editDatos["pais"]) {
        if (editDatos["pais"]?.isNotEmpty() == true) {
            loadingRegion = true
            firestore.collection("countries").get().addOnSuccessListener { snap ->
                countryList = snap.documents.mapNotNull { it.getString("name") }
                loadingRegion = false
            }
        }
    }

    // Cargar regiones según país
    LaunchedEffect(editDatos["pais"]) {
        if (editDatos["pais"]?.isNotEmpty() == true) {
            loadingRegion = true
            firestore.collection("countries")
                .whereEqualTo("name", editDatos["pais"])
                .limit(1)
                .get()
                .addOnSuccessListener { countries ->
                    if (countries.isEmpty) {
                        loadingRegion = false
                        return@addOnSuccessListener
                    }
                    val countryId = countries.first().id
                    firestore.collection("countries/$countryId/regions")
                        .get()
                        .addOnSuccessListener { regions ->
                            regionList = regions.documents.mapNotNull { it.getString("name") }
                            loadingRegion = false
                        }
                }
        }
    }

    // Cargar comunas según región y país
    LaunchedEffect(editDatos["region"], editDatos["pais"]) {
        if (editDatos["pais"]?.isNotEmpty() == true && editDatos["region"]?.isNotEmpty() == true) {
            loadingCommune = true
            firestore.collection("countries")
                .whereEqualTo("name", editDatos["pais"])
                .limit(1)
                .get()
                .addOnSuccessListener { countries ->
                    if (countries.isEmpty) {
                        loadingCommune = false
                        return@addOnSuccessListener
                    }
                    val countryId = countries.first().id
                    firestore.collection("countries/$countryId/regions")
                        .whereEqualTo("name", editDatos["region"])
                        .limit(1)
                        .get()
                        .addOnSuccessListener { regions ->
                            if (regions.isEmpty) {
                                loadingCommune = false
                                return@addOnSuccessListener
                            }
                            val regionId = regions.first().id
                            firestore.collection("countries/$countryId/regions/$regionId/communes")
                                .get()
                                .addOnSuccessListener { communes ->
                                    communeList = communes.documents.mapNotNull { it.getString("name") }
                                    loadingCommune = false
                                }
                        }
                }
        }
    }

    // Pantalla de carga
    if (loading || saving) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF43EA7B), Color(0xFF1B8D4A), Color(0xFF0D47A1)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 2000f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f))
            ) {
                Column(
                    Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF1B8D4A))
                    Spacer(Modifier.height(16.dp))
                    Text("Cargando...", color = Color(0xFF1B8D4A), fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Pantalla de éxito
    if (showSuccess) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF43EA7B), Color(0xFF1B8D4A), Color(0xFF0D47A1)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 2000f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.8f),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
                ) {
                    Column(
                        Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF43A047),
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "¡Tus cambios fueron guardados!",
                            color = Color(0xFF1B8D4A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Tu perfil se actualizó correctamente.",
                            color = Color(0xFF0D47A1),
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = {
                                showSuccess = false
                                editMode = false
                                modifiedFields = emptySet()
                                datos = editDatos
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D4A))
                        ) { Text("Volver al perfil", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        return
    }

    if (showDeleteConfirmation) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFEF5350), Color(0xFFC62828), Color(0xFFB71C1C)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 2000f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showDeleteConfirmation,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.8f),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
                ) {
                    Column(
                        Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "¿Estás seguro?",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Esta acción eliminará tu cuenta permanentemente.",
                            color = Color(0xFF424242),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        var isPressedDelete by remember { mutableStateOf(false) }
                        val progress by animateFloatAsState(if (isPressedDelete) 1f else 0f, animationSpec = tween(900))
                        val buttonColor = lerp(Color(0xFFE53935), Color(0xFFEF5350), progress)
                        val textButton = if (isPressedDelete) "Mantén presionado para eliminar" else "Eliminar cuenta"

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(buttonColor)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressedDelete = true
                                            val job = scope.launch {
                                                delay(900)
                                                if (isPressedDelete) {
                                                    saving = true
                                                    firestore.collection("usuarios")
                                                        .whereEqualTo("rut", rutPrefs)
                                                        .get()
                                                        .addOnSuccessListener { snapshot ->
                                                            snapshot.documents.forEach { doc ->
                                                                firestore.collection("usuarios").document(doc.id)
                                                                    .delete()
                                                                    .addOnSuccessListener {
                                                                        // Limpiar caché/sharedPreferences
                                                                        prefs.edit().clear().apply()
                                                                        context.cacheDir.deleteRecursively()

                                                                        // Redirigir a LoginActivity
                                                                        context.startActivity(Intent(context, LoginActivity::class.java))
                                                                        (context as? Activity)?.finishAffinity()
                                                                    }
                                                                    .addOnFailureListener {
                                                                        saving = false
                                                                        Toast.makeText(context, "Error al eliminar la cuenta", Toast.LENGTH_SHORT).show()
                                                                    }
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                            saving = false
                                                            Toast.makeText(context, "No se encontró la cuenta", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                            }
                                            tryAwaitRelease()
                                            isPressedDelete = false
                                            job.cancel()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .height(8.dp)
                                        .fillMaxWidth(progress)
                                        .background(Color(0xFFFFC107), RoundedCornerShape(16.dp))
                                )
                                Text(
                                    textButton,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showDeleteConfirmation = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    val editColors = listOf(Color(0xFFFF7043), Color(0xFFFFC107), Color(0xFFFFEB3B))
    val normalColors = listOf(Color(0xFF43EA7B), Color(0xFF1B8D4A), Color(0xFF0D47A1))
    val fondoActual = if (editMode) editColors else normalColors
    val cardColorActual = if (editMode) Color(0xFFFFF8E1) else Color.White.copy(alpha = 0.92f)
    val tituloColor = if (editMode) Color(0xFFD84315) else Color(0xFF0D47A1)
    val labelColor = if (editMode) Color(0xFFFFA000) else Color(0xFF1B8D4A)
    val datoColor = if (editMode) Color(0xFFD84315) else Color(0xFF357ABD)
    val dividerColor = if (editMode) Color(0xFFFFA726) else Color(0xFFE0E0E0)
    val campoModificadoColor = if (editMode) Color(0xFFFFF3E0) else Color.Transparent
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = fondoActual,
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 2000f)
                )
            )
    ) {
        AnimatedVisibility(
            visible = !editMode,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardColorActual)
                    .padding(24.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Mi Perfil",
                    style = MaterialTheme.typography.headlineMedium,
                    color = tituloColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))

                datos.keys.forEach { key ->
                    val label = when (key) {
                        "nombre" -> "Nombre completo"
                        "rut" -> "RUT"
                        "email" -> "Correo electrónico"
                        "pais" -> "País"
                        "region" -> "Región"
                        "comuna" -> "Comuna"
                        "direccion" -> "Dirección"
                        "numero" -> "Número"
                        "genero" -> "Género"
                        "fecha_nacimiento" -> "Fecha de nacimiento"
                        else -> key
                    }
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(label, fontWeight = FontWeight.Bold, color = labelColor)
                        Text(datos[key] ?: "", color = datoColor, fontSize = 16.sp)
                        Divider(Modifier.padding(top = 6.dp), color = dividerColor)
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Botones en modo vista: verticalmente ordenados, mejor espaciado y visual
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { editMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar datos personales", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            context.startActivity(Intent(context, MainMenuActivity::class.java))
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Regresar al menú principal", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salir de la cuenta", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar cuenta", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = editMode,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardColorActual)
                    .padding(24.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Editar Perfil",
                    style = MaterialTheme.typography.headlineMedium,
                    color = tituloColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))

                // Campos editables
                datos.keys.forEach { key ->
                    val label = when (key) {
                        "nombre" -> "Nombre completo"
                        "rut" -> "RUT"
                        "email" -> "Correo electrónico"
                        "pais" -> "País"
                        "region" -> "Región"
                        "comuna" -> "Comuna"
                        "direccion" -> "Dirección"
                        "numero" -> "Número"
                        "genero" -> "Género"
                        "fecha_nacimiento" -> "Fecha de nacimiento"
                        else -> key
                    }
                    val isModified = key in modifiedFields
                    val campoColor = if (isModified) campoModificadoColor else Color.Transparent
                    Column(
                        Modifier
                            .padding(vertical = 6.dp)
                            .background(campoColor, RoundedCornerShape(12.dp))
                    ) {
                        when (key) {
                            "pais" -> CountrySelectorPerfil(
                                items = countryList,
                                selectedItem = editDatos[key] ?: "",
                                onItemSelected = {
                                    editDatos = editDatos.toMutableMap().apply { put("pais", it); put("region", ""); put("comuna", "") }
                                    modifiedFields = updateModifiedFields(datos, editDatos)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            "region" -> RegionSelectorPerfil(
                                items = regionList,
                                selectedItem = editDatos[key] ?: "",
                                onItemSelected = {
                                    editDatos = editDatos.toMutableMap().apply { put("region", it); put("comuna", "") }
                                    modifiedFields = updateModifiedFields(datos, editDatos)
                                },
                                enabled = (editDatos["pais"] ?: "").isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            "comuna" -> CommuneSelectorPerfil(
                                items = communeList,
                                selectedItem = editDatos[key] ?: "",
                                onItemSelected = {
                                    editDatos = editDatos.toMutableMap().apply { put("comuna", it) }
                                    modifiedFields = updateModifiedFields(datos, editDatos)
                                },
                                enabled = (editDatos["region"] ?: "").isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            "genero" -> {
                                Text("Género", fontWeight = FontWeight.Bold, color = labelColor)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    GenderOptionPerfil(
                                        icon = Icons.Default.Male,
                                        label = "Masculino",
                                        selected = editDatos[key] == "Masculino",
                                        color = Color(0xFF1976D2)
                                    ) {
                                        editDatos = editDatos.toMutableMap().apply { put("genero", "Masculino") }
                                        modifiedFields = updateModifiedFields(datos, editDatos)
                                    }
                                    GenderOptionPerfil(
                                        icon = Icons.Default.Female,
                                        label = "Femenino",
                                        selected = editDatos[key] == "Femenino",
                                        color = Color(0xFFE91E63)
                                    ) {
                                        editDatos = editDatos.toMutableMap().apply { put("genero", "Femenino") }
                                        modifiedFields = updateModifiedFields(datos, editDatos)
                                    }
                                }
                            }
                            "fecha_nacimiento" -> {
                                val fecha = editDatos[key] ?: ""
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        val year = calendar.get(Calendar.YEAR)
                                        val month = calendar.get(Calendar.MONTH)
                                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val nuevaFecha = "%02d/%02d/%04d".format(d, m + 1, y)
                                                editDatos = editDatos.toMutableMap().apply { put("fecha_nacimiento", nuevaFecha) }
                                                modifiedFields = updateModifiedFields(datos, editDatos)
                                            },
                                            year, month, day
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726))
                                ) {
                                    Text(if (fecha.isBlank()) "Seleccionar fecha" else fecha)
                                }
                                // Link para recuperar contraseña justo debajo de fecha de nacimiento
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "¿Olvidaste tu contraseña?",
                                    color = Color(0xFFD84315),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { /* TODO: navegar a recuperar contraseña */ }
                                        .padding(8.dp)
                                )
                            }
                            else -> EditableTextFieldPerfil(
                                label = label,
                                value = editDatos[key] ?: "",
                                onValueChange = {
                                    val capitalized = if (key == "nombre" || key == "direccion") it.split(" ").joinToString(" ") { part -> part.lowercase().replaceFirstChar { c -> c.uppercase() } } else it
                                    editDatos = editDatos.toMutableMap().apply { put(key, capitalized) }
                                    modifiedFields = updateModifiedFields(datos, editDatos)
                                },
                                enabled = key != "rut",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Botones en modo edición: mejor distribución
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { editMode = false; editDatos = datos; modifiedFields = emptySet() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Descartar cambios", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))

                    // Botón guardar cambios con animación
                    var isPressed by remember { mutableStateOf(false) }
                    val progress by animateFloatAsState(if (isPressed) 1f else 0f, animationSpec = tween(900))
                    val buttonColor = lerp(Color(0xFFFF7043), Color(0xFFFFC107), progress)
                    val textButton = if (isPressed) "Mantén presionado para guardar" else "Guardar cambios"
                    val isDisabled = modifiedFields.isEmpty() || editDatos.values.any { it.isBlank() }

                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDisabled) Color(0xFFBDBDBD) else buttonColor)
                            .pointerInput(isDisabled) {
                                if (!isDisabled) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            val job = scope.launch {
                                                delay(900)
                                                if (isPressed && modifiedFields.isNotEmpty() && !editDatos.values.any { it.isBlank() }) {
                                                    saving = true
                                                    val docRef = firestore.collection("usuarios").whereEqualTo("rut", datos["rut"]).limit(1)
                                                    docRef.get().addOnSuccessListener { snap ->
                                                        val doc = snap.documents.firstOrNull()
                                                        if (doc != null) {
                                                            firestore.collection("usuarios").document(doc.id)
                                                                .update(editDatos.filterKeys { it in modifiedFields })
                                                                .addOnSuccessListener {
                                                                    saving = false
                                                                    showSuccess = true
                                                                }
                                                                .addOnFailureListener {
                                                                    saving = false
                                                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                                                }
                                                        }
                                                    }
                                                }
                                            }
                                            tryAwaitRelease()
                                            isPressed = false
                                            job.cancel()
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .height(8.dp)
                                    .fillMaxWidth(progress)
                                    .background(Color(0xFFFFC107), RoundedCornerShape(16.dp))
                            )
                            Text(
                                textButton,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTextFieldPerfil(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
            containerColor = Color.Transparent,
            focusedBorderColor = Color(0xFFFFA000),
            unfocusedBorderColor = Color(0xFFE0E0E0),
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectorPerfil(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            readOnly = true,
            value = if (selectedItem.isEmpty()) "Selecciona tu país" else selectedItem,
            onValueChange = {},
            label = { Text("País") },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = {
                    onItemSelected(item)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectorPerfil(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            readOnly = true,
            value = if (selectedItem.isEmpty()) "Selecciona tu región" else selectedItem,
            onValueChange = {},
            label = { Text("Región") },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            enabled = enabled
        )
        ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = {
                    onItemSelected(item)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuneSelectorPerfil(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            readOnly = true,
            value = if (selectedItem.isEmpty()) "Selecciona tu comuna" else selectedItem,
            onValueChange = {},
            label = { Text("Comuna") },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            enabled = enabled
        )
        ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = {
                    onItemSelected(item)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderOptionPerfil(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.size(90.dp)
    ) {
        Card(
            modifier = Modifier
                .size(60.dp)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                if (selected) color else Color.LightGray
            ),
            shape = RoundedCornerShape(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

fun updateModifiedFields(original: Map<String, String>, edited: Map<String, String>): Set<String> {
    return original.keys.filter { (original[it] ?: "") != (edited[it] ?: "") }.toSet()
}
