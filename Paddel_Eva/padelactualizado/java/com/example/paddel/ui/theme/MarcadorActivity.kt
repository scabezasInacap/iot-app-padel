package com.example.paddel.ui.theme

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.paddel.R

class MarcadorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marcador)

        val inputA = findViewById<EditText>(R.id.editPuntajeA)
        val inputB = findViewById<EditText>(R.id.editPuntajeB)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarMarcador)
        val txtResultado = findViewById<TextView>(R.id.txtResultado)

        btnGuardar.setOnClickListener {
            val puntajeA = inputA.text.toString()
            val puntajeB = inputB.text.toString()
            val resultado = "A: $puntajeA  B: $puntajeB"

            val prefs = getSharedPreferences("HistorialPrefs", MODE_PRIVATE)
            val previo = prefs.getString("historial", "")
            val nuevo = if (previo.isNullOrEmpty()) resultado else "$previo||$resultado"
            prefs.edit().putString("historial", nuevo).apply()

            txtResultado.text = "Guardado: $resultado"
        }
    }
}