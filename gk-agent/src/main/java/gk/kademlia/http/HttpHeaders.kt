package gk.kademlia.http

import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.CharBuffer

/**
 * User: jim
 * Date: May 11, 2009
 * Time: 11:37:55 PM
 */
enum class HttpHeaders {
    `Content$2dLength`, `Content$2dEncoding`, Host, Accept, `User$2dAgent`;

    val header = CharBuffer.wrap(URLDecoder.decode(name.replace('$', '%')))
    val token: ByteBuffer = Charsets.UTF_8.encode(header)
    var tokenLen = token.limit()
    fun recognize(buffer: ByteBuffer): Boolean {
        val i = buffer.position()
        val byte  = buffer.get(tokenLen + i)
        if (byte.toInt() == ':'.code) {
            var j: Int
            j = 0
            while (j < tokenLen && token[j] == buffer[i + j]) {
                j++
            }
            return tokenLen == j
        }
        return false
    }
}