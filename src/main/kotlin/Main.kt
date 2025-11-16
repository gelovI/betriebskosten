package com.gelov.betriebskosten

import androidx.compose.ui.window.application
import com.gelov.betriebskosten.data.DatabaseFactory
import com.gelov.betriebskosten.ui.BetriebskostenApp

fun main() {
    DatabaseFactory.init()
    application {
        BetriebskostenApp()
    }
}
