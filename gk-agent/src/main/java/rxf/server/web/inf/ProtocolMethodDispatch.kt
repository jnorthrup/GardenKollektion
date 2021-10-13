package rxf.server.web.inf

import one.xio.AsioVisitor
import one.xio.HttpMethod
import rxf.server.*
import rxf.server.web.inf.ContentRootCacheImpl
import rxf.server.web.inf.ContentRootImpl
import rxf.server.web.inf.ContentRootNoCacheImpl
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.regex.Pattern

/**
 * this class holds a protocol namespace to dispatch requests
 *
 *
 * [rxf.server.CouchNamespace.NAMESPACE] is a  map of http methods each containing an ordered map of regexes tested in order of
 * map insertion.
 *
 *
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
class ProtocolMethodDispatch : AsioVisitor.Impl() {
    @Throws(IOException::class)
    override fun onAccept(key: SelectionKey) {
        val channel = key.channel() as ServerSocketChannel
        val accept = channel.accept()
        accept.configureBlocking(false)
        HttpMethod.Companion.enqueue(accept, SelectionKey.OP_READ, this)
    }

    @Throws(Exception::class)
    override fun onRead(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        val cursor = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize())
        val read = channel.read(cursor)
        if (-1 == read) {
            (key.channel() as SocketChannel).socket().close() //cancel();
            return
        }
        var method: HttpMethod? = null
        var httpRequest: Rfc822HeaderState.HttpRequest? = null
        try {
            //find the method to dispatch
            val state = Rfc822HeaderState().apply(cursor.flip() as ByteBuffer)
            httpRequest = state!!.`$req`()
            if (BlobAntiPatternObject.DEBUG_SENDJSON) {
                System.err.println(BlobAntiPatternObject.deepToString<CharBuffer>(HttpMethod.Companion.UTF8.decode(
                    httpRequest
                        .headerBuf()!!.duplicate().rewind() as ByteBuffer)))
            }
            val method1 = httpRequest.method()
            method = HttpMethod.valueOf(method1!!)
        } catch (e: Exception) {
        }
        if (null == method) {
            (key.channel() as SocketChannel).socket().close() //cancel();
            return
        }
        val entries: Set<Map.Entry<Pattern, Class<out AsioVisitor.Impl>>> =
            CouchNamespace.Companion.NAMESPACE.get(method)!!.entries
        val path = httpRequest!!.path()
        for ((key1, value) in entries) {
            val matcher = key1.matcher(path)
            if (matcher.find()) {
                if (BlobAntiPatternObject.DEBUG_SENDJSON) {
                    System.err.println("+?+?+? using $matcher")
                }
                val impl: AsioVisitor.Impl
                impl = value.newInstance()
                val a = arrayOf(impl, httpRequest, cursor)
                key.attach(a)
                if (PreRead::class.java.isAssignableFrom(value)) impl.onRead(key)
                key.selector().wakeup()
                return
            }
        }
        System.err.println(BlobAntiPatternObject.deepToString<Any>("!!!1!1!!", "404", path, "using",
            CouchNamespace.Companion.NAMESPACE))
    }

    companion object {
        val NONCE = ByteBuffer.allocateDirect(0)

        /**
         * the PUT protocol handlers, only static for the sake of javadocs
         */
        var POSTmap: Map<Pattern, Class<out AsioVisitor.Impl>> = LinkedHashMap()

        /**
         * the GET protocol handlers, only static for the sake of javadocs
         */
        var GETmap: MutableMap<Pattern, Class<out AsioVisitor.Impl>> = LinkedHashMap()

        init {
            CouchNamespace.Companion.NAMESPACE.put(HttpMethod.POST, POSTmap)
            CouchNamespace.Companion.NAMESPACE.put(HttpMethod.GET, GETmap)
            /**
             * for gwt requestfactory done via POST.
             *
             * TODO: rf GET from query parameters
             */
//    POSTmap.put(Pattern.compile("^/gwtRequest"), GwtRequestFactoryVisitor.class);
            /**
             * any url begining with /i is a proxied $req to couchdb but only permits image/ * and text/ *
             */
            val passthroughExpr = Pattern.compile("^/i(/.*)$")
            GETmap[rxf.server.web.inf.passthroughExpr] = HttpProxyImpl::class.java
            /**
             * general purpose httpd static content server that recognizes .gz and other compression suffixes when convenient
             *
             * any random config mechanism with a default will suffice here to define the content root.
             *
             * widest regex last intentionally
             * system proprty: {value #RXF_SERVER_CONTENT_ROOT}
             */
            GETmap[ContentRootCacheImpl.Companion.CACHE_PATTERN] =
                ContentRootCacheImpl::class.java
            GETmap[ContentRootNoCacheImpl.Companion.NOCACHE_PATTERN] =
                ContentRootNoCacheImpl::class.java
            GETmap[Pattern.compile(".*")] = ContentRootImpl::class.java
        }
    }
}