package com.example.paddel

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
import androidx.compose.foundation.border
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

    // Estados de datos originales y editables
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

    // Cargar selects
    LaunchedEffect(editDatos["pais"]) {
        if (editDatos["pais"]?.isNotEmpty() == true) {
            loadingRegion = true
            firestore.collection("countries").get().addOnSuccessListener { snap ->
                countryList = snap.documents.mapNotNull { it.getString("name") }
                loadingRegion = false
            }
        }
    }
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

    val scrollState = rememberScrollState()

    // Fondo y tarjeta central
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF43EA7B), Color(0xFF1B8D4A), Color(0xFF0D47A1)),
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
                .background(Color.White.copy(alpha = 0.92f))
                .padding(24.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Mi Perfil",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF0D47A1),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))

            // Mostrar datos como lista o campos editables
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
                val borderColor = if (isModified) Color(0xFF1976D2) else Color.Transparent

                if (!editMode) {
                    // Vista solo lectura
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(label, fontWeight = FontWeight.Bold, color = Color(0xFF1B8D4A))
                        Text(datos[key] ?: "", color = Color(0xFF357ABD), fontSize = 16.sp)
                        Divider(Modifier.padding(top = 6.dp), color = Color(0xFFE0E0E0))
                    }
                } else {
                    // Vista editable
                    Column(Modifier.padding(vertical = 6.dp)) {
                        when (key) {
                            "pais" -> CountrySelectorPerfil(
                                items = countryList,
                                selectedItem = editDatos[key] ?: "",
                                onItemSelected = {
                                    editDatos = editDatos.toMutableMap().apply { put("pais", it); put("region", ""); put("comuna", "") }
                                    modifiedFields = updateModifiedFields(datos, editDatos)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            )
                            "region" -> RegionSelectorPerfil(
                                items = regionList,
                                selectedItem = editDatos[key] ?: "",
                                onItemSelected = {
                                    editDatos = editDatos.toMutableMap().apply { put("region", it); put("comuna", "") }
                                    modifiedFields = updateModifiedFields(datos, editDatos)
                                },
                                enabled = (editDatos["pais"] ?: "").isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            )
                            "comuna" -> CommuneSelectorPerfil(
                                items = communeList,
                                selectedItem = editDatos[key] ?: "",
                                onItemSelected = {
                                    editDatos = editDatos.toMutableMap().apply { put("comuna", it) }
                                    modifiedFields = updateModifiedFields(datos, editDatos)
                                },
                                enabled = (editDatos["region"] ?: "").isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            )
                            "genero" -> {
                                Text("Género", fontWeight = FontWeight.Bold, color = Color(0xFF1B8D4A))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    GenderOptionPerfil(
                                        icon = Icons.Default.Male,
                                        label = "Masculino",
                                        selected = editDatos[key] == "Masculino",
                                        color = Color(0xFF357ABD)
                                    ) {
                                        editDatos = editDatos.toMutableMap().apply { put("genero", "Masculino") }
                                        modifiedFields = updateModifiedFields(datos, editDatos)
                                    }
                                    GenderOptionPerfil(
                                        icon = Icons.Default.Female,
                                        label = "Femenino",
                                        selected = editDatos[key] == "Femenino",
                                        color = Color(0xFF9C27B0)
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D4A))
                                ) {
                                    Text(if (fecha.isBlank()) "Seleccionar fecha" else fecha)
                                }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            )
                        }
                        if (isModified) {
                            Text("Modificado", color = Color(0xFF1976D2), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Botones de acción
            if (!editMode) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { editMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) { Text("Editar perfil", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            (context as? android.app.Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) { Text("Cerrar sesión", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { /* TODO: delegar cuenta */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                    ) { Text("Delegar cuenta", fontWeight = FontWeight.Bold) }
                }
            } else {
                // Botón de guardar con animación de mantener presionado
                var isPressed by remember { mutableStateOf(false) }
                val progress by animateFloatAsState(if (isPressed) 1f else 0f, animationSpec = tween(900))
                val buttonColor = lerp(Color(0xFF1976D2), Color(0xFF43A047), progress)
                val textButton = if (isPressed) "Mantén presionado para guardar" else "Guardar cambios"
                val isDisabled = modifiedFields.isEmpty() || editDatos.values.any { it.isBlank() }

                Box(
                    Modifier
                        .fillMaxWidth()
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
                                .background(Color(0xFF43A047), RoundedCornerShape(16.dp))
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
                    onClick = { editMode = false; editDatos = datos; modifiedFields = emptySet() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar edición", fontWeight = FontWeight.Bold) }
            }

            // Botón eliminar cuenta debajo de fecha
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { /* TODO: eliminar cuenta */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Eliminar cuenta", fontWeight = FontWeight.Bold) }
        }
    }
}

// --- COMPONENTES REUTILIZADOS Y ADAPTADOS ---

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
        shape = RoundedCornerShape(12.dp)
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