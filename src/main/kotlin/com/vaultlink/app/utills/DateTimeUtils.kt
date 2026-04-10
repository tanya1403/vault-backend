package com.vaultlink.app.utills

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


enum class DateFilter(val value:String,val dayCode:Int) {
    LAST_7_DAYS("LAST_7_DAYS", -7),
    LAST_28_DAYS("LAST_28_DAYS", -28),
    LAST_MONTH("LAST_MONTH", 0),
    LAST_3_MONTHS("LAST_3_MONTHS", 0),
    LAST_6_MONTHS("LAST_6_MONTHS", 0),
    CUSTOM("CUSTOM", 0);

    companion object {
        fun getDateFilterEnum(value: String): DateFilter {
            return values().first { it.value == value }
        }
    }

    open fun getStartDatetime(): String? {
        return when (this) {
            LAST_7_DAYS -> DateTimeUtils.getDateTimeByAddingDays(dayCode, DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST) + " 00:00:00"
            LAST_28_DAYS -> DateTimeUtils.getDateTimeByAddingDays(dayCode, DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST) + " 00:00:00"
            LAST_MONTH -> DateTimeUtils.getLastNMonthFirstDate(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST,1) + " 00:00:00"
            LAST_3_MONTHS -> DateTimeUtils.getLastNMonthFirstDate(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST,3) + " 00:00:00"
            LAST_6_MONTHS -> DateTimeUtils.getLastNMonthFirstDate(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST,6) + " 00:00:00"
            else -> null
        }
    }

    open fun getEndDatetime(): String? {
        return when (this) {
            LAST_7_DAYS -> DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST) + " 23:59:59"
            LAST_28_DAYS -> DateTimeUtils.getDateTime(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST) + " 23:59:59"
            LAST_MONTH -> DateTimeUtils.getLastNMonthLastDate(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST,1) + " 23:59:59"
            LAST_3_MONTHS -> DateTimeUtils.getLastNMonthLastDate(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST,3) + " 23:59:59"
            LAST_6_MONTHS -> DateTimeUtils.getLastNMonthLastDate(DateTimeFormat.yyyy_MM_dd, DateTimeZone.IST,6) + " 23:59:59"
            else -> null
        }
    }
}

enum class DateTimeZone(val value: String) {
    IST("IST"), GMT("GMT");
}

