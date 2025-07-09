package com.example.proyectopadel1
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var txtMarcador: TextView
    private lateinit var txtSetActual: TextView
    private lateinit var txtGanador: TextView
    private lateinit var txtSaque: TextView
    private lateinit var txtHistorial: TextView

    private var puntoA = 0
    private var puntoB = 0
    private var set = 1
    private var setsGanadosA = 0
    private var setsGanadosB = 0
    private var historial = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtMarcador = findViewById(R.id.txtMarcador)
        txtSetActual = findViewById(R.id.txtSetActual)
        txtGanador = findViewById(R.id.txtGanador)
        txtSaque = findViewById(R.id.txtSaque)
        txtHistorial = findViewById(R.id.txtHistorial)

        findViewById<Button>(R.id.btnMasA).setOnClickListener {
            puntoA++
            revisar()
        }
        findViewById<Button>(R.id.btnMenosA).setOnClickListener {
            if (puntoA > 0) puntoA--
            revisar()
        }
        findViewById<Button>(R.id.btnMasB).setOnClickListener {
            puntoB++
            revisar()
        }
        findViewById<Button>(R.id.btnMenosB).setOnClickListener {
            if (puntoB > 0) puntoB--
            revisar()
        }
        findViewById<Button>(R.id.btnReiniciar).setOnClickListener {
            puntoA = 0
            puntoB = 0
            set = 1
            setsGanadosA = 0
            setsGanadosB = 0
            historial = ""
            txtGanador.text = ""
            revisar()
        }
        revisar()
    }
    private fun revisar() {
        txtMarcador.text = "Equipo A: $puntoA | Equipo B: $puntoB"
        txtSetActual.text = "Set $set"
        txtSaque.text = "Saque: " + if (set % 2 == 1) "Equipo A" else "Equipo B"
        if ((puntoA >= 4 || puntoB >= 4) && abs(puntoA - puntoB) >= 2) {
            if (puntoA > puntoB) {
                setsGanadosA++
                historial += "Set $set: A=$puntoA, B=$puntoB, Gana A\n"
            } else {
                setsGanadosB++
                historial += "Set $set: A=$puntoA, B=$puntoB, Gana B\n"
            }
            set++
            puntoA = 0
            puntoB = 0
        }
        txtHistorial.text = "Historial de sets:\n$historial"
        if (setsGanadosA == 2) {
            txtGanador.text = "Equipo A gana el partido"
        } else if (setsGanadosB == 2) {
            txtGanador.text = "Equipo B gana el partido"
        }
    }
}



