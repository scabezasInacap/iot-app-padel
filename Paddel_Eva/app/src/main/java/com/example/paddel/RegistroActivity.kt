package com.example.paddel

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paddel.ui.theme.PaddelTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

data class Commune(val id: String, val name: String)

class RegistroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaddelTheme(darkTheme = false) {
                RegisterFinalScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterFinalScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    var nombre by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthdayFormatted by remember { mutableStateOf("") }
    var selectedCountryName by remember { mutableStateOf("") }
    var selectedRegionName by remember { mutableStateOf("") }
    var selectedCommune by remember { mutableStateOf<Commune?>(null) }
    var countryList by remember { mutableStateOf(listOf<String>()) }
    var regionList by remember { mutableStateOf(listOf<String>()) }
    var communeList by remember { mutableStateOf(listOf<Commune>()) }
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasNumber = password.any { it.isDigit() }
    val specialChars = "!@#\$%^&*()-_=+[]{};':\",.<>/?"
    val hasSpecial = password.any { specialChars.contains(it) }
    val hasLength = password.length >= 8
    val passwordsMatch = password == confirmPassword
    val requisitosCumplidos = hasUpperCase && hasNumber && hasSpecial && hasLength
    var shakeFields by remember { mutableStateOf(setOf<String>()) }
    var missingFieldsMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val calendar = Calendar.getInstance()
    val maxYear = calendar.get(Calendar.YEAR)
    val maxMonth = calendar.get(Calendar.MONTH)
    val maxDay = calendar.get(Calendar.DAY_OF_MONTH)
    var day by remember { mutableStateOf(maxDay) }
    var month by remember { mutableStateOf(maxMonth) }
    var year by remember { mutableStateOf(maxYear) }

    val dateText = if (birthdayFormatted.isNotBlank()) {
        birthdayFormatted
    } else {
        "Seleccionar Fecha"
    }

