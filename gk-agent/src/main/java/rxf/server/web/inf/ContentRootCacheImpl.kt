package rxf.server.web.inf

import one.xio.HttpHeaders
import rxf.server.DateHeaderParser
import java.nio.channels.SelectionKey
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ContentRootCacheImpl : ContentRootImpl() {
    @Throws(Exception::class)
    override fun onWrite(key: SelectionKey) {
        req!!.headerStrings!![HttpHeaders.Expires.header] =
            DateHeaderParser.RFC1123.format.format(Date(Date().time + YEAR))
        super.onWrite(key)
    }

    companion object {
        val YEAR = TimeUnit.MILLISECONDS.convert(365, TimeUnit.DAYS)
        val CACHE_PATTERN = Pattern.compile(".*(clear.cache.gif|[0-9A-F]{32}[.]cache[.]html)$")
    }
}