package rxf.server

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and
 * other headers.  This class handles dates as defined by RFC 2616 section
 * 3.3.1 as well as some other common non-standard formats.
 *
 * @author Christopher Brown
 * @author Michael Becke
 * @overhauled Jim Northrup
 */
enum class DateHeaderParser(val format: DateFormat) {
    /**
     * 5.2.14  RFC-822 Date and Time Specification: RFC-822 Section 5
     *
     *
     * The syntax for the date is hereby changed to:
     *
     *
     * date = 1*2DIGIT month 2*4DIGIT
     */
    RFC1123("EEE, dd MMM yyyy HH:mm:ss z"),

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1036 format.
     */
    RFC1036("EEEE, dd-MMM-yy HH:mm:ss z"),

    /**
     * Date format pattern used to parse HTTP date headers in ANSI C
     * `asctime()` format.
     */
    ISO8601("yyyy-MM-dd'T'HH:mm:ssz"), ISOMS("yyyy-MM-dd'T'HH:mm:ss.SSS zzz"), SHORT(DateFormat
        .getDateInstance(DateFormat.SHORT)),
    MED(DateFormat.getDateInstance(DateFormat.MEDIUM)), LONG(
        DateFormat.getDateInstance(DateFormat.LONG)),
    FULL(DateFormat
        .getDateInstance(DateFormat.FULL)),
    ASCTIME("EEE MMM d HH:mm:ss yyyy");

    constructor(fmt: String) : this(SimpleDateFormat(fmt, Locale.getDefault())) {}

    init {
        format.isLenient = true
        //for unit tests we want GMT as predictable.  for other println's we want local tz
        if (BlobAntiPatternObject.isDEBUG_SENDJSON) format.timeZone = TimeZone.getTimeZone("GMT")
    }

    companion object {
        /**
         * Parses the date value using the given date formats.
         *
         * @param dateValue the date value to parse
         * @return the parsed date
         */
        fun parseDate(dateValue: CharSequence): Date? {
            var dateValue = dateValue
            val c = dateValue[0]
            when (c) {
                '\'', '"' -> dateValue = dateValue.subSequence(1, dateValue.length - 1)
                else -> {}
            }
            val source = dateValue.toString()
            for (dateHeaderParser in values()) {
                try {
                    return dateHeaderParser.format.parse(source)
                } catch (e: ParseException) {
                    if (BlobAntiPatternObject.isDEBUG_SENDJSON) {
                        System.err.println(".--" + dateHeaderParser.name + " failed parse: " + source)
                    }
                }
            }
            return null
        }

        fun formatHttpHeaderDate(vararg fdate: Date?): String {
            return RFC1123.format.format(if (fdate.size > 0) fdate[0] else Date())
        }
    }
}