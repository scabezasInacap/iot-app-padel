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
import androidx.compose.foundation.*
import java.util.*
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer

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
    val firestore = FirebaseFirestore.getInstance()

    // Estados para campos
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

    val backgroundColors = listOf(
        Color(0xFFFFA726),
        Color(0xFFFDD835),
        Color(0xFFE57373)
    )

    var currentStep by remember { mutableStateOf(0) }
    var isRutValidNow by remember { mutableStateOf(true) }
    var isRutInUse by remember { mutableStateOf(false) }

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

            when (currentStep) {
                0 -> StepOne(
                    nombre = nombre,
                    rut = rut,
                    email = email,
                    selectedCountryName = selectedCountryName,
                    selectedRegionName = selectedRegionName,
                    selectedCommune = selectedCommune,
                    onNombreChange = { nombre = it },
                    onRutChange = { newText ->
                        rut = newText.filter { it.isDigit() || it == '.' || it == '-' || it == 'K' || it == 'k' }
                        if (rut.length > 12) rut = rut.take(12)

                        isRutValidNow = validarRUT(rut)

                        firestore.collection("usuarios")
                            .whereEqualTo("rut", cleanRUT(rut))
                            .get()
                            .addOnSuccessListener { snapshot ->
                                isRutInUse = !snapshot.isEmpty
                            }
                    },
                    onEmailChange = { email = it },
                    onDireccionChange = { direccion = it },
                    onNumeroChange = { numero = it.filter { it.isDigit() } },
                    onCountrySelected = { selectedCountryName = it },
                    onRegionSelected = { selectedRegionName = it },
                    onCommuneSelected = { name ->
                        selectedCommune = communeList.find { it.name == name }
                    },
                    countryList = countryList,
                    regionList = regionList,
                    communeList = communeList.map { it.name },
                    shakeFields = shakeFields,
                    missingFieldsMessage = missingFieldsMessage,
                    isRutValidNow = isRutValidNow,
                    isRutInUse = isRutInUse,
                    direccion = direccion,
                    numero = numero
                )
                1 -> StepTwo(
                    gender = gender,
                    birthdayFormatted = birthdayFormatted,
                    dateText = dateText,
                    onGenderChange = { gender = it },
                    onBirthdayChange = { newDate ->
                        birthdayFormatted = newDate
                        day = newDate.split("/")[0].toInt()
                        month = newDate.split("/")[1].toInt() - 1
                        year = newDate.split("/")[2].toInt()
                    },
                    shakeFields = shakeFields,
                    missingFieldsMessage = missingFieldsMessage
                )
                2 -> StepThree(
                    password = password,
                    confirmPassword = confirmPassword,
                    requisitosCumplidos = requisitosCumplidos,
                    passwordsMatch = passwordsMatch,
                    onPasswordChange = { password = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    shakeFields = shakeFields,
                    missingFieldsMessage = missingFieldsMessage
                )
                3 -> StepFour(
                    nombre = nombre,
                    rut = rut,
                    email = email,
                    pais = selectedCountryName,
                    region = selectedRegionName,
                    comuna = selectedCommune?.name ?: "",
                    genero = gender,
                    fechaNac = birthdayFormatted,
                    direccionValida = direccion.isNotBlank(),
                    numeroValido = numero.isNotBlank(),
                    isEmailValid = isValidEmail(email),
                    isRutValid = validarRUT(rut),
                    requisitosCumplidos = requisitosCumplidos,
                    passwordsMatch = passwordsMatch,
                    onNavigateToStep = { step -> currentStep = step },
                    onRegistrarseClick = {
                        val isEmailValid = isValidEmail(email)
                        val isRutValid = validarRUT(rut)
                        val isPasswordOk = requisitosCumplidos && passwordsMatch

                        if (!isRutValid) {
                            Toast.makeText(context, "El RUT es inválido.", Toast.LENGTH_SHORT).show()
                            return@StepFour
                        }
                        if (!isEmailValid) {
                            Toast.makeText(context, "Correo electrónico inválido.", Toast.LENGTH_SHORT).show()
                            return@StepFour
                        }
                        if (!isPasswordOk) {
                            Toast.makeText(context, "La contraseña no cumple los requisitos.", Toast.LENGTH_SHORT).show()
                            return@StepFour
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
                                val hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt())
                                val userData = hashMapOf<String, Any>(
                                    "nombre" to nombre,
                                    "rut" to cleanRUT(rut),
                                    "numero" to numero,
                                    "direccion" to direccion,
                                    "email" to email.lowercase(),
                                    "genero" to gender,
                                    "fecha_nacimiento" to birthdayFormatted,
                                    "ubicacion" to mapOf(
                                        "pais" to selectedCountryName,
                                        "region" to selectedRegionName,
                                        "comuna" to (selectedCommune?.name ?: "")
                                    ),
                                    "password_hash" to hashedPassword,
                                    "code_recovery" to "",
                                    "state_recovery" to false
                                )
                                firestore.collection("usuarios")
                                    .add(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                                        context.startActivity(Intent(context, LoginActivity::class.java))
                                        (context as? android.app.Activity)?.finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Error al guardar datos", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnCompleteListener {
                                        loading = false
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al validar RUT", Toast.LENGTH_SHORT).show()
                                loading = false
                            }
                    },
                    loading = loading
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentStep > 0) {
                    Button(onClick = { currentStep-- }) {
                        Text("Anterior")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                if (currentStep < 3) {
                    Button(
                        onClick = {
                            val missing = mutableSetOf<String>()
                            missingFieldsMessage = ""
                            when (currentStep) {
                                0 -> {
                                    if (nombre.isBlank()) missing.add("nombre")
                                    if (rut.isBlank()) missing.add("rut")
                                    if (email.isBlank()) missing.add("email")
                                    if (selectedCountryName.isBlank()) missing.add("pais")
                                    if (selectedRegionName.isBlank()) missing.add("region")
                                    if (selectedCommune == null || selectedCommune!!.name.isBlank()) missing.add("comuna")
                                    if (direccion.isBlank()) missing.add("direccion")
                                    if (numero.isBlank()) missing.add("numero")
                                }
                                1 -> {
                                    if (gender.isBlank()) missing.add("género")
                                    if (birthdayFormatted.isBlank()) missing.add("fecha")
                                }
                                2 -> {
                                    if (!requisitosCumplidos) missing.add("password")
                                    if (!passwordsMatch) missing.add("confirmPassword")
                                }
                            }

                            if (missing.isNotEmpty()) {
                                shakeFields = missing
                                missingFieldsMessage = "Por favor completa los campos faltantes."
                                coroutineScope.launch {
                                    delay(300)
                                    shakeFields = emptySet()
                                }
                                return@Button
                            }

                            currentStep++
                        }
                    ) {
                        Text("Siguiente")
                    }
                } else {
                    val rotation by animateFloatAsState(if (loading) 360f else 0f)
                    val scale by animateFloatAsState(if (loading) 1.1f else 1f)

                    Button(
                        onClick = {
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

                            firestore.collection("usuarios")
                                .whereEqualTo("rut", cleanRUT(rut))
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (!querySnapshot.isEmpty()) {
                                        Toast.makeText(context, "Este RUT ya está registrado.", Toast.LENGTH_SHORT).show()
                                        loading = false
                                        return@addOnSuccessListener
                                    }
                                    val hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt())
                                    val userData = hashMapOf<String, Any>(
                                        "nombre" to nombre,
                                        "rut" to cleanRUT(rut),
                                        "numero" to numero,
                                        "direccion" to direccion,
                                        "email" to email.lowercase(),
                                        "genero" to gender,
                                        "fecha_nacimiento" to birthdayFormatted,
                                        "ubicacion" to mapOf(
                                            "pais" to selectedCountryName,
                                            "region" to selectedRegionName,
                                            "comuna" to (selectedCommune?.name ?: "")
                                        ),
                                        "password_hash" to hashedPassword,
                                        "code_recovery" to "",
                                        "state_recovery" to false
                                    )
                                    firestore.collection("usuarios")
                                        .add(userData)
                                        .addOnSuccessListener {
                                            loading = false
                                            Toast.makeText(context, "Registro completado", Toast.LENGTH_SHORT).show()
                                            context.startActivity(Intent(context, LoginActivity::class.java))
                                            (context as? android.app.Activity)?.finish()
                                        }
                                        .addOnFailureListener {
                                            loading = false
                                            Toast.makeText(context, "Error al crear usuario", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener {
                                    loading = false
                                    Toast.makeText(context, "Error al validar RUT", Toast.LENGTH_SHORT).show()
                                }
                        },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .rotate(rotation)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Crear Cuenta", color = Color.White)
                    }
                }
            }

            TextButton(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }

    LaunchedEffect(Unit) {
        firestore.collection("countries").get().addOnSuccessListener { snapshot ->
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

// PASO 1: Datos básicos
@Composable
fun StepOne(
    nombre: String,
    rut: String,
    email: String,
    selectedCountryName: String,
    selectedRegionName: String,
    selectedCommune: Commune?,
    onNombreChange: (String) -> Unit,
    onRutChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDireccionChange: (String) -> Unit,
    onNumeroChange: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onRegionSelected: (String) -> Unit,
    onCommuneSelected: (String) -> Unit,
    countryList: List<String>,
    regionList: List<String>,
    communeList: List<String>,
    shakeFields: Set<String>,
    missingFieldsMessage: String,
    isRutValidNow: Boolean,
    isRutInUse: Boolean,
    direccion: String,
    numero: String
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState)) {

        // Nombre
        val shakeNombre by animateFloatAsState(
            targetValue = if ("nombre" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
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
            onValueChange = onRutChange,
            label = { Text("RUT (ej: 123456789-1)") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeRut },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(12.dp),
            isError = "rut" in shakeFields
        )
        if (!isRutValidNow && rut.isNotBlank()) {
            Text(text = "RUT inválido", color = Color.Red, fontSize = 12.sp)
        }
        if (isRutInUse && rut.isNotBlank()) {
            Text(text = "Este RUT ya está registrado", color = Color.Red, fontSize = 12.sp)
        }

        // Correo
        val shakeCorreo by animateFloatAsState(
            targetValue = if ("email" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Correo Electrónico") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeCorreo },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            isError = "email" in shakeFields
        )

        // Dirección
        val shakeDireccion by animateFloatAsState(
            targetValue = if ("direccion" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        OutlinedTextField(
            value = direccion,
            onValueChange = onDireccionChange,
            label = { Text("Dirección") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeDireccion },
            shape = RoundedCornerShape(12.dp),
            isError = "direccion" in shakeFields
        )

        // Teléfono
        val shakeTelefono by animateFloatAsState(
            targetValue = if ("numero" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        OutlinedTextField(
            value = numero,
            onValueChange = onNumeroChange,
            label = { Text("Número telefónico") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeTelefono },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            isError = "numero" in shakeFields
        )

        // País
        val shakePais by animateFloatAsState(
            targetValue = if ("pais" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        Text("País", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        CountrySelector(
            items = countryList,
            selectedItem = selectedCountryName,
            onItemSelected = onCountrySelected,
            modifier = Modifier.graphicsLayer { translationX = shakePais }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Región
        val shakeRegion by animateFloatAsState(
            targetValue = if ("region" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        Text("Región", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        RegionSelector(
            items = regionList,
            selectedItem = selectedRegionName,
            onItemSelected = onRegionSelected,
            enabled = selectedCountryName.isNotEmpty(),
            modifier = Modifier.graphicsLayer { translationX = shakeRegion }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Comuna
        val shakeComuna by animateFloatAsState(
            targetValue = if ("comuna" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        Text("Comuna", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        CommuneSelector(
            items = communeList,
            selectedItem = selectedCommune?.name ?: "",
            onItemSelected = onCommuneSelected,
            enabled = selectedRegionName.isNotEmpty(),
            modifier = Modifier.graphicsLayer { translationX = shakeComuna }
        )

        if (missingFieldsMessage.isNotBlank()) {
            Text(
                text = missingFieldsMessage,
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// PASO 2: Género y fecha
@Composable
fun StepTwo(
    gender: String,
    birthdayFormatted: String,
    dateText: String,
    onGenderChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    shakeFields: Set<String>,
    missingFieldsMessage: String
) {
    val context = LocalContext.current
    val maxYear = Calendar.getInstance().get(Calendar.YEAR)
    val maxMonth = Calendar.getInstance().get(Calendar.MONTH)
    val maxDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    Column {
        val shakeGenero by animateFloatAsState(
            targetValue = if ("género" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        Text("Género", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .graphicsLayer { translationX = shakeGenero },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GenderOption(icon = Icons.Default.Male, label = "Masculino", selected = gender == "Masculino") {
                onGenderChange("Masculino")
            }
            GenderOption(
                icon = Icons.Default.Female,
                label = "Femenino",
                selected = gender == "Femenino",
                containerColor = if (gender == "Femenino") Color(0xFFAB47BC).copy(alpha = 0.9f) else Color.LightGray.copy(alpha = 0.8f)
            ) {
                onGenderChange("Femenino")
            }
        }

        val shakeFecha by animateFloatAsState(
            targetValue = if ("fecha" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        Text("Fecha De Nacimiento", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Button(
            onClick = {
                DatePickerDialog(
                    context as android.app.Activity,
                    { _, yearD, monthD, dayD ->
                        if (yearD > maxYear || (yearD == maxYear && monthD > maxMonth) ||
                            (yearD == maxYear && monthD == maxMonth && dayD > maxDay)
                        ) {
                            Toast.makeText(context, "No puedes seleccionar una fecha futura.", Toast.LENGTH_SHORT).show()
                        } else {
                            onBirthdayChange("$dayD/${monthD + 1}/$yearD")
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
            Text(dateText)
        }

        if (missingFieldsMessage.isNotBlank()) {
            Text(
                text = missingFieldsMessage,
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// PASO 3: Contraseña
@Composable
fun StepThree(
    password: String,
    confirmPassword: String,
    requisitosCumplidos: Boolean,
    passwordsMatch: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    shakeFields: Set<String>,
    missingFieldsMessage: String
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column {
        val shakePassword by animateFloatAsState(
            targetValue = if ("password" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )

        RequisitoItem("Al menos 8 caracteres", password.length >= 8, "password" in shakeFields)
        RequisitoItem("Una letra mayúscula", password.any { it.isUpperCase() }, "password" in shakeFields)
        RequisitoItem("Un número", password.any { it.isDigit() }, "password" in shakeFields)
        RequisitoItem("Un carácter especial", password.any { "!@#\$%^&*()-_=+[]{};':\",.<>/?".contains(it) }, "password" in shakeFields)

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
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


        val shakeConfirm by animateFloatAsState(
            targetValue = if ("confirmPassword" in shakeFields) 10f else 0f,
            animationSpec = repeatable(iterations = 3, animation = tween(50), repeatMode = RepeatMode.Reverse)
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirmar Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeConfirm },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            isError = "confirmPassword" in shakeFields
        )

        if (!passwordsMatch && confirmPassword.isNotBlank()) {
            Text(
                text = "Las contraseñas no coinciden.",
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (missingFieldsMessage.isNotBlank()) {
            Text(
                text = missingFieldsMessage,
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun StepFour(
    nombre: String,
    rut: String,
    email: String,
    pais: String,
    region: String,
    comuna: String,
    genero: String,
    fechaNac: String,
    direccionValida: Boolean,
    numeroValido: Boolean,
    isEmailValid: Boolean,
    isRutValid: Boolean,
    requisitosCumplidos: Boolean,
    passwordsMatch: Boolean,
    onNavigateToStep: (Int) -> Unit,
    onRegistrarseClick: () -> Unit,
    loading: Boolean
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        SectionTitle(title = "Datos Personales", stepNumber = 0, onNavigate = onNavigateToStep) {
            InfoRow(label = "Nombre", value = nombre, valid = nombre.isNotBlank())
            InfoRow(label = "RUT", value = rut, valid = isRutValid)
            InfoRow(label = "Correo", value = email, valid = isEmailValid)
            InfoRow(label = "Teléfono", value = numeroValido)
            InfoRow(label = "Dirección", value = direccionValida)
        }

        SectionTitle(title = "Ubicación", stepNumber = 0, onNavigate = onNavigateToStep) {
            InfoRow(label = "País", value = pais, valid = pais.isNotBlank())
            InfoRow(label = "Región", value = region, valid = region.isNotBlank())
            InfoRow(label = "Comuna", value = comuna, valid = comuna.isNotBlank())
        }

        SectionTitle(title = "Datos Biométricos", stepNumber = 1, onNavigate = onNavigateToStep) {
            InfoRow(label = "Género", value = genero, valid = genero.isNotBlank())
            InfoRow(label = "Fecha de nacimiento", value = fechaNac, valid = fechaNac.isNotBlank())
        }

        val rotation by animateFloatAsState(if (loading) 360f else 0f)
        val scale by animateFloatAsState(if (loading) 1.1f else 1f)


    }
}

@Composable
fun SectionTitle(
    title: String,
    stepNumber: Int,
    onNavigate: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(Color.LightGray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { onNavigate(stepNumber) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF357ABD)),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Ir a", color = Color.White, fontSize = 14.sp)
            }
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: Any, valid: Boolean = true) {
    val textColor = if (valid) Color.Black else Color.Red
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$label:", fontWeight = FontWeight.Bold)
        Text(
            text = if (value is String && value.isBlank()) "Campo vacío" else "✓",
            color = textColor
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}

// Validación de RUT
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
        else -> "$dvEsperado"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelector(
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
fun RegionSelector(
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
fun CommuneSelector(
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