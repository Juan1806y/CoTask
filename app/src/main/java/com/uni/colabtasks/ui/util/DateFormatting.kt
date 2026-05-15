package com.uni.colabtasks.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val longDateFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es"))

fun formatShortDate(epochMs: Long): String = shortDateFormat.format(Date(epochMs))
fun formatLongDate(epochMs: Long): String = longDateFormat.format(Date(epochMs))