    // Degradado de fondo
    val backgroundColors = listOf(
        Color(0xFFFFA726), // Naranja claro
        Color(0xFFFDD835), // Amarillo pastel
        Color(0xFFE57373)  // Rojo suave
    )

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(backgroundColors))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .border(1.dp, Color.LightGray, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Si eres nuevo en nuestra aplicación, antes de continuar necesitaremos tus datos para que formes parte de nuestra comunidad.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Nombre
                val shakeNombre by animateFloatAsState(
                    targetValue = if ("nombre" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeNombre },
                    shape = RoundedCornerShape(12.dp),
                    isError = "nombre" in shakeFields
                )

                // RUT
                val shakeRut by animateFloatAsState(
                    targetValue = if ("rut" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                OutlinedTextField(
                    value = rut,
                    onValueChange = { newText ->
                        rut = newText.filter { it.isDigit() || it == '.' || it == '-' || it == 'K' || it == 'k' }
                        if (rut.length > 12) rut = rut.take(12)
                    },
                    label = { Text("RUT (ej: 123456789-1)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeRut },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(12.dp),
                    isError = "rut" in shakeFields
                )

                val shakeTelefono by animateFloatAsState(
                    targetValue = if ("numero" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it.filter { it.isDigit() } },
                    label = { Text("Número telefónico") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeTelefono },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    isError = "numero" in shakeFields
                )

                val shakeDireccion by animateFloatAsState(
                    targetValue = if ("direccion" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeDireccion },
                    shape = RoundedCornerShape(12.dp),
                    isError = "direccion" in shakeFields
                )

                val shakeCorreo by animateFloatAsState(
                    targetValue = if ("email" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeCorreo },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    isError = "email" in shakeFields
                )

                val shakePais by animateFloatAsState(
                    targetValue = if ("pais" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                Text(text = "País", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                CountrySelector(
                    items = countryList,
                    selectedItem = selectedCountryName,
                    onItemSelected = { selectedCountryName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakePais }
                )
                Spacer(modifier = Modifier.height(16.dp))

                val shakeRegion by animateFloatAsState(
                    targetValue = if ("region" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                Text(text = "Región", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                RegionSelector(
                    items = regionList,
                    selectedItem = selectedRegionName,
                    onItemSelected = { selectedRegionName = it },
                    enabled = selectedCountryName.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeRegion }
                )
                Spacer(modifier = Modifier.height(16.dp))

                val shakeComuna by animateFloatAsState(
                    targetValue = if ("comuna" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                Text(text = "Comuna", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                CommuneSelector(
                    items = communeList.map { it.name },
                    selectedItem = selectedCommune?.name ?: "",
                    onItemSelected = { name ->
                        selectedCommune = communeList.find { it.name == name }
                    },
                    enabled = selectedRegionName.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeComuna }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Género", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GenderOption(icon = Icons.Default.Male, label = "Masculino", selected = gender == "Masculino") {
                        gender = "Masculino"
                    }
                    GenderOption(
                        icon = Icons.Default.Female,
                        label = "Femenino",
                        selected = gender == "Femenino",
                        containerColor = if (gender == "Femenino") Color(0xFFAB47BC).copy(alpha = 0.9f) else Color.LightGray.copy(alpha = 0.8f)
                    ) {
                        gender = "Femenino"
                    }
                }
                Text(text = "Fecha De Nacimiento", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                val shakeFecha by animateFloatAsState(
                    targetValue = if ("fecha" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )

                    Button(
                        onClick = {
                            DatePickerDialog(
                                context as android.app.Activity,
                                { _, yearD, monthD, dayD ->
                                    if (yearD > maxYear || (yearD == maxYear && monthD > maxMonth) ||
                                        (yearD == maxYear && monthD == maxMonth && dayD > maxDay)
                                    ) {
                                        Toast.makeText(
                                            context,
                                            "No puedes seleccionar una fecha futura.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        day = dayD
                                        month = monthD
                                        year = yearD
                                        birthdayFormatted = "$day/${month + 1}/$year"
                                    }
                                },
                                maxYear,
                                maxMonth,
                                maxDay
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationX = shakeFecha }
                    ) {
                        Text(text = dateText)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                var passwordVisible by remember { mutableStateOf(false) }
                val shakePassword by animateFloatAsState(
                    targetValue = if ("password" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakePassword },
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
                    isError = "password" in shakeFields
                )
                RequisitoItem("Al menos 8 caracteres", hasLength, shakeFields.contains("password"))
                RequisitoItem("Una letra mayúscula", hasUpperCase, shakeFields.contains("password"))
                RequisitoItem("Un número", hasNumber, shakeFields.contains("password"))
                RequisitoItem("Un carácter especial", hasSpecial, shakeFields.contains("password"))

                val shakeConfirm by animateFloatAsState(
                    targetValue = if ("confirmPassword" in shakeFields) 10f else 0f,
                    animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Contraseña") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeConfirm },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    isError = "confirmPassword" in shakeFields
                )

                // Mensaje de error
                if (missingFieldsMessage.isNotBlank()) {
                    Text(
                        text = missingFieldsMessage,
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de registro con efecto
                val scale by animateFloatAsState(if (loading) 0.95f else 1f)
                val rotate by animateFloatAsState(if (loading) 10f else 0f)

                Button(
                    onClick = {
                        val missing = mutableSetOf<String>()
                        missingFieldsMessage = ""

                        if (nombre.isBlank()) missing.add("nombre")
                        if (rut.isBlank()) missing.add("rut")
                        if (numero.isBlank()) missing.add("numero")
                        if (direccion.isBlank()) missing.add("direccion")
                        if (email.isBlank()) missing.add("email")
                        if (selectedCountryName.isBlank()) missing.add("pais")
                        if (selectedRegionName.isBlank()) missing.add("region")
                        if (selectedCommune == null || selectedCommune!!.name.isBlank()) missing.add("comuna")
                        if (gender.isBlank()) missing.add("género")
                        if (birthdayFormatted.isBlank()) missing.add("fecha")
                        if (password.isBlank()) missing.add("password")
                        if (confirmPassword.isBlank()) missing.add("confirmPassword")

                        if (missing.isNotEmpty()) {
                            shakeFields = missing
                            missingFieldsMessage = "Por favor completa los campos faltantes."
                            coroutineScope.launch {
                                delay(300)
                                shakeFields = emptySet()
                            }
                            return@Button
                        }

                        val isEmailValid = isValidEmail(email)
                        val isRutValid = validarRUT(rut)
                        val isPasswordOk = requisitosCumplidos && passwordsMatch

                        if (!isRutValid) {
                            Toast.makeText(context, "El RUT es inválido.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (!isEmailValid) {
                            Toast.makeText(context, "Correo electrónico inválido.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (!isPasswordOk) {
                            Toast.makeText(context, "La contraseña no cumple los requisitos.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        loading = true
                        firestore.collection("usuarios")
                            .whereEqualTo("rut", cleanRUT(rut))
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty()) {
                                    Toast.makeText(context, "Este RUT ya está registrado.", Toast.LENGTH_SHORT).show()
                                    loading = false
                                    return@addOnSuccessListener
                                }

                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            val userData = hashMapOf(
                                                "nombre" to nombre,
                                                "rut" to cleanRUT(rut),
                                                "numero" to numero,
                                                "direccion" to direccion,
                                                "email" to email,
                                                "genero" to gender,
                                                "fecha_nacimiento" to birthdayFormatted,
                                                "ubicacion" to mapOf(
                                                    "pais" to selectedCountryName,
                                                    "region" to selectedRegionName,
                                                    "comuna" to (selectedCommune?.name ?: "")
                                                ),
                                                "code_recovery" to "",
                                                "state_recovery" to false
                                            )



                                            user?.uid?.let { uid ->
                                                firestore.collection("usuarios").document(uid)
                                                    .set(userData)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(context, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                                                        context.startActivity(Intent(context, LoginActivity::class.java))
                                                        (context as? android.app.Activity)?.finish()
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(context, "Error al guardar datos", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        } else {
                                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                        loading = false
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al validar RUT", Toast.LENGTH_SHORT).show()
                                loading = false
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .rotate(rotate),
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
                    Text("Registrarse", color = Color.White)
                }

                TextButton(
                    onClick = {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("¿Ya tienes cuenta? Inicia sesión")
                }
            }
        }
    }

    // --- Carga de datos desde Firestore ---

    LaunchedEffect(Unit) {
        firestore.collection("countries")
            .get()
            .addOnSuccessListener { snapshot ->
                countryList = snapshot.documents.mapNotNull { it.getString("name") }
            }
    }

    LaunchedEffect(selectedCountryName) {
        if (selectedCountryName.isNotEmpty()) {
            firestore.collection("countries")
                .whereEqualTo("name", selectedCountryName)
                .limit(1)
                .get()
                .addOnSuccessListener { countries ->
                    if (countries.isEmpty()) return@addOnSuccessListener
                    val countryId = countries.first().id
                    firestore.collection("countries/$countryId/regions")
                        .get()
                        .addOnSuccessListener { regions ->
                            regionList = regions.documents.mapNotNull { it.getString("name") }
                        }
                }
        }
    }

    LaunchedEffect(selectedRegionName) {
        if (selectedCountryName.isNotEmpty() && selectedRegionName.isNotEmpty()) {
            firestore.collection("countries")
                .whereEqualTo("name", selectedCountryName)
                .limit(1)
                .get()
                .addOnSuccessListener { countries ->
                    if (countries.isEmpty()) return@addOnSuccessListener
                    val countryId = countries.first().id
                    firestore.collection("countries/$countryId/regions")
                        .whereEqualTo("name", selectedRegionName)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { regions ->
                            if (regions.isEmpty()) return@addOnSuccessListener
                            val regionId = regions.first().id
                            firestore.collection("countries/$countryId/regions/$regionId/communes")
                                .get()
                                .addOnSuccessListener { communes ->
                                    communeList = communes.documents.mapNotNull { doc ->
                                        val name = doc.getString("name") ?: return@mapNotNull null
                                        Commune(doc.id, name)
                                    }
                                }
                        }
                }
        }
    }
}

// --- SELECTORS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelector(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = if (selectedItem.isEmpty()) "Selecciona tu país" else selectedItem,
            onValueChange = {},
            label = { Text("País") },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelector(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = if (selectedItem.isEmpty()) "Selecciona tu región" else selectedItem,
            onValueChange = {},
            label = { Text("Región") },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuneSelector(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = if (selectedItem.isEmpty()) "Selecciona tu comuna" else selectedItem,
            onValueChange = {},
            label = { Text("Comuna") },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- FUNCIONES AUXILIARES ---

fun validarRUT(rut: String): Boolean {
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

fun cleanRUT(rut: String): String {
    return rut.filter { it.isDigit() || it == 'K' || it == 'k' }
        .replace("K", "k")
        .replace("k", "K")
}

fun isValidEmail(email: String): Boolean {
    val pattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\$")
    return pattern.matches(email)
}

@Composable
fun RequisitoItem(text: String, cumplido: Boolean, shake: Boolean) {
    val alpha by animateFloatAsState(if (shake) 0.9f else 1f)
    val scale by animateFloatAsState(if (shake) 1.05f else 1f)

    val textColor = when {
        cumplido -> Color(0xFF4CAF50)
        shake -> Color.Red
        else -> Color.Gray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    containerColor: Color = if (selected) Color(0xFF357ABD) else Color.LightGray,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.size(90.dp)
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor),
            modifier = Modifier.size(80.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color.White else Color.Black,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}