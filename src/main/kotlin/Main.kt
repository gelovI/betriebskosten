package com.gelov.betriebskosten

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gelov.betriebskosten.data.DatabaseFactory
import com.gelov.betriebskosten.ui.BetriebskostenApp

fun main() {
    println("=== Start der Anwendung ===")

    DatabaseFactory.init()
    println("Nach DatabaseFactory.init()")

    application {
        println("application{} startet UI")

        Window(
            onCloseRequest = ::exitApplication,
            title = "Wohnungskostenabrechnung",
            state = rememberWindowState(width = 1400.dp, height = 900.dp)
        ) {
            println("Window-Content wird aufgebaut")
            BetriebskostenApp()
        }

        println("application{} – nach dem Window-Block")
    }

    println("application{} wurde normal beendet")
}

