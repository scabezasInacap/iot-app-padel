package com.example.paddel.ui.theme

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.paddel.R

class HistorialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        val txtHistorial = findViewById<TextView>(R.id.txtHistorialCompleto)
        val prefs = getSharedPreferences("HistorialPrefs", MODE_PRIVATE)
        val historial = prefs.getString("historial", "")
        txtHistorial.text = historial
            ?.split("||")
            ?.joinToString("\n")
            ?: "No hay historial disponible."
    }
}
