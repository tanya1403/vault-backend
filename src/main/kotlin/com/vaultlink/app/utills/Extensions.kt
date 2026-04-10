package com.vaultlink.app.utills

import org.apache.commons.lang3.StringUtils
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt


fun String?.isNotNullOrNA(): Boolean {
    return (
            null != this
                    && !this.equals(NA, ignoreCase = true)
                    && !this.equals("Null", ignoreCase = true)
                    && this.isNotEmpty()
            )
}

fun String?.isInvalid() = !this.isNotNullOrNA()

fun String?.isValid() = this.isNotNullOrNA()

fun String?.isInvalidLAI() = !this.isNotNullOrNA() && this?.length != 12

fun String?.isValidEmail(): Boolean {
    val regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$".toRegex()
    return this.isNotNullOrNA() && this!!.matches(regex)
}

fun String?.isValidMobile(): Boolean {
    val regex = "^(?:(?:\\+|0{0,2})91(\\s*[\\-]\\s*)?|[0]?)?[789]\\d{9}$".toRegex()
    return this.isNotNullOrNA() && this!!.matches(regex)
}

fun String?.isValidMobileNumber(): Boolean {
    val regex = "[0-9]{10}\$".toRegex()
    return this.isNotNullOrNA() && this!!.matches(regex)
}

fun String?.isInvalidMobileNumber() = !this.isValidMobileNumber()

fun String?.isInvalidEmail() = !this.isValidEmail()

fun String?.maskEmail(): String? {
    return if (this.isValidEmail()) {
        this?.replaceRange(0, this.indexOf("@"), "X".repeat(this.indexOf("@")))
    } else NA
}


fun String?.maskMobileNumber() = this?.let {
    if (!it.isInvalid() && it.length > 8)
        it.replaceRange(0, 8, "X".repeat(8))
    else
        it
}

fun String?.maskCharUpTo(upTo: Int = 8): String {
    return if (this.isNotNullOrNA() && this!!.length >= upTo)
        this.replaceRange(0, upTo, "X".repeat(upTo))
    else NA
}

fun JSONObject?.isSFDataPresent() : Boolean {
    return (null != this && this.getInt("totalSize") > 0)
}

fun Double?.isValid(theValue: Double = NO_DECIMAL_NUMBER) : Boolean {
    return (this ?: theValue) > NO_DECIMAL_NUMBER
}

fun Double?.isInvalid(theValue: Double = NO_DECIMAL_NUMBER) = this?.isValid(theValue) == false

fun Int?.isValid(theValue: Int = NO_NUMBER) : Boolean {
    return (this ?: theValue) > NO_NUMBER
}

fun Int?.isInvalid(theValue: Int = NO_NUMBER) = this?.isValid(theValue) == false

fun String?.toHashMap() : HashMap<String, String> {

    val value = StringUtils.substringBetween(this, "{", "}")

    val keyValuePairs: List<String> = value.split(",") //split the string to creat key-value pairs

    val map: HashMap<String, String> = HashMap()

    for (pair in keyValuePairs)  //iterate over the pairs
    {
        val entry = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray() //split the pairs to get key and value
        map[entry[0].trim { it <= ' ' }] =
            entry[1].trim { it <= ' ' } //add them to the hashmap and trim whitespaces
    }

    return map

}

fun Boolean?.isTrue() = this == true

fun Double.roundToTwoDecimal() = (this * 100.0).roundToInt() / 100.0

fun Double.currencyString(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "in"))
    val currency = format.format(this)
    if (currency.endsWith(".00", true)) {
        return currency.replace(".00", "", true)
    }
    return currency
}

fun String.fileExtension(): String =
    substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()

fun String.isBetween0And100(): Boolean =
    toIntOrNull()?.let { it in 0..100 } ?: false

fun String.isValidLevel(): Boolean =
    this in setOf("L1", "L2", "L3")

fun String.getFileNameFromUrl(): String {
    var fileName = substringAfterLast("/")

    val queryIndex = fileName.indexOf("?")
    if (queryIndex != -1) {
        fileName = fileName.substring(0, queryIndex)
    }

    return fileName
}