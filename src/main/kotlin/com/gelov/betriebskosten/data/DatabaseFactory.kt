package com.gelov.betriebskosten.data

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    fun init() {
        val url = "jdbc:mysql://localhost:3306/betriebskosten?useSSL=false&serverTimezone=UTC"
        val user = "root"
        val password = ""

        Database.connect(
            url = url,
            driver = "com.mysql.cj.jdbc.Driver",
            user = user,
            password = password
        )
    }
}
