package com.gelov.betriebskosten.data

import com.gelov.betriebskosten.domain.AbrechnungErgebnis
import com.gelov.betriebskosten.domain.Eigentuemer
import com.gelov.betriebskosten.domain.Kostenart
import com.gelov.betriebskosten.domain.VorauszahlungsPeriode
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.awt.Color
import java.awt.Desktop
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PdfService {

    // ---------------- Archiv-Helfer ----------------

    private fun archiveDir(): Path {
        val dir = Paths.get(System.getProperty("user.home"), "betriebskosten_pdfs")
        if (Files.notExists(dir)) Files.createDirectories(dir)
        return dir
    }

    fun listArchivedPdfs(): List<File> {
        val dir = archiveDir().toFile()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".pdf", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun deletePdf(file: File) {
        if (file.exists()) file.delete()
    }

    fun openPdf(file: File) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        }
    }

    // ---------------- Format-Helfer ----------------

    private fun BigDecimal.toEuro(): String =
        this.setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',') + " €"

    private fun BigDecimal.toPlainComma(): String =
        this.setScale(8, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')


    private val periodFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM.yyyy")

    private fun formatEuro(bd: BigDecimal): String =
        bd.setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',') + " €"

    // ---------------- PDF-Erzeugung ----------------

    fun generateAbrechnungPdf(
        jahr: Int,
        eigentuemer: Eigentuemer,
        kostenarten: List<Kostenart>,
        ergebnisse: List<AbrechnungErgebnis>,
        periodenByWohnungId: Map<Long, List<VorauszahlungsPeriode>>

    ): File {
        val doc = PDDocument()
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)

        val margin = 40f
        var y = page.mediaBox.height - margin

        val cs = PDPageContentStream(doc, page)

        // Farben wie im Muster
        val headerGray = Color(0xF3, 0xF3, 0xF3)      // Tabellenkopf-Hintergrund
        val borderGray = Color(0xD9, 0xD9, 0xD9)      // Tabellenlinien

        cs.setLineWidth(0.5f)
        cs.setStrokingColor(borderGray)
        cs.setNonStrokingColor(Color.BLACK)

        fun drawText(
            text: String,
            x: Float,
            yPos: Float,
            bold: Boolean = false,
            size: Float = 10f
        ) {
            val font = if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
            cs.beginText()
            cs.setFont(font, size)
            cs.newLineAtOffset(x, yPos)
            cs.showText(text)
            cs.endText()
        }

        fun drawCentered(
            text: String,
            centerX: Float,
            yPos: Float,
            bold: Boolean = false,
            size: Float = 12f
        ) {
            val font = if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
            val width = font.getStringWidth(text) / 1000f * size
            val x = centerX - width / 2f
            cs.beginText()
            cs.setFont(font, size)
            cs.newLineAtOffset(x, yPos)
            cs.showText(text)
            cs.endText()
        }

        fun drawRightAligned(
            text: String,
            rightX: Float,
            yPos: Float,
            bold: Boolean = false,
            size: Float = 10f
        ) {
            val font = if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
            val width = font.getStringWidth(text) / 1000f * size
            val x = rightX - width
            cs.beginText()
            cs.setFont(font, size)
            cs.newLineAtOffset(x, yPos)
            cs.showText(text)
            cs.endText()
        }

        val centerX = page.mediaBox.width / 2f

        // ---------- Titel ----------
        drawCentered("Betriebsabrechnung", centerX, y, bold = true, size = 18f)
        y -= 40f

        // ---------- Kopf-Tabelle: Grundstück / Eigentümer / Abrechnungsperiode ----------

        val headerTableX = margin
        val headerTableWidth = page.mediaBox.width - 2 * margin
        val headerRowHeight = 20f

        val headerColWidths = floatArrayOf(
            headerTableWidth / 3f,
            headerTableWidth / 3f,
            headerTableWidth / 3f
        )

        // Kopfzeile (grau hinterlegt, 3 Labels)
        run {
            cs.setNonStrokingColor(headerGray)
            cs.addRect(headerTableX, y - headerRowHeight, headerTableWidth, headerRowHeight)
            cs.fill()
            cs.setNonStrokingColor(Color.BLACK)

            cs.addRect(headerTableX, y - headerRowHeight, headerTableWidth, headerRowHeight)
            cs.stroke()

            var colX = headerTableX
            headerColWidths.forEach { w ->
                cs.moveTo(colX + w, y - headerRowHeight)
                cs.lineTo(colX + w, y)
                cs.stroke()
                colX += w
            }

            val labels = listOf("Grundstück", "Eigentümer", "Abrechnungsperiode")
            colX = headerTableX
            labels.forEachIndexed { i, label ->
                drawText(label, colX + 6f, y - 13f, bold = true, size = 9f)
                colX += headerColWidths[i]
            }

            y -= headerRowHeight
        }

        // Werte-Zeile darunter
        run {
            cs.addRect(headerTableX, y - headerRowHeight, headerTableWidth, headerRowHeight)
            cs.stroke()

            var colX = headerTableX
            headerColWidths.forEach { w ->
                cs.moveTo(colX + w, y - headerRowHeight)
                cs.lineTo(colX + w, y)
                cs.stroke()
                colX += w
            }

            val grundstueckText = eigentuemer.grundstueck ?: ""
            val eigentumerText = eigentuemer.name
            val periodeText = eigentuemer.abrechnungsperiode
                .ifBlank { "01.01 - 31.12.$jahr" }

            val values = listOf(grundstueckText, eigentumerText, periodeText)
            colX = headerTableX
            values.forEachIndexed { i, value ->
                drawText(value, colX + 6f, y - 13f, bold = false, size = 9f)
                colX += headerColWidths[i]
            }

            y -= headerRowHeight
        }

        y -= 30f

        // ---------- Kostenarten-Tabelle ----------

        drawCentered("Kostenarten:", centerX, y, bold = true, size = 13f)
        y -= 24f

        val costTableX = margin
        val costTableWidth = page.mediaBox.width - 2 * margin
        val costRowHeight = 18f
        val costColWidths = floatArrayOf(
            costTableWidth * 0.40f, // Bezeichnung
            costTableWidth * 0.35f, // Beschreibung
            costTableWidth * 0.25f  // Betrag
        )

        fun drawCostHeader() {
            cs.setNonStrokingColor(headerGray)
            cs.addRect(costTableX, y - costRowHeight, costTableWidth, costRowHeight)
            cs.fill()
            cs.setNonStrokingColor(Color.BLACK)

            cs.addRect(costTableX, y - costRowHeight, costTableWidth, costRowHeight)
            cs.stroke()

            var colX = costTableX
            costColWidths.forEach { w ->
                cs.moveTo(colX + w, y - costRowHeight)
                cs.lineTo(colX + w, y)
                cs.stroke()
                colX += w
            }

            val headerLabels = listOf("Bezeichnung", "Beschreibung", "Betrag")
            colX = costTableX
            headerLabels.forEachIndexed { i, label ->
                drawText(label, colX + 6f, y - 12f, bold = true, size = 9f)
                colX += costColWidths[i]
            }

            y -= costRowHeight
        }

        fun drawCostRow(bezeichnung: String, beschreibung: String, betrag: String) {
            cs.addRect(costTableX, y - costRowHeight, costTableWidth, costRowHeight)
            cs.stroke()

            var colX = costTableX
            costColWidths.forEach { w ->
                cs.moveTo(colX + w, y - costRowHeight)
                cs.lineTo(colX + w, y)
                cs.stroke()
                colX += w
            }

            val fontSize = 9f
            colX = costTableX

            drawText(bezeichnung, colX + 6f, y - 12f, size = fontSize)
            colX += costColWidths[0]

            drawText(beschreibung, colX + 6f, y - 12f, size = fontSize)
            colX += costColWidths[1]

            drawRightAligned(betrag, colX + costColWidths[2] - 6f, y - 12f, size = fontSize)

            y -= costRowHeight
        }

        fun drawCostSummaryRow(text: String, summe: String) {
            cs.setNonStrokingColor(headerGray)
            cs.addRect(costTableX, y - costRowHeight, costTableWidth, costRowHeight)
            cs.fill()
            cs.setNonStrokingColor(Color.BLACK)

            cs.addRect(costTableX, y - costRowHeight, costTableWidth, costRowHeight)
            cs.stroke()

            drawText(text, costTableX + 6f, y - 12f, bold = false, size = 9f)
            drawRightAligned(summe, costTableX + costTableWidth - 6f, y - 12f, size = 9f)

            y -= costRowHeight
        }

        drawCostHeader()
        kostenarten.forEach { k ->
            drawCostRow(
                bezeichnung = k.bezeichnung,
                beschreibung = k.beschreibung ?: "",
                betrag = k.summe.toEuro()
            )
        }

        val gesamt = kostenarten.fold(BigDecimal.ZERO) { acc, k -> acc + k.summe }
        val gesamtEinheiten = ergebnisse.sumOf { it.einheiten }
        val kostenJeEinheit = if (gesamtEinheiten != 0)
            gesamt.divide(BigDecimal(gesamtEinheiten), 8, RoundingMode.HALF_UP)
        else BigDecimal.ZERO

        val summaryText =
            "Summe der umlagefähigen Kosten zu verteilen auf $gesamtEinheiten Einheiten = ${kostenJeEinheit.toPlainComma()}"
        drawCostSummaryRow(summaryText, gesamt.toEuro())

        y -= 12f
        drawText("Umlageschlüssel: Wohnfläche * Monate = Einheit", margin, y, size = 9f)
        y -= 30f

        // ---------- Mieter-Tabelle ----------

        val tenantTableX = margin
        val tenantTableWidth = page.mediaBox.width - 2 * margin

        val tenantHeaderRowHeight = 22f   // Header höher
        val tenantBaseRowHeight = 18f     // Basis-Höhe pro Zeile

        // Spaltenbreiten (wie bei dir schon angepasst)
        val tenantColWidths = floatArrayOf(
            tenantTableWidth * 0.24f, // Mieter (leicht schmaler)
            tenantTableWidth * 0.16f, // Wohnfläche (m²)  → mehr Platz
            tenantTableWidth * 0.07f, // Monate
            tenantTableWidth * 0.19f, // Vorauszahlung (€) → mehr Platz
            tenantTableWidth * 0.10f, // Anteil
            tenantTableWidth * 0.09f, // Einheiten (leicht schmaler)
            tenantTableWidth * 0.15f  // Nachzahlung/Guthaben → breiter
        )

        fun drawTenantHeader() {
            // Hintergrund
            cs.setNonStrokingColor(headerGray)
            cs.addRect(tenantTableX, y - tenantHeaderRowHeight, tenantTableWidth, tenantHeaderRowHeight)
            cs.fill()
            cs.setNonStrokingColor(Color.BLACK)

            // Rahmen außen
            cs.addRect(tenantTableX, y - tenantHeaderRowHeight, tenantTableWidth, tenantHeaderRowHeight)
            cs.stroke()

            // Vertikale Linien
            var colX = tenantTableX
            tenantColWidths.forEach { w ->
                cs.moveTo(colX + w, y - tenantHeaderRowHeight)
                cs.lineTo(colX + w, y)
                cs.stroke()
                colX += w
            }

            val labels = listOf(
                "Mieter",
                "Wohnfläche (m²)",
                "Monate",
                "Vorauszahlung (€)",
                "Anteil",
                "Einheiten",
                "Nachzahlung(-)\nGuthaben(+)"
            )

            colX = tenantTableX
            labels.forEachIndexed { i, label ->
                val lines = label.split('\n')
                lines.forEachIndexed { lineIndex, line ->
                    val yOffset = y - 13f - lineIndex * 8f
                    drawText(line, colX + 6f, yOffset, bold = true, size = 8.5f)
                }
                colX += tenantColWidths[i]
            }

            y -= tenantHeaderRowHeight
        }

        fun drawTenantRow(e: AbrechnungErgebnis) {
            // Perioden für diese Wohnung (falls vorhanden)
            val perioden = periodenByWohnungId[e.wohnungId] ?: emptyList()

            // Anzahl Textzeilen in der Vorauszahlungs-Spalte:
            // pro Periode: 2 Zeilen ("Von ...", "Summe ...")
            val linesForPeriods = if (perioden.isEmpty()) 1 else perioden.size * 2
            val estimatedHeightForPeriods = 6f + linesForPeriods * 9f

            // tatsächliche Zeilenhöhe = max(Basis, benötigte Höhe)
            val rowHeight = maxOf(tenantBaseRowHeight, estimatedHeightForPeriods)

            // Rahmen der Zeile
            cs.addRect(tenantTableX, y - rowHeight, tenantTableWidth, rowHeight)
            cs.stroke()

            // vertikale Linien
            var colX = tenantTableX
            tenantColWidths.forEach { w ->
                cs.moveTo(colX + w, y - rowHeight)
                cs.lineTo(colX + w, y)
                cs.stroke()
                colX += w
            }

            val fontSize = 9f
            var textY = y - 12f
            colX = tenantTableX

            // Mieter
            drawText(e.mieterName, colX + 6f, textY, size = fontSize)
            colX += tenantColWidths[0]

            // Wohnfläche
            drawRightAligned("${e.wohnflaeche}", colX + tenantColWidths[1] - 6f, textY, size = fontSize)
            colX += tenantColWidths[1]

            // Monate
            drawRightAligned(e.monate.toString(), colX + tenantColWidths[2] - 6f, textY, size = fontSize)
            colX += tenantColWidths[2]

            // Vorauszahlung-Spalte
            // Vorauszahlung-Spalte
            val vorausCellLeft = colX
            val vorausCellWidth = tenantColWidths[3]

            if (perioden.isEmpty()) {
                // Standardfall: eine Zeile wie bisher, rechtsbündig
                val vorausRightX = vorausCellLeft + vorausCellWidth - 6f
                drawRightAligned(
                    e.vorauszahlung.toPlainComma(),
                    vorausRightX,
                    textY,
                    size = fontSize
                )
            } else {
                // Zeitlich begrenzte Beträge → mehrere Zeilen, KLEINER und LINKSBÜNDIG
                val periodFontSize = 8f
                var lineY = y - 10f
                val startX = vorausCellLeft + 6f

                perioden.forEach { p ->
                    val vonText = p.von.format(periodFormatter)
                    val bisText = p.bis.format(periodFormatter)

                    // Anzahl Monate in der Periode
                    val startYm = java.time.YearMonth.from(p.von)
                    val endYm = java.time.YearMonth.from(p.bis)
                    val months = java.time.temporal.ChronoUnit.MONTHS.between(startYm, endYm) + 1
                    val periodSum = p.betragMonat * BigDecimal.valueOf(months)

                    // Zeile 1: Von ... bis ...
                    drawText(
                        "Von $vonText bis $bisText",
                        startX,
                        lineY,
                        size = periodFontSize
                    )
                    lineY -= 9f

                    // Zeile 2: Summe: xxx,xx €
                    drawText(
                        "Summe: ${formatEuro(periodSum)}",
                        startX,
                        lineY,
                        size = periodFontSize
                    )
                    lineY -= 10f      // etwas Abstand zur nächsten Periode
                }
            }

            colX += tenantColWidths[3]

            // Anteil (Umlage)
            drawRightAligned(
                e.umlageBetrag.toEuro(),
                colX + tenantColWidths[4] - 6f,
                textY,
                size = fontSize
            )
            colX += tenantColWidths[4]

            // Einheiten
            drawRightAligned(
                e.einheiten.toString(),
                colX + tenantColWidths[5] - 6f,
                textY,
                size = fontSize
            )
            colX += tenantColWidths[5]

            // Nachzahlung / Guthaben
            drawRightAligned(
                e.ergebnis.toEuro(),
                colX + tenantColWidths[6] - 6f,
                textY,
                size = fontSize
            )

            // y für nächste Zeile nach unten verschieben
            y -= rowHeight
        }

        drawTenantHeader()
        ergebnisse.forEach { drawTenantRow(it) }

        cs.close()

        val fileName = "betriebskosten_${jahr}_${LocalDate.now()}.pdf"
        val outFile = archiveDir().resolve(fileName).toFile()
        doc.save(outFile)
        doc.close()
        return outFile
    }
}