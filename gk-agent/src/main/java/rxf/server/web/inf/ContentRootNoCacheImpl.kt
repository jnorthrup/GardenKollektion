package rxf.server.web.inf

import one.xio.HttpHeaders
import rxf.server.DateHeaderParser
import java.nio.channels.SelectionKey
import java.util.*
import java.util.regex.Pattern

class ContentRootNoCacheImpl : ContentRootImpl() {
    @Throws(Exception::class)
    override fun onWrite(key: SelectionKey) {
        req!!.headerStrings!![HttpHeaders.Expires.header] = DateHeaderParser.RFC1123.format.format(Date())
        super.onWrite(key)
    }

    companion object {
        val NOCACHE_PATTERN = Pattern.compile(".*[.]nocache[.](js|html)$")
    }
}