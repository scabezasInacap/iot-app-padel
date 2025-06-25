package com.example.paddel

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
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
import androidx.compose.runtime.Composable
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import java.util.*
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.shadow

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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
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

    var loadingCountry by remember { mutableStateOf(false) }
    var loadingRegion by remember { mutableStateOf(false) }
    var loadingCommune by remember { mutableStateOf(false) }

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

    // ... (código anterior)

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
                .background(Color.White.copy(alpha = 0.95f))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
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

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                },
                label = "stepTransition"
            ) { step ->
                when (step) {
                    0 -> StepOne(
                        nombre = nombre,
                        rut = rut,
                        email = email,
                        selectedCountryName = selectedCountryName,
                        selectedRegionName = selectedRegionName,
                        selectedCommune = selectedCommune,
                        onNombreChange = { nombre = it },
                        onRutChange = { newText ->
                            rut =
                                newText.filter { it.isDigit() || it == '.' || it == '-' || it == 'K' || it == 'k' }
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
                        numero = numero,
                        loadingCountry = loadingCountry,
                        loadingRegion = loadingRegion,
                        loadingCommune = loadingCommune,
                        onNext = { currentStep++ },
                        nextEnabled = true,
                        loadingNext = false
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
                        direccion = direccion,
                        numero = numero,
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
                                Toast.makeText(context, "El RUT es inválido.", Toast.LENGTH_SHORT)
                                    .show()
                                return@StepFour
                            }
                            if (!isEmailValid) {
                                Toast.makeText(
                                    context,
                                    "Correo electrónico inválido.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@StepFour
                            }
                            if (!isPasswordOk) {
                                Toast.makeText(
                                    context,
                                    "La contraseña no cumple los requisitos.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@StepFour
                            }

                            loading = true
                            val nombreCapitalizado = nombre.split(" ").joinToString(" ") {
                                it.lowercase().replaceFirstChar { c -> c.uppercase() }
                            }
                            val direccionCapitalizada = direccion.split(" ").joinToString(" ") {
                                it.lowercase().replaceFirstChar { c -> c.uppercase() }
                            }
                            val emailMinuscula = email.lowercase()

                            firestore.collection("usuarios")
                                .whereEqualTo("rut", cleanRUT(rut))
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (!querySnapshot.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "Este RUT ya está registrado.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        loading = false
                                        return@addOnSuccessListener
                                    }
                                    val hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(
                                        password,
                                        org.mindrot.jbcrypt.BCrypt.gensalt()
                                    )
                                    val userData = hashMapOf<String, Any>(
                                        "nombre" to nombreCapitalizado,
                                        "rut" to cleanRUT(rut),
                                        "numero" to numero,
                                        "direccion" to direccionCapitalizada,
                                        "email" to emailMinuscula,
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
                                            Toast.makeText(
                                                context,
                                                "Cuenta creada exitosamente",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            context.startActivity(
                                                Intent(
                                                    context,
                                                    LoginActivity::class.java
                                                )
                                            )
                                            (context as? android.app.Activity)?.finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Error al guardar datos",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnCompleteListener {
                                            loading = false
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al validar RUT",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    loading = false
                                }
                        },
                        loading = loading,
                        onBack = { currentStep-- },
                        onLoginClick = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Mover el siguiente y el botón de inicio de sesión aquí
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (currentStep < 3) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (currentStep > 0) {
                        AnimatedButton(
                            onClick = { currentStep-- },
                            text = "Regresar",
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                TextButton(
                    onClick = {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "¿Ya tienes cuenta? Inicia sesión",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }


    LaunchedEffect(Unit) {
            loadingCountry = true
            firestore.collection("countries").get().addOnSuccessListener { snapshot ->
                countryList = snapshot.documents.mapNotNull { it.getString("name") }
                loadingCountry = false
            }
        }

        LaunchedEffect(selectedCountryName) {
            if (selectedCountryName.isNotEmpty()) {
                loadingRegion = true
                firestore.collection("countries")
                    .whereEqualTo("name", selectedCountryName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { countries ->
                        if (countries.isEmpty()) {
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
    }

    LaunchedEffect(selectedRegionName) {
        if (selectedCountryName.isNotEmpty() && selectedRegionName.isNotEmpty()) {
            loadingCommune = true
            firestore.collection("countries")
                .whereEqualTo("name", selectedCountryName)
                .limit(1)
                .get()
                .addOnSuccessListener { countries ->
                    if (countries.isEmpty()) {
                        loadingCommune = false
                        return@addOnSuccessListener
                    }
                    val countryId = countries.first().id
                    firestore.collection("countries/$countryId/regions")
                        .whereEqualTo("name", selectedRegionName)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { regions ->
                            if (regions.isEmpty()) {
                                loadingCommune = false
                                return@addOnSuccessListener
                            }
                            val regionId = regions.first().id
                            firestore.collection("countries/$countryId/regions/$regionId/communes")
                                .get()
                                .addOnSuccessListener { communes ->
                                    communeList = communes.documents.mapNotNull { doc ->
                                        val name = doc.getString("name") ?: return@mapNotNull null
                                        Commune(doc.id, name)
                                    }
                                    loadingCommune = false
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
    isRutValidNow: Boolean = true,
    isRutInUse: Boolean = false,
    direccion: String,
    numero: String,
    loadingCountry: Boolean = false,
    loadingRegion: Boolean = false,
    loadingCommune: Boolean = false,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    loadingNext: Boolean = false
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var missingFieldsMessage by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(bottom = 72.dp)
        ) {
            // Campo Nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    val capitalized = it.split(" ").joinToString(" ") { part ->
                        part.lowercase().replaceFirstChar { c -> c.uppercase() }
                    }
                    onNombreChange(capitalized)
                },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // RUT
            if (!isRutValidNow && rut.isNotBlank()) {
                Text(text = "RUT inválido", color = Color.Red, fontSize = 12.sp)
            }
            if (isRutInUse && rut.isNotBlank()) {
                Text(text = "Este RUT ya está registrado", color = Color.Red, fontSize = 12.sp)
            }
            OutlinedTextField(
                value = rut,
                onValueChange = onRutChange,
                label = { Text("RUT (ej: 123456789-1)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(12.dp)
            )

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { onEmailChange(it.lowercase()) },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp)
            )

            // Dirección
            OutlinedTextField(
                value = direccion,
                onValueChange = {
                    val capitalized = it.split(" ").joinToString(" ") { part ->
                        part.lowercase().replaceFirstChar { c -> c.uppercase() }
                    }
                    onDireccionChange(capitalized)
                },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Teléfono
            OutlinedTextField(
                value = numero,
                onValueChange = onNumeroChange,
                label = { Text("Número telefónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp)
            )

            // País
            Text("País", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (loadingCountry) {
                Text("Cargando países...", color = Color.Gray, fontSize = 14.sp)
            } else {
                CountrySelector(
                    items = countryList,
                    selectedItem = selectedCountryName,
                    onItemSelected = onCountrySelected,
                    modifier = Modifier
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Región
            Text("Región", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (loadingRegion) {
                Text("Cargando regiones...", color = Color.Gray, fontSize = 14.sp)
            } else {
                RegionSelector(
                    items = regionList,
                    selectedItem = selectedRegionName,
                    onItemSelected = onRegionSelected,
                    enabled = selectedCountryName.isNotEmpty(),
                    modifier = Modifier
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comuna
            Text("Comuna", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (loadingCommune) {
                Text("Cargando comunas...", color = Color.Gray, fontSize = 14.sp)
            } else {
                CommuneSelector(
                    items = communeList,
                    selectedItem = selectedCommune?.name ?: "",
                    onItemSelected = onCommuneSelected,
                    enabled = selectedRegionName.isNotEmpty(),
                    modifier = Modifier
                )
            }

            // Mensaje de error general
            if (missingFieldsMessage.isNotBlank()) {
                Text(
                    text = missingFieldsMessage,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }


        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedButton(
                    onClick = {
                        val missing = mutableListOf<String>()

                        if (nombre.isBlank()) missing.add("Nombre")
                        if (rut.isBlank()) missing.add("RUT")
                        if (email.isBlank()) missing.add("Correo electrónico")
                        if (direccion.isBlank()) missing.add("Dirección")
                        if (numero.isBlank()) missing.add("Teléfono")
                        if (selectedCountryName.isBlank()) missing.add("País")
                        if (selectedRegionName.isBlank()) missing.add("Región")
                        if (selectedCommune == null || selectedCommune.name.isBlank()) missing.add("Comuna")

                        if (missing.isNotEmpty()) {
                            val message = "Por favor completa los siguientes campos faltantes:\n• ${missing.joinToString("\n• ")}"
                            missingFieldsMessage = message
                            return@AnimatedButton
                        }



                        onNext()
                    },
                    text = "Siguiente",
                    enabled = nextEnabled,
                    loading = loadingNext,
                    modifier = Modifier.fillMaxWidth()
                )
                }
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
    direccion: String,
    numero: String,
    isEmailValid: Boolean,
    isRutValid: Boolean,
    requisitosCumplidos: Boolean,
    passwordsMatch: Boolean,
    onNavigateToStep: (Int) -> Unit,
    onRegistrarseClick: () -> Unit,
    loading: Boolean,
    onBack: () -> Unit,
    onLoginClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            AnimatedSectionCard(
                title = "Datos Personales",
                icon = Icons.Default.Visibility,
                expandedInitially = true
            ) {
                InfoRow(label = "Nombre", value = nombre, valid = nombre.isNotBlank())
                InfoRow(label = "RUT", value = rut, valid = isRutValid)
                InfoRow(label = "Correo", value = email, valid = isEmailValid)
                InfoRow(label = "Teléfono", value = numero, valid = numero.isNotBlank())
                InfoRow(label = "Dirección", value = direccion, valid = direccion.isNotBlank())
            }
            AnimatedSectionCard(
                title = "Ubicación",
                icon = Icons.Default.Visibility,
                expandedInitially = false
            ) {
                InfoRow(label = "País", value = pais, valid = pais.isNotBlank())
                InfoRow(label = "Región", value = region, valid = region.isNotBlank())
                InfoRow(label = "Comuna", value = comuna, valid = comuna.isNotBlank())
            }
            AnimatedSectionCard(
                title = "Datos Biométricos",
                icon = Icons.Default.Visibility,
                expandedInitially = false
            ) {
                InfoRow(label = "Género", value = genero, valid = genero.isNotBlank())
                InfoRow(label = "Fecha de nacimiento", value = fechaNac, valid = fechaNac.isNotBlank())
            }
            AnimatedSectionCard(
                title = "Requisitos de la contraseña",
                icon = Icons.Default.Visibility,
                expandedInitially = false
            ) {
                RequisitoItem("Al menos 8 caracteres", requisitosCumplidos, false)
                RequisitoItem("Una letra mayúscula", requisitosCumplidos, false)
                RequisitoItem("Un número", requisitosCumplidos, false)
                RequisitoItem("Un carácter especial", requisitosCumplidos, false)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedButton(
                    onClick = onBack,
                    text = "Regresar",
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    enabled = !loading
                )
            }
        }
    }
}

@Composable
fun AnimatedSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expandedInitially: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(expandedInitially) }
    val transition = updateTransition(targetState = expanded, label = "expand")
    val elevation by transition.animateDp(label = "elevation") { if (it) 12.dp else 4.dp }
    val bgColor by transition.animateColor(label = "bgColor") {
        if (it) Color.White else Color(0xFFF7F9FB)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(elevation, RoundedCornerShape(16.dp), clip = false)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF4F8DFD))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = Color.Gray
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: Any, valid: Boolean = true) {
    val textColor = if (valid) Color(0xFF388E3C) else Color.Red
    val icon = if (valid) Icons.Default.Visibility else Icons.Default.VisibilityOff
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label:", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (value is String && value.isBlank()) "Campo vacío" else value.toString(),
                color = textColor
            )
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp).padding(start = 4.dp))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f)
    val bgColor by animateColorAsState(
        if (enabled) Color(0xFF4F8DFD) else Color(0xFFB0BEC5)
    )
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(14.dp), clip = false),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(8.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
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
