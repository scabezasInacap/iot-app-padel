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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.res.painterResource
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.io.IOException




data class Commune(val id: String, val name: String)

class RegistroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val desdeLogin = intent.getBooleanExtra("verificar_desde_login", false)
        val email = intent.getStringExtra("verificar_email") ?: ""
        val userId = intent.getStringExtra("verificar_user_id")
        setContent {
            PaddelTheme(darkTheme = false) {
                RegisterFinalScreen(
                    startStep = if (desdeLogin) 4 else 0,
                    userIdFromLogin = if (desdeLogin) userId else null,
                    emailFromLogin = if (desdeLogin) email else ""
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RegisterFinalScreen(
    startStep: Int = 0,
    userIdFromLogin: String? = null,
    emailFromLogin: String = ""
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var nombre by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(emailFromLogin) }
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

    var isRutValidNow by remember { mutableStateOf(true) }
    var isRutInUse by remember { mutableStateOf(false) }
    var loadingNext by remember { mutableStateOf(false) }
    var loadingRegister by remember { mutableStateOf(false) }
    var loadingVerify by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(startStep) }
    var userIdCreated by remember { mutableStateOf(userIdFromLogin) }
    var codeGenerated by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val maxYear = calendar.get(Calendar.YEAR)
    val maxMonth = calendar.get(Calendar.MONTH)
    val maxDay = calendar.get(Calendar.DAY_OF_MONTH)
    val dateText = if (birthdayFormatted.isNotBlank()) birthdayFormatted else "Seleccionar Fecha"

    val backgroundColors = listOf(
        Color(0xFFFFA726),
        Color(0xFFFDD835),
        Color(0xFFE57373)
    )

    var showWelcome by remember { mutableStateOf(false) }

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
            Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Si eres nuevo en nuestra aplicación, antes de continuar necesitaremos tus datos para que formes parte de nuestra comunidad.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(targetState = currentStep, label = "step") { step ->
                when (step) {
                    0 -> StepOne(
                        nombre,
                        rut,
                        email,
                        selectedCountryName,
                        selectedRegionName,
                        selectedCommune,
                        onNombreChange = { nombre = it },
                        onRutChange = { rut = it; isRutValidNow = validarRUT(it) },
                        onEmailChange = { email = it },
                        onDireccionChange = { direccion = it },
                        onNumeroChange = { numero = it },
                        onCountrySelected = {
                            selectedCountryName = it; selectedRegionName = ""; selectedCommune =
                            null
                        },
                        onRegionSelected = { selectedRegionName = it; selectedCommune = null },
                        onCommuneSelected = { communeName ->
                            selectedCommune = communeList.find { it.name == communeName }
                        },
                        countryList = countryList,
                        regionList = regionList,
                        communeList = communeList.map { it.name },
                        shakeFields = setOf(), // o la variable correspondiente
                        missingFieldsMessage = "", // o la variable correspondiente
                        isRutValidNow = isRutValidNow,
                        isRutInUse = isRutInUse,
                        direccion = direccion,
                        numero = numero,
                        loadingCountry = loadingCountry,
                        loadingRegion = loadingRegion,
                        loadingCommune = loadingCommune,
                        onNext = { currentStep = 1 },
                        nextEnabled = true,
                        loadingNext = loadingNext,
                        onLoginClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    LoginActivity::class.java
                                )
                            )
                        }
                    )

                    1 -> StepTwo(
                        gender, birthdayFormatted, dateText,
                        onGenderChange = { gender = it },
                        onBirthdayChange = { birthdayFormatted = it },
                        onBack = { currentStep = 0 },
                        onNext = { currentStep = 2 },
                        onLoginClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    LoginActivity::class.java
                                )
                            )
                        }
                    )

                    2 -> StepThree(
                        password, confirmPassword, requisitosCumplidos, passwordsMatch,
                        onPasswordChange = { password = it },
                        onConfirmPasswordChange = { confirmPassword = it },
                        onBack = { currentStep = 1 },
                        onNext = { currentStep = 3 },
                        onLoginClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    LoginActivity::class.java
                                )
                            )
                        }
                    )

                    3 -> StepFour(
                        nombre,
                        rut,
                        email,
                        selectedCountryName,
                        selectedRegionName,
                        selectedCommune?.name ?: "",
                        gender,
                        birthdayFormatted,
                        direccion,
                        numero,
                        requisitosCumplidos,
                        passwordsMatch,
                        onBack = { currentStep = 2 },
                        onRegister = {
                            val rutLimpio = cleanRUT(rut)
                            if (!validarRUT(rutLimpio)) {
                                Toast.makeText(context, "El RUT no es válido", Toast.LENGTH_SHORT).show()
                            }
                            loadingRegister = true
                            val code = (100000..999999).random().toString()
                            codeGenerated = code
                            val expiryMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                            val dateFormat =
                                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val expiryDate = dateFormat.format(Date(expiryMillis))

                            val userData = hashMapOf(
                                "nombre" to nombre,
                                "rut" to rutLimpio,
                                "email" to email,
                                "pais" to selectedCountryName,
                                "region" to selectedRegionName,
                                "comuna" to (selectedCommune?.name ?: ""),
                                "genero" to gender,
                                "fecha_nacimiento" to birthdayFormatted,
                                "direccion" to direccion,
                                "numero" to numero,
                                "password_hash" to BCrypt.hashpw(password, BCrypt.gensalt()),
                                "state_recovery" to true,
                                "code_recovery" to code,
                                "verificado" to false,
                                "code_expiry" to expiryMillis
                            )
                            firestore.collection("usuarios")
                                .add(userData)
                                .addOnSuccessListener { doc ->
                                    userIdCreated = doc.id
                                    sendVerificationCodeByEmail(context, email, code, expiryDate)
                                    Toast.makeText(
                                        context,
                                        "Cuenta creada. Revisa tu correo para verificar.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    currentStep = 4
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al crear usuario",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnCompleteListener {
                                    loadingRegister = false
                                }
                        },
                        loading = loadingRegister,
                        onLoginClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    LoginActivity::class.java
                                )
                            )
                        }
                    )

                    4 -> {
                        val dateFormat = remember {
                            java.text.SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            )
                        }
                        var expiryDateState by remember { mutableStateOf("") }

                        LaunchedEffect(userIdCreated) {
                            if (userIdCreated != null && expiryDateState.isBlank()) {
                                firestore.collection("usuarios").document(userIdCreated!!).get()
                                    .addOnSuccessListener { doc ->
                                        val expiryMillis = doc.getLong("code_expiry") ?: 0L
                                        if (expiryMillis > 0L) {
                                            expiryDateState = dateFormat.format(Date(expiryMillis))
                                        }
                                    }
                            }
                        }

                        StepVerifyCode(
                            email = email,
                            expiryDate = expiryDateState,
                            onCodeVerified = {
                                currentStep = 5
                            },
                            onBack = { currentStep = 3 },
                            loading = loadingVerify,
                            onResend = {
                                if (userIdCreated != null) {
                                    val newCode = (100000..999999).random().toString()
                                    codeGenerated = newCode
                                    val newExpiryMillis =
                                        System.currentTimeMillis() + 24 * 60 * 60 * 1000
                                    expiryDateState = dateFormat.format(Date(newExpiryMillis))
                                    val updateData = mapOf(
                                        "code_recovery" to newCode,
                                        "code_expiry" to newExpiryMillis
                                    )
                                    firestore.collection("usuarios").document(userIdCreated!!)
                                        .update(updateData)
                                        .addOnSuccessListener {
                                            sendVerificationCodeByEmail(
                                                context,
                                                email,
                                                newCode,
                                                expiryDateState
                                            )
                                            Toast.makeText(
                                                context,
                                                "Código reenviado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                        )
                    }

                    5 -> WelcomeScreen(
                        onLoginClick = {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                            (context as? android.app.Activity)?.finish()
                        }
                    )
                }
            }
        }
    }
    // Carga de países, regiones y comunas
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
    loadingNext: Boolean = false,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    var shakeFieldsState by remember { mutableStateOf(setOf<String>()) }
    var errorMessages by remember { mutableStateOf(mapOf<String, String>()) }
    var loadingUnique by remember { mutableStateOf(false) }

    fun validateFields(): Boolean {
        val missing = mutableSetOf<String>()
        val errors = mutableMapOf<String, String>()

        if (nombre.isBlank()) {
            missing.add("nombre")
            errors["nombre"] = "El nombre es obligatorio."
        }
        if (rut.isBlank()) {
            missing.add("rut")
            errors["rut"] = "El RUT es obligatorio."
        } else if (!isRutValidNow) {
            missing.add("rut")
            errors["rut"] = "El RUT no es válido."
        }
        if (email.isBlank()) {
            missing.add("email")
            errors["email"] = "El correo electrónico es obligatorio."
        } else if (!isValidEmail(email)) {
            missing.add("email")
            errors["email"] = "El correo electrónico no es válido."
        }
        if (direccion.isBlank()) {
            missing.add("direccion")
            errors["direccion"] = "La dirección es obligatoria."
        }
        if (numero.isBlank()) {
            missing.add("numero")
            errors["numero"] = "El número telefónico es obligatorio."
        }
        if (selectedCountryName.isBlank()) {
            missing.add("pais")
            errors["pais"] = "Selecciona un país."
        }
        if (selectedRegionName.isBlank()) {
            missing.add("region")
            errors["region"] = "Selecciona una región."
        }
        if (selectedCommune == null || selectedCommune.name.isBlank()) {
            missing.add("comuna")
            errors["comuna"] = "Selecciona una comuna."
        }

        shakeFieldsState = missing
        errorMessages = errors
        return missing.isEmpty()
    }

    fun checkRutAndEmailUnique(
        rut: String,
        email: String,
        onResult: (Boolean, Boolean) -> Unit
    ) {
        loadingUnique = true
        firestore.collection("usuarios")
            .whereEqualTo("rut", rut)
            .get()
            .addOnSuccessListener { rutSnapshot ->
                val rutExists = !rutSnapshot.isEmpty
                firestore.collection("usuarios")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { emailSnapshot ->
                        val emailExists = !emailSnapshot.isEmpty
                        loadingUnique = false
                        onResult(rutExists, emailExists)
                    }
            }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Nombre
        val shakeNombre by animateFloatAsState(
            targetValue = if ("nombre" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["nombre"] != null) {
            Text(errorMessages["nombre"]!!, color = Color.Red, fontSize = 13.sp)
        }
        OutlinedTextField(
            value = nombre,
            singleLine = true,
            onValueChange = {
                val capitalized = it.split(" ").joinToString(" ") { part ->
                    part.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
                onNombreChange(capitalized)
            },
            label = { Text("Nombre") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeNombre },
            isError = "nombre" in shakeFieldsState,
            shape = RoundedCornerShape(12.dp)
        )

        // RUT
        val shakeRut by animateFloatAsState(
            targetValue = if ("rut" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["rut"] != null) {
            Text(errorMessages["rut"]!!, color = Color.Red, fontSize = 13.sp)
        }
        OutlinedTextField(
            value = rut,
            singleLine = true,
            onValueChange = onRutChange,
            label = { Text("RUT (ej: 123456789-1)") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeRut },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            isError = "rut" in shakeFieldsState,
            shape = RoundedCornerShape(12.dp)
        )

        // Email
        val shakeEmail by animateFloatAsState(
            targetValue = if ("email" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["email"] != null) {
            Text(errorMessages["email"]!!, color = Color.Red, fontSize = 13.sp)
        }
        OutlinedTextField(
            value = email,
            singleLine = true,
            onValueChange = { onEmailChange(it.lowercase()) },
            label = { Text("Correo Electrónico") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeEmail },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = "email" in shakeFieldsState,
            shape = RoundedCornerShape(12.dp)
        )

        // Dirección
        val shakeDireccion by animateFloatAsState(
            targetValue = if ("direccion" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["direccion"] != null) {
            Text(errorMessages["direccion"]!!, color = Color.Red, fontSize = 13.sp)
        }
        OutlinedTextField(
            value = direccion,
            singleLine = true,
            onValueChange = {
                val capitalized = it.split(" ").joinToString(" ") { part ->
                    part.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
                onDireccionChange(capitalized)
            },
            label = { Text("Dirección") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeDireccion },
            isError = "direccion" in shakeFieldsState,
            shape = RoundedCornerShape(12.dp)
        )

        // Teléfono
        val shakeNumero by animateFloatAsState(
            targetValue = if ("numero" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["numero"] != null) {
            Text(errorMessages["numero"]!!, color = Color.Red, fontSize = 13.sp)
        }
        OutlinedTextField(
            value = numero,
            singleLine = true,
            onValueChange = onNumeroChange,
            label = { Text("Número telefónico") },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeNumero },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = "numero" in shakeFieldsState,
            shape = RoundedCornerShape(12.dp)
        )

        // País
        val shakePais by animateFloatAsState(
            targetValue = if ("pais" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["pais"] != null) {
            Text(errorMessages["pais"]!!, color = Color.Red, fontSize = 13.sp)
        }
        Text("País", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (loadingCountry) {
            Text("Cargando países...", color = Color.Gray, fontSize = 14.sp)
        } else {
            Box(modifier = Modifier.graphicsLayer { translationX = shakePais }) {
                CountrySelector(
                    items = countryList,
                    selectedItem = selectedCountryName,
                    onItemSelected = onCountrySelected
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Región
        val shakeRegion by animateFloatAsState(
            targetValue = if ("region" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["region"] != null) {
            Text(errorMessages["region"]!!, color = Color.Red, fontSize = 13.sp)
        }
        Text("Región", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (loadingRegion) {
            Text("Cargando regiones...", color = Color.Gray, fontSize = 14.sp)
        } else {
            Box(modifier = Modifier.graphicsLayer { translationX = shakeRegion }) {
                RegionSelector(
                    items = regionList,
                    selectedItem = selectedRegionName,
                    onItemSelected = onRegionSelected,
                    enabled = selectedCountryName.isNotEmpty(),
                    modifier = Modifier
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Comuna
        val shakeComuna by animateFloatAsState(
            targetValue = if ("comuna" in shakeFieldsState) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["comuna"] != null) {
            Text(errorMessages["comuna"]!!, color = Color.Red, fontSize = 13.sp)
        }
        Text("Comuna", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (loadingCommune) {
            Text("Cargando comunas...", color = Color.Gray, fontSize = 14.sp)
        } else {
            Box(modifier = Modifier.graphicsLayer { translationX = shakeComuna }) {
                CommuneSelector(
                    items = communeList,
                    selectedItem = selectedCommune?.name ?: "",
                    onItemSelected = onCommuneSelected,
                    enabled = selectedRegionName.isNotEmpty(),
                    modifier = Modifier
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedButton(
            onClick = {
                if (validateFields()) {
                    checkRutAndEmailUnique(rut, email) { rutExists, emailExists ->
                        if (rutExists) {
                            errorMessages = errorMessages + ("rut" to "Este RUT ya está registrado.")
                            shakeFieldsState = shakeFieldsState + "rut"
                        } else if (emailExists) {
                            errorMessages = errorMessages + ("email" to "Este correo ya está registrado.")
                            shakeFieldsState = shakeFieldsState + "email"
                        } else {
                            onNext()
                        }
                    }
                }
            },
            text = "Siguiente",
            enabled = nextEnabled && !loadingUnique,
            loading = loadingNext || loadingUnique,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
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
    onBack: () -> Unit,
    onNext: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    var shakeFields by remember { mutableStateOf(setOf<String>()) }
    var errorMessages by remember { mutableStateOf(mapOf<String, String>()) }

    fun validateFields(): Boolean {
        val missing = mutableSetOf<String>()
        val errors = mutableMapOf<String, String>()
        if (gender.isBlank()) {
            missing.add("genero")
            errors["genero"] = "Selecciona tu género."
        }
        if (birthdayFormatted.isBlank()) {
            missing.add("fecha")
            errors["fecha"] = "Selecciona tu fecha de nacimiento."
        }
        shakeFields = missing
        errorMessages = errors
        return missing.isEmpty()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val shakeGenero by animateFloatAsState(
            targetValue = if ("genero" in shakeFields) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["genero"] != null) {
            Text(errorMessages["genero"]!!, color = Color.Red, fontSize = 13.sp)
        }
        Text("Género", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeGenero },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GenderOption(
                icon = Icons.Default.Male,
                label = "Masculino",
                selected = gender == "Masculino",
                color = Color(0xFF357ABD) // azul
            ) {
                onGenderChange("Masculino")
            }
            GenderOption(
                icon = Icons.Default.Female,
                label = "Femenino",
                selected = gender == "Femenino",
                color = Color(0xFF9C27B0) // morado
            ) {
                onGenderChange("Femenino")
            }
        }


        val shakeFecha by animateFloatAsState(
            targetValue = if ("fecha" in shakeFields) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["fecha"] != null) {
            Text(errorMessages["fecha"]!!, color = Color.Red, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Fecha De Nacimiento", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    val minYear = year - 100

                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            // Validar que la fecha no sea futura ni menor a minYear
                            val selected = Calendar.getInstance().apply { set(y, m, d) }
                            val now = Calendar.getInstance()
                            if (selected.after(now)) {
                                Toast.makeText(context, "No puedes seleccionar una fecha futura", Toast.LENGTH_SHORT).show()
                            } else if (y < minYear) {
                                Toast.makeText(context, "Fecha demasiado antigua", Toast.LENGTH_SHORT).show()
                            } else {
                                onBirthdayChange("%02d/%02d/%04d".format(d, m + 1, y))
                            }
                        },
                        year, month, day
                    ).apply {
                        datePicker.maxDate = calendar.timeInMillis
                        calendar.set(minYear, 0, 1)
                        datePicker.minDate = calendar.timeInMillis
                    }.show()
                },
                modifier = Modifier
                    .graphicsLayer { translationX = shakeFecha }
                    .padding(vertical = 8.dp)
            ) {
                Text(dateText)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)

            ) {
                Text("Atrás")
            }
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedButton(
                onClick = {
                    if (validateFields()) {
                        onNext()
                    }
                },
                text = "Siguiente",
                enabled = true,
                loading = false,
                modifier = Modifier.weight(1f)
            )
        }
        TextButton(
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}


@Composable
fun StepThree(
    password: String,
    confirmPassword: String,
    requisitosCumplidos: Boolean,
    passwordsMatch: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onLoginClick: () -> Unit
) {
    var shakeFields by remember { mutableStateOf(setOf<String>()) }
    var errorMessages by remember { mutableStateOf(mapOf<String, String>()) }
    var passwordVisible by remember { mutableStateOf(false) }

    fun validateFields(): Boolean {
        val missing = mutableSetOf<String>()
        val errors = mutableMapOf<String, String>()
        if (!requisitosCumplidos) {
            missing.add("password")
            errors["password"] = "La contraseña no cumple los requisitos."
        }
        if (!passwordsMatch) {
            missing.add("confirmPassword")
            errors["confirmPassword"] = "Las contraseñas no coinciden."
        }
        shakeFields = missing
        errorMessages = errors
        return missing.isEmpty()
    }

    Column {
        val shakePassword by animateFloatAsState(
            targetValue = if ("password" in shakeFields) 10f else 0f,
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["password"] != null) {
            Text(errorMessages["password"]!!, color = Color.Red, fontSize = 13.sp)
        }
        RequisitoItem("Al menos 8 caracteres", password.length >= 8, "password" in shakeFields)
        RequisitoItem("Una letra mayúscula", password.any { it.isUpperCase() }, "password" in shakeFields)
        RequisitoItem("Un número", password.any { it.isDigit() }, "password" in shakeFields)
        RequisitoItem("Un carácter especial", password.any { "!@#\$%^&*()-_=+[]{};':\",.<>/?".contains(it) }, "password" in shakeFields)

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakePassword },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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
            animationSpec = repeatable(3, tween(50), RepeatMode.Reverse)
        )
        if (errorMessages["confirmPassword"] != null) {
            Text(errorMessages["confirmPassword"]!!, color = Color.Red, fontSize = 13.sp)
        }
        OutlinedTextField(
            value = confirmPassword,
            singleLine = true,
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

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedButton(
                onClick = {
                    if (validateFields()) {
                        onNext()
                    }
                },
                text = "Siguiente",
                enabled = true,
                loading = false,
                modifier = Modifier.weight(1f)
            )
        }
        TextButton(
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
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
    requisitosCumplidos: Boolean,
    passwordsMatch: Boolean,
    onBack: () -> Unit,
    onRegister: () -> Unit,
    loading: Boolean,
    onLoginClick: () -> Unit
) {
    val (expanded1, setExpanded1) = remember { mutableStateOf(false) }
    val (expanded2, setExpanded2) = remember { mutableStateOf(false) }
    val (expanded3, setExpanded3) = remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Revisa tus datos",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF0D47A1),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        ExpandableCard(
            title = "Datos personales",
            expanded = expanded1,
            onExpandChange = setExpanded1
        ) {
            InfoField("Nombre", nombre)
            InfoField("RUT", rut)
            InfoField("Correo", email)
            InfoField("País", pais)
            InfoField("Región", region)
            InfoField("Comuna", comuna)
            InfoField("Dirección", direccion)
            InfoField("Número", numero)
        }
        ExpandableCard(
            title = "Datos biométricos",
            expanded = expanded2,
            onExpandChange = setExpanded2
        ) {
            InfoField("Género", genero)
            InfoField("Fecha de nacimiento", fechaNac)
        }
        ExpandableCard(
            title = "Contraseña y seguridad",
            expanded = expanded3,
            onExpandChange = setExpanded3
        ) {
            Text("Contraseña:", fontWeight = FontWeight.SemiBold)
            RequisitoItem("Al menos 8 caracteres", requisitosCumplidos, false)
            RequisitoItem("Las contraseñas coinciden", passwordsMatch, false)
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedButton(
                onClick = onRegister,
                text = "Crear Cuenta",
                enabled = requisitosCumplidos && passwordsMatch,
                loading = loading,
                modifier = Modifier.weight(1f)
            )
        }
        TextButton(
            onClick = onLoginClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}


@Composable
fun InfoField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            text = if (value.isBlank()) "Campo vacío" else value,
            color = Color(0xFF357ABD),
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
        Divider(modifier = Modifier.padding(top = 6.dp), color = Color(0xFFE0E0E0))
    }
}

@Composable
fun StepVerifyCode(
    email: String,
    expiryDate: String,
    onCodeVerified: () -> Unit,
    onBack: () -> Unit,
    loading: Boolean,
    onResend: () -> Unit
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Verifica tu correo", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ingresa el código que enviamos a ")
        Text(email, color = Color(0xFF0D47A1)) // Azul
        Spacer(modifier = Modifier.height(8.dp))
        Text("Válido hasta: $expiryDate", color = Color.Gray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Código de verificación") },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

        )
        if (error != null) {
            Text(error!!, color = Color.Red, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedButton(
                onClick = {
                    verificarCodigo(context, email, code, {
                        error = null
                        onCodeVerified()
                    }, {
                        error = "Código incorrecto o expirado"
                    })
                },
                text = "Verificar",
                enabled = !loading,
                loading = loading,
                modifier = Modifier.weight(1f)
            )
        }
        TextButton(onClick = onResend) {
            Text("Reenviar código")
        }
    }
}

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.mipmap.paddel_launcher_foreground),
            contentDescription = "Logo",
            modifier = Modifier.size(120.dp),
            tint = Color(0xFF0D47A1)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "¡Bienvenido a nuestra aplicación!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Tu cuenta ha sido verificada con éxito.",
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Ir a Iniciar Sesión")
        }
    }
}

@Composable
fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(arrowRotation)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}


@Composable
fun AnimatedErrorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    errorMessage: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val shakeAnim by animateFloatAsState(
        targetValue = if (isError) 16f else 0f,
        animationSpec = repeatable(4, tween(60), RepeatMode.Reverse)
    )
    val colorAnim by animateColorAsState(
        targetValue = if (isError) Color(0xFFFF5252) else Color(0xFF0D47A1)
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (isError) 1.05f else 1f
    )
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = shakeAnim
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorAnim,
                unfocusedBorderColor = colorAnim,
                errorBorderColor = Color.Red
            ),
            shape = RoundedCornerShape(14.dp)
        )
        if (isError && errorMessage != null) {
            Text(
                errorMessage,
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
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
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        modifier = modifier
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0D47A1),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp) // Sin sombra
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
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

@Composable
fun InfoRow(label: String, value: Any, valid: Boolean = true) {
    val textColor = if (valid) Color(0xFF388E3C) else Color.Red
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label:", fontWeight = FontWeight.Bold)
        Text(
            text = if (value is String && value.isBlank()) "Campo vacío" else value.toString(),
            color = textColor
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

fun validarRUT(rut: String): Boolean {
    val cleanRut = rut.filter { it.isDigit() || it == 'K' || it == 'k' }
    if (cleanRut.length < 8) return false
    if (cleanRut.all { it == '0' }) return false
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

fun sendVerificationCodeByEmail(context: android.content.Context, email: String, code: String, expiryDate: String) {
    val client = OkHttpClient()
    val jsonBody = JSONObject().apply {
        put("service_id", "service_9aadq5j")
        put("template_id", "template_hkn8lca")
        put("user_id", "Y0PggIiBi8Aiv2s24")
        val templateParams = JSONObject().apply {
            put("to_email", email)
            put("passcode", code)
            put("name", "Usuario")
            put("expiry", expiryDate)
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

fun getCurrentTimePlusHours(hours: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.HOUR_OF_DAY, hours)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    return "$hour:${String.format("%02d", minute)}"
}

fun verificarCodigo(
    context: android.content.Context,
    email: String,
    code: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("usuarios")
        .whereEqualTo("email", email)
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                onError()
                return@addOnSuccessListener
            }
            val doc = snapshot.documents[0]
            val codeRecovery = doc.getString("code_recovery")
            val expiry = doc.getLong("code_expiry") ?: 0L
            val now = System.currentTimeMillis()
            if (codeRecovery == code && now < expiry) {
                firestore.collection("usuarios")
                    .document(doc.id)
                    .update(
                        mapOf(
                            "verificado" to true,
                            "code_expiry" to null,
                            "code_recovery" to null,
                            "state_recovery" to false
                        )
                    )
                    .addOnSuccessListener { onSuccess() }
            } else {
                onError()
            }
        }
        .addOnFailureListener { onError() }
}


