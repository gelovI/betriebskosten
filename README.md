🏢 Betriebskosten Desktop App

Moderne, schnelle und resiliente Anwendung für Miet- & Betriebskostenabrechnung
Kotlin • Compose Desktop • Exposed • PDF-Export • Live-Recalculation

⭐ Übersicht

Die Betriebskosten Desktop App ist eine moderne, lokal laufende Desktop-Lösung zur Verwaltung und Abrechnung von Betriebskosten für Wohnungen und deren Mieter.

Die App entstand ursprünglich als Laravel-Webprojekt – hat aber inzwischen eine komplette technische Renaissance erlebt:
Jetzt läuft alles nativ, offline, schnell und völlig unabhängig von Browsern oder Server-Backends.

🚀 Features
📄 Vollautomatische PDF-Erstellung

sauber formatierte PDFs (PDFBox)

präzise Kostenverteilung

Umlageschlüssel: Wohnfläche × Monate

Zeitliche Vorauszahlungsperioden als mehrzeilige Darstellung

🏠 Verwaltung von:

Wohnungen

Mietern

Kostenarten

Eigentümer

Archiv gespeicherter Abrechnungen

📆 Abrechnungslogik

dynamisch anpassbare Monate je Wohnung

Standard-Vorauszahlung oder beliebig viele zeitliche Perioden

automatische Neuberechnung ohne Refresh

exakte Rundungslogik (volle Euro)

🛡️ Sicherheits- & Resilience-orientiert

Exposed SQL-Layer mit strikten Queries

Datenkonsistenz bei Änderungen in UI & Repository

vollständige Reset-Logik für Vorauszahlungsperioden

Fehler-Resistenz durch Dialogvalidierung & Null-Safety

🧠 Technologie-Stack
Bereich	Technologie
UI / Desktop-Framework	Compose Multiplatform (Desktop)
Sprache	Kotlin
Persistenz	Exposed SQL (leicht, schnell, robust)
PDF-Generierung	Apache PDFBox
Buildsystem	Gradle Kotlin DSL
Architektur	Repository-Pattern, Stateful Screens

Hauptübersicht

Kostenarten

Vorauszahlungsperioden

PDF-Beispiel

📁 Projektstruktur
```
betriebskosten/
 ├── data/                     # Repositories, Tables
 ├── domain/                   # Fachlogik & Models
 ├── ui/                       # Screens (Compose Desktop)
 │    ├── SettlementScreen.kt
 │    ├── WohnungenScreen.kt
 │    ├── MieterScreen.kt
 │    ├── CostTypeScreen.kt
 │    ├── ArchiveScreen.kt
 │    └── EigentuemerScreen.kt
 ├── ui/util/                  # CommonSimpleTable, UI-Utilities
 ├── PdfService.kt             # PDF Export
 ├── Abrechnung.kt             # Abrechnungslogik
 └── build.gradle.kts          # Project Build Script
```

🔧 Installation

Repository klonen:
```
git clone https://github.com/gelovI/betriebskosten.git
```

In IntelliJ IDEA öffnen.

Gradle Sync abwarten (Compose Desktop lädt automatisch).

Starten:
```
Main.kt → Run
```

📝 Nutzung

Wohnungen, Mieter und Kostenarten anlegen

Abrechnungsjahr wählen

Optional zeitliche Vorauszahlungen pro Wohnung definieren

„Neu berechnen“ klicken

„Speichern“ → erzeugt eine vollständige Archiv-PDF

📦 PDF-Speicherort

Alle Abrechnungen werden automatisch abgelegt unter:
```
/Users/<username>/betriebskosten_pdfs/
```

👤 Autor

Ivan Gelov
Softwareentwickler – Kotlin, Compose, Exposed, AI, Desktop-Systeme, Android-Systeme
🔗 LinkedIn & GitHub Links kannst du hier ergänzen