enum class DateTimeFormat(val value: String) {
    yyyy_MM_dd_T_HH_mm_ss_Z("yyyy-MM-dd'T'HH:mm:ssZ"),
    yyyy_MM_dd_HH_mm_ss_SSS_GMT_z("yyyy-MM-dd HH:mm:ss.SSS z"),
    yyyy_MM_dd_T_HH_mm_ss_SSSZ("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
    yyyy_MM_dd_T_HH_mm_ss_XXX("yyyy-MM-dd HH:mm:ssXXX"),
    yyyy_MM_dd("yyyy-MM-dd"),
    yyyy_MM("yyyy-MM"),
    MMM_dd_yyyy("MMM dd, yyyy"),
    E("E"),
    hh_mm("hh:mm"),
    HH_mm("HH:mm"),
    MMM("MMM"),
    MM("MM"),
    dd("dd"),
    yyyy("yyyy"),
    EEE_MMM_d("EEE, MMM d"),
    d_MMM("d MMM"),
    ddMMM("ddMMM"),
    MMM_d("MMM d"),
    MMM_d_hh_mm_a("MMM d, hh:mm a"),
    MMM_d_yyyy("MMM d, yyyy"),
    MMM_yyyy("MMM-yyyy"),
    hh_mm_a("hh:mm a"),
    MMM_dd_yyyy_h_mm_a("MMM dd, yyyy h:mm a"),
    yyyy_MM_dd_HH_mm("yyyy-MM-dd HH:mm"),
    h_mm_a("h:mm a"),
    dd_MM_yyyy("dd-MM-yyyy"),
    dd_MM_yyyy_slash("dd/MM/yyyy"),
    dd_M_yyyy_slash("dd/M/yyyy"),
    dd_MM_yyyy_HH_mm_ss_slash("dd/MM/yyyy HH:mm:ss"),
    yyyy_MM_dd_HH_mm_ss("yyyy-MM-dd HH:mm:ss"),
    yyyy_MM_dd_T_HH_mm_ssXXX("yyyy-MM-dd'T'HH:mm:ssXXX"),
    dd_MM_yyyy_HH_mm_ss("dd-MM-yyyy HH:mm:ss"),
    dd_MM_yyyy_hh_mm_a("dd-MM-yyyy hh:mm a"),
    d_EEE_yyyy("d MMM, yyyy"),
    d_MMM_yyyy_hh_mm_a("d MMM, yyyy hh:mm a"),
    ddMMyyyy("ddMMyyyy"),
    d_EEE_yyyy_hh_mm_a("d MMM, yyyy hh:mm a"),
    yyyy_MM_dd_T_HH_mm_ss("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
    CC_YY_MM_DD("yyyyMMdd"),
    HH_MM_SS("hhmmss"),
    HH_MM_SS_colon("hh:mm:ss")

}

object DateTimeUtils {

    fun getCurrentDateTimeInIST(): String = getCurrentDateTimeInIST(DateTimeFormat.yyyy_MM_dd_HH_mm_ss)

    fun getCurrentDate(): String = getCurrentDateTimeInIST(DateTimeFormat.yyyy_MM_dd)

    fun getCurrentEpochSecond(): Long = Instant.now().epochSecond

    fun getCurrentDateTimeInIST(dateTimeFormat: DateTimeFormat): String {
        val dt = Date()
        val sdf = SimpleDateFormat(dateTimeFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(DateTimeZone.IST.value)
        return sdf.format(dt)
    }

    fun getCurrentTimeInISTMillis(): Long {
        val istZoneId = ZoneId.of("Asia/Kolkata")
        val instant = ZonedDateTime.now(istZoneId).toInstant()
        return instant.toEpochMilli()
    }


    fun getDateTime(dFormat: DateTimeFormat, zone: DateTimeZone): String? {
        val dt = Date()
        val sdf = SimpleDateFormat(dFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(zone.value)
        return sdf.format(dt)
    }

    fun getDateTimeByAddingDays(day: Int, dFormat: DateTimeFormat, zone: DateTimeZone): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, day)
        val dt = cal.time
        val sdf = SimpleDateFormat(dFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(zone.value)
        return sdf.format(dt)
    }

    fun getLastNMonthFirstDate(dFormat: DateTimeFormat, zone: DateTimeZone, n:Int): String? {
        val aCalendar = Calendar.getInstance()
        aCalendar.add(Calendar.MONTH, -n)
        aCalendar[Calendar.DATE] = 1
        val firstDateOfPreviousMonth = aCalendar.time
        val sdf = SimpleDateFormat(dFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(zone.value)
        return sdf.format(firstDateOfPreviousMonth)
    }



    fun getLastNMonthLastDate(dFormat: DateTimeFormat, zone: DateTimeZone,n:Int): String? {
        val aCalendar = Calendar.getInstance()
        aCalendar.add(Calendar.MONTH, -1)
        aCalendar[Calendar.DATE] = 1

        val sdf = SimpleDateFormat(dFormat.value)

        aCalendar[Calendar.DATE] = aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val lastDateOfPreviousMonth = aCalendar.time
        val sdf1 = SimpleDateFormat(dFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(zone.value)
        return sdf1.format(lastDateOfPreviousMonth)
    }

    fun isGreaterThanCurrentTime(
            futureDatetime: String?,
            dateTimeFormat: DateTimeFormat = DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
    ): Boolean {

        val sdf = SimpleDateFormat(dateTimeFormat.value)
        val currentDateTime = getCurrentDateTimeInIST(dateTimeFormat)

        val currentDate = sdf.parse(currentDateTime)
        val futureDate = sdf.parse(futureDatetime)

        return futureDate.after(currentDate)

    }

    fun getDateDifferenceInMinutes(
            startDate: String?,
            endDate: String?,
    ): Int {
        val sdf = SimpleDateFormat(DateTimeFormat.yyyy_MM_dd_HH_mm_ss.value)
        val differenceInTime = sdf.parse(endDate).time - sdf.parse(startDate).time
        val diffInMinutes = differenceInTime / 60000
        return diffInMinutes.toInt()
    }

    fun getDateDifferenceInDays(
        startDate: String?,
        endDate: String?,
    ): Int {
        val sdf = SimpleDateFormat(DateTimeFormat.yyyy_MM_dd_HH_mm_ss.value)
        val differenceInTime = sdf.parse(endDate).time - sdf.parse(startDate).time
        val differenceInDays = differenceInTime / 86400000
        return differenceInDays.toInt()
    }

    fun getAge(dobString: String): Int {
        var date: Date? = null
        val sdf = SimpleDateFormat(DateTimeFormat.yyyy_MM_dd.value)
        try {
            date = sdf.parse(dobString)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        if (date == null) return 0
        val dob = Calendar.getInstance()
        val today = Calendar.getInstance()
        dob.time = date
        val year = dob[Calendar.YEAR]
        val month = dob[Calendar.MONTH]
        val day = dob[Calendar.DAY_OF_MONTH]
        dob[year, month + 1] = day
        var age = today[Calendar.YEAR] - dob[Calendar.YEAR]
        if (today[Calendar.DAY_OF_YEAR] < dob[Calendar.DAY_OF_YEAR]) {
            age--
        }
        return age
    }

    fun getAgeForFormat(dobString: String): Int {

        return try {

            val sdf = SimpleDateFormat(DateTimeFormat.dd_MM_yyyy_slash.value, Locale.getDefault())
            val date = sdf.parse(dobString) ?: return 0

            val dob = Calendar.getInstance().apply { time = date }
            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age

        } catch (e: ParseException) {
            e.printStackTrace()
            0
        }
    }



    fun getDateTimeByAddingHours(
            hour: Int,
            dFormat: DateTimeFormat = DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
            zone: DateTimeZone = DateTimeZone.IST,
    ): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, hour)
        val dt = cal.time
        val sdf = SimpleDateFormat(dFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(zone.value)
        return sdf.format(dt)
    }

    fun getDateTimeByAddingMins(
        mins: Int,
        dFormat: DateTimeFormat = DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
        zone: DateTimeZone = DateTimeZone.IST,
    ): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, mins)
        val dt = cal.time
        val sdf = SimpleDateFormat(dFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(zone.value)
        return sdf.format(dt)
    }

    fun getDateTimeFromEpoch(epoch: Long, dFormat: DateTimeFormat, zone: DateTimeZone): String? {
        val dt = Date(epoch * 1000)
        val sdf = SimpleDateFormat(dFormat.value)
        sdf.timeZone = TimeZone.getTimeZone(zone.value)
        return sdf.format(dt)
    }

    @Throws(ParseException::class)
    fun getDateFromDateTimeString(
            dateString: String,
            format: DateTimeFormat = DateTimeFormat.yyyy_MM_dd_HH_mm_ss,
    ): Date {
        return SimpleDateFormat(format.value).parse(dateString)
    }

    @Throws(ParseException::class)
    fun getDateForSalesforce(datetime: String?): String? {

        val date = SimpleDateFormat(DateTimeFormat.yyyy_MM_dd_HH_mm_ss.value).parse(datetime)
        val sdf = SimpleDateFormat(DateTimeFormat.yyyy_MM_dd_T_HH_mm_ss_SSSZ.value)
        sdf.timeZone = TimeZone.getTimeZone(DateTimeZone.IST.value)

        return sdf.format(date)

    }

    fun getDateForSF(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

        return ZonedDateTime
            .now(ZoneId.of("Asia/Kolkata"))
            .format(formatter)
    }

    @Throws(ParseException::class)
    fun getStringFromDateTimeString(
        datetime: String,
        inputFormat: DateTimeFormat,
        outputFormat: DateTimeFormat
    ): String? {
        if (datetime == NA) return datetime
        val date = SimpleDateFormat(inputFormat.value).parse(datetime)
        val sdf = SimpleDateFormat(outputFormat.value)
        return sdf.format(date)
    }

    fun isValid(dateStr: String?, inputFormat: DateTimeFormat): Boolean {
        val sdf = SimpleDateFormat(inputFormat.value)
        sdf.isLenient = false
        try {
            sdf.parse(dateStr)
        } catch (e: ParseException) {
            return false
        }
        return true
    }

    fun convertUTCtoIST(
        utcDateTime: String,
        inputFormat: DateTimeFormat = DateTimeFormat.yyyy_MM_dd_T_HH_mm_ssXXX,
        outputFormat: DateTimeFormat = DateTimeFormat.yyyy_MM_dd_HH_mm_ss
    ): String {

        val sdfUTC = SimpleDateFormat(inputFormat.value)
        sdfUTC.timeZone = TimeZone.getTimeZone("UTC")

        val date = sdfUTC.parse(utcDateTime)

        val sdfIST = SimpleDateFormat(outputFormat.value)
        sdfIST.timeZone = TimeZone.getTimeZone("Asia/Kolkata")

        return sdfIST.format(date)
    }

    fun formatExperianDate(date: String?): String {

        if (date.isInvalid()) return "-"

        return try {

            val inputFormatter = SimpleDateFormat(DateTimeFormat.CC_YY_MM_DD.value)

            val outputFormatter = SimpleDateFormat(DateTimeFormat.dd_MM_yyyy_slash.value)

            val date = inputFormatter.parse(date)
            outputFormatter.format(date)

        } catch (e: Exception){
            throw IllegalArgumentException("Invalid date format: $date")
        }
    }

    fun formatExperianTime(time: String) : String {
        return try {
            val inputFormatter = SimpleDateFormat(DateTimeFormat.HH_MM_SS.value)
            val outputFormatter = SimpleDateFormat(DateTimeFormat.HH_MM_SS_colon.value)

            val time = inputFormatter.parse(time)
            outputFormatter.format(time)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid time format: $time")
        }
    }

}
