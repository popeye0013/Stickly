package com.stickly.app.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openUrl(url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Opens a UPI payment chooser. Returns false when no UPI app is installed. */
fun Context.openUpiPayment(
    upiId: String,
    payeeName: String,
    note: String? = null,
): Boolean {
    val uri = Uri.parse("upi://pay").buildUpon()
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", payeeName)
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tr", "STK" + System.currentTimeMillis())
        .appendQueryParameter("mode", "02")
        .apply { if (!note.isNullOrBlank()) appendQueryParameter("tn", note) }
        .build()

    val payIntent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (packageManager.queryIntentActivities(payIntent, 0).isEmpty()) return false
    val chooser = Intent.createChooser(payIntent, "Pay with UPI").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        startActivity(chooser)
        true
    } catch (e: Exception) {
        false
    }
}

fun Context.copyText(label: String, text: String): Boolean {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    return true
}
