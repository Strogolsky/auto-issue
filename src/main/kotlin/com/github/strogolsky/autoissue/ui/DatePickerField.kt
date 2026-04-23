package com.github.strogolsky.autoissue.ui

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DatePickerField : JPanel(BorderLayout()) {
    private val textField =
        JBTextField(10).apply {
            emptyText.text = "yyyy-MM-dd"
        }
    private val pickerButton =
        JButton("...").apply {
            isFocusable = false
            toolTipText = "Pick a date"
        }

    var selectedDate: LocalDate? = null
        private set

    init {
        add(textField, BorderLayout.CENTER)
        add(pickerButton, BorderLayout.EAST)

        pickerButton.addActionListener { showCalendarPopup() }

        textField.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = syncFromText()
                override fun removeUpdate(e: DocumentEvent) = syncFromText()
                override fun changedUpdate(e: DocumentEvent) {}
            },
        )
    }

    fun setDate(date: LocalDate?) {
        selectedDate = date
        textField.text = date?.toString() ?: ""
    }

    private fun syncFromText() {
        selectedDate =
            try {
                LocalDate.parse(textField.text)
            } catch (_: DateTimeParseException) {
                null
            }
    }

    private fun showCalendarPopup() {
        val initial = selectedDate ?: LocalDate.now()
        var popup: com.intellij.openapi.ui.popup.JBPopup? = null
        val calendarPanel =
            CalendarPanel(initial) { chosen ->
                setDate(chosen)
                popup?.cancel()
            }
        popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(calendarPanel, calendarPanel)
                .setResizable(false)
                .setFocusable(true)
                .createPopup()
        popup.showUnderneathOf(pickerButton)
    }
}

private class CalendarPanel(
    initialDate: LocalDate,
    private val onDateSelected: (LocalDate) -> Unit,
) : JPanel(BorderLayout()) {
    private var displayedMonth = initialDate.withDayOfMonth(1)
    private val monthLabel = JLabel("", SwingConstants.CENTER)
    private val gridPanel = JPanel()

    init {
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

        val prevButton = JButton("<").apply { isFocusable = false }
        val nextButton = JButton(">").apply { isFocusable = false }
        prevButton.addActionListener { displayedMonth = displayedMonth.minusMonths(1); refresh() }
        nextButton.addActionListener { displayedMonth = displayedMonth.plusMonths(1); refresh() }

        add(
            JPanel(BorderLayout()).apply {
                add(prevButton, BorderLayout.WEST)
                add(monthLabel, BorderLayout.CENTER)
                add(nextButton, BorderLayout.EAST)
            },
            BorderLayout.NORTH,
        )
        add(gridPanel, BorderLayout.CENTER)

        refresh()
    }

    private fun refresh() {
        monthLabel.text =
            "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}"

        gridPanel.removeAll()
        gridPanel.layout = GridLayout(0, 7, 2, 2)

        DayOfWeek.entries.forEach { dow ->
            gridPanel.add(
                JLabel(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER)
                    .apply { foreground = Color.GRAY },
            )
        }

        repeat(displayedMonth.dayOfWeek.value - 1) { gridPanel.add(JLabel("")) }

        repeat(displayedMonth.lengthOfMonth()) { i ->
            val date = displayedMonth.withDayOfMonth(i + 1)
            gridPanel.add(
                JButton((i + 1).toString()).apply {
                    isFocusable = false
                    addActionListener { onDateSelected(date) }
                },
            )
        }

        gridPanel.revalidate()
        gridPanel.repaint()
    }
}
