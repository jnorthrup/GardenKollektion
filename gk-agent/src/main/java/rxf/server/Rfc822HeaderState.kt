package rxf.server

import one.xio.HttpHeaders
import one.xio.HttpMethod
import one.xio.HttpStatus
import one.xio.MimeType
import vec.macros.Pai2
import vec.macros.t2
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.Charsets.UTF_8


/**
 * this is a utility class to parse a HttpRequest header or
 * $res header according to declared need of
 * header/cookies downstream.
 *
 *
 * much of what is in [BlobAntiPatternObject] can
 * be teased into this class peicemeal.
 *
 *
 * since java string parsing can be expensive and addHeaderInterest
 * can be numerous this class is designed to parse only
 * what is necessary or typical and enable slower dynamic
 * grep operations to suit against a captured
 * [ByteBuffer] as needed (still cheap)
 *
 * preload addHeaderInterest and cookies, send $res
 * and HttpRequest initial onRead for .apply()
 *
 * User: jim
 * Date: 5/19/12
 * Time: 10:00 PM
 */
open class Rfc822HeaderState {

    val _hdrIntrest = AtomicReference<Array<String>>()
    var headerInterest: Array<String>
        inline get() = _hdrIntrest.get()
        inline set(x) = _hdrIntrest.set(x)


    var cookies: Pai2<*, *>? = null


    /**
     * stored buffer from which things are parsed and later grepped.
     *
     *
     * NOT atomic.
     */
    var headerBuf: ByteBuffer? = null
    val _headerStrings = AtomicReference<MutableMap<String?, String>?>()

    /**
     * parsed valued post-[.apply]
     */
    var headerStrings
        get() = _headerStrings.get()
        set(v) = _headerStrings.set(v)


    /**
     * dual purpose HTTP protocol header token found on the first line of a HttpRequest/$res in the first position.
     *
     *
     * contains either the method (HttpRequest) or a the "HTTP/1.1" string (the protocol) on responses.
     *
     *
     * user is responsible for populating this on outbound addHeaderInterest
     */
    private var methodProtocol = AtomicReference<String>()

    /**
     * dual purpose HTTP protocol header token found on the first line of a HttpRequest/$res in the second position
     *
     *
     * contains either the path (HttpRequest) or a the numeric result code on responses.
     *
     *
     * user is responsible for populating this on outbound addHeaderInterest
     */
    private var pathRescode = AtomicReference<String>()


    /**
     * passed in on 0.0.0.0 dispatch to tie the header state to an nio object, to provide a socketchannel handle, and to lookup up the incoming source route
     */
    private var sourceKey = AtomicReference<SelectionKey>()

    /**
     * copy ctor
     *
     *
     * jrn: moved most things to atomic state soas to provide letter-envelope abstraction without
     * undue array[1] members to do the same thing.
     *
     * @param proto the original Rfc822HeaderState
     */
    constructor(proto: Rfc822HeaderState) {
        cookies = proto.cookies
        headerBuf = proto.headerBuf
        headerInterest = proto.headerInterest
        headerStrings = proto.headerStrings
        methodProtocol = proto.methodProtocol
        pathRescode = proto.pathRescode
        //this.PREFIX                =proto.PREFIX                       ;
        protocolStatus = proto.protocolStatus
        sourceKey = proto.sourceKey
        sourceRoute = proto.sourceRoute
    }

    /**
     * default ctor populates [.headerInterest]
     *
     * @param headerInterest keys placed in     [.headerInterest] which will be parsed on [.apply]
     */
    constructor(vararg h: String) {
        headerInterest = h as Array<String>
    }


    /**
     * simple wrapper for HttpRequest setters
     */
    inline val `$req` get() = (this as? HttpRequest) ?: HttpRequest(this)

    /**
     * simple wrapper for HttpRequest setters
     */
    inline val `$res` get() = (this as? HttpResponse) ?: HttpResponse(this)

    open fun <T> `as`(clazz: Class<T>): T = when (clazz) {
        HttpResponse::class.java -> `$res` as T
        HttpRequest::class.java -> `$req` as T
        String::class.java -> this.toString() as T
        ByteBuffer::class.java -> throw UnsupportedOperationException("must promote to as((HttpRequest|HttpResponse)).class first")
        else -> throw UnsupportedOperationException("don't know how to infer " + clazz.canonicalName)
    }

    /**
     * terminates header keys
     */
    fun headerString(hdrEnum: HttpHeaders, s: String): Rfc822HeaderState =
        headerString(hdrEnum.header.trim { it <= ' ' }, s)

    fun headerString(httpHeader: HttpHeaders): String? = headerString(httpHeader.header)

    /**
     * assigns a state parser to a  [SelectionKey] and attempts to grab the source route froom the active socket.
     *
     *
     * this is necessary to look up GeoIpService queries among other things
     *
     * @param key a NIO select key
     * @return self
     * @throws IOException
     */
    @Throws(IOException::class)
    fun sourceKey(key: SelectionKey): Rfc822HeaderState = apply {
        sourceKey.set(key)
        val channel = sourceKey.get().channel() as SocketChannel
        sourceRoute.set(channel.socket().inetAddress)
    }


    /**
     * this is a grep of the full header state to find one or more headers of a given name.
     *
     *
     * performs rewind
     *
     * @param header a header name
     * @return a list of values
     */
    fun getHeadersNamed(header: String?): List<String> {
        val charBuffer = CharBuffer.wrap(header)
        val henc: ByteBuffer = UTF_8.encode(charBuffer)
        var ret = headerExtract(henc)
        val objects: MutableList<String> = ArrayList()
        while (null != ret) {
            objects.add(UTF_8.decode(ret.first).toString())
            ret = ret.second as Pai2<ByteBuffer?, out Pai2<*, *>>
        }
        return objects
    }

    /**
     * this is agrep of the full header state to find one or more headers of a given name.
     *
     *
     * performs rewind
     *
     * @param theHeader a header enum
     * @return a list of values
     */
    fun getHeadersNamed(theHeader: HttpHeaders): List<String> {
        var ret = headerExtract(theHeader.token)
        val objects = ArrayList<String>()
        while (null != ret) {
            objects.add(UTF_8.decode(ret.first).toString())
            ret = ret.second as Pai2<ByteBuffer?, out Pai2<*, *>>
        }
        return objects
    }

    /**
     * string-averse buffer based header extraction
     *
     * @param hdrEnc a header token
     * @return a backwards singley-linked list of pairs.
     */
    fun headerExtract(hdrEnc: ByteBuffer?): Pai2<ByteBuffer?, out Pai2<*, *>>? {
        var hdrEnc = hdrEnc
        hdrEnc = hdrEnc!!.asReadOnlyBuffer().rewind() as ByteBuffer
        val buf = BlobAntiPatternObject.avoidStarvation(headerBuf)
        var ret: Pai2<ByteBuffer?, out Pai2<*, *>>? = null
        val hdrTokenEnd = hdrEnc.limit()
        while (buf!!.hasRemaining()) {
            var begin = buf.position()
            while (buf.hasRemaining() && ':'.code.toByte() != buf.get() && buf.position() - 1 - begin <= hdrTokenEnd);
            val tokenEnd = buf.position() - 1 - begin
            if (tokenEnd == hdrTokenEnd) {
                val sampleHdr = (buf.duplicate().position(begin) as ByteBuffer).slice().limit(hdrTokenEnd) as ByteBuffer
                if (sampleHdr == hdrEnc.rewind()) {
                    //found it for sure
                    begin = buf.position()
                    while (buf.hasRemaining()) {
                        var endl = buf.position()
                        var b: Byte = 0
                        while (buf.hasRemaining() && LF.code.toByte() != buf.get().also { b = it }) {
                            if (!Character.isWhitespace(b.toInt().toChar())) {
                                endl = buf.position()
                            }
                        }
                        buf.mark()
                        if (buf.hasRemaining()) {
                            b = buf.get()
                            if (!Character.isWhitespace(b.toInt())) {
                                val outBuf =
                                    (buf.reset() as ByteBuffer).duplicate().position(begin).limit(endl) as ByteBuffer
                                while (outBuf.hasRemaining() && Character.isWhitespace((outBuf.mark() as ByteBuffer).get()
                                        .toInt())
                                ) {
                                }
                                outBuf.reset() //ltrim()
                                ret = outBuf t2 ret as Pai2<*, *>
                                break
                            }
                        }
                    }
                }
            }
            if (buf.remaining() > hdrTokenEnd + 3) {
                while (buf.hasRemaining() && LF.code.toByte() != buf.get());
            }
        }
        return ret
    }

    /**
     * direction-agnostic RFC822 header state is mapped from a ByteBuffer with tolerance for HTTP method and results in the first line.
     *
     *
     * [.headerInterest] contains a list of addHeaderInterest that will be converted to a [Map] and available via [Rfc822HeaderState.headerStrings]
     *
     *
     * currently this is done inside of [ProtocolMethodDispatch] surrounding [com.google.web.bindery.requestfactory.server.SimpleRequestProcessor.process]
     *
     * @param cursor
     * @return this
     */
    operator fun invoke(cursor: ByteBuffer): Rfc822HeaderState {
        if (!cursor.hasRemaining()) {
            cursor.flip()
        }
        val anchor = cursor.position()
        var slice = cursor.duplicate().slice()
        while (slice.hasRemaining() && SPC.code.toByte() != slice.get()) {
        }
        methodProtocol.set(UTF_8.decode(slice.flip() as ByteBuffer).toString().trim { it <= ' ' })
        while (cursor.hasRemaining() && SPC.code.toByte() != cursor.get()) {
            //method/proto
        }
        slice = cursor.slice()
        while (slice.hasRemaining() && SPC.code.toByte() != slice.get()) {
        }
        pathRescode.set(UTF_8.decode(slice.flip() as ByteBuffer).toString().trim { it <= ' ' })
        while (cursor.hasRemaining() && SPC.code.toByte() != cursor.get()) {
        }
        slice = cursor.slice()
        while (slice.hasRemaining() && LF.code.toByte() != slice.get()) {
        }
        protocolStatus = (UTF_8.decode(slice.flip() as ByteBuffer).toString().trim { it <= ' ' })
        headerBuf = null
        val wantsCookies = null != cookies
        val wantsHeaders = wantsCookies || 0 < headerInterest.size
        headerBuf = moveCaretToDoubleEol(cursor).duplicate().flip() as ByteBuffer
        headerStrings!!.clear()
        if (wantsHeaders) {
            val headerMap =
                HttpHeaders.getHeaders(headerBuf!!.rewind() as ByteBuffer)
            headerStrings = (LinkedHashMap())
            for (o in headerInterest) {
                val o1 = (headerMap[o]) ?: continue
                headerStrings!![o] =
                    UTF_8.decode(headerBuf!!.duplicate().clear().position(o1.first).limit(o1.second) as ByteBuffer)
                        .toString().trim { it <= ' ' }
            }
        }
        return this
    }

    fun headerInterest(vararg replaceInterest: HttpHeaders): Rfc822HeaderState {
        val strings = staticHeaderStrings(*replaceInterest)
        headerInterest = (strings)
        return this
    }

    fun addHeaderInterest(vararg appendInterest: HttpHeaders): Rfc822HeaderState {
        val strings = staticHeaderStrings(*appendInterest)
        return addHeaderInterest(*strings)
    }

    /**
     * Appends to the Set of header keys this parser is interested in mapping to strings.
     *
     *
     * these addHeaderInterest are mapped at cardinality<=1 when  [.apply]  }is called.
     *
     *
     * for cardinality=>1  addHeaderInterest [.getHeadersNamed] is a pure grep over the entire ByteBuffer.
     *
     *
     *
     * @param newInterest
     * @return
     * @see .getHeadersNamed
     * @see .apply
     */
    fun addHeaderInterest(vararg newInterest: String?): Rfc822HeaderState {

        //adds a few more instructions than the blind append but does what was desired
        val theCow = CopyOnWriteArraySet(Arrays.asList(*headerInterest))
        theCow.addAll(Arrays.asList(*newInterest))
        val strings = theCow.toTypedArray()
        Arrays.sort(strings)
        headerInterest = (strings)
        return this
    }

    /**
     * the source route from the active socket.
     *
     *
     * this is necessary to look up  GeoIpService queries among other things
     */
    private var sourceRoute = AtomicReference<InetAddress>()

    /**
     * @return inet4 addr
     * @see .sourceRoute
     */
    fun sourceRoute(): InetAddress {
        return sourceRoute.get()
    }

    /**
     * this holds an inet address which may be inferred diuring [.sourceKey] as well as directly
     *
     * @param sourceRoute an internet ipv4 address
     * @return self
     */
    fun sourceRoute(sourceRoute: InetAddress) = apply {
        this.sourceRoute.set(sourceRoute)
        return this
    }

    /**
     * this is what has been sent to [.apply].
     *
     *
     * care must be taken to avoid [ByteBuffer.compact] during the handling of
     * the dst/cursor found in AsioVisitor code if this is sent in without a clean ByteBuffer.
     *
     * @param headerBuf an immutable  [ByteBuffer]
     * @return self
     */
    fun headerBuf(headerBuf: ByteBuffer?): Rfc822HeaderState {
        this.headerBuf = headerBuf
        return this
    }

    /**
     * @return
     * @see .methodProtocol
     */
    fun methodProtocol(): String? {
        return methodProtocol.get()
    }

    /**
     * @return
     * @see .methodProtocol
     */
    fun methodProtocol(methodProtocol: String): Rfc822HeaderState {
        this.methodProtocol.set(methodProtocol)
        return this
    }

    /**
     * dual purpose HTTP protocol header token found on the first line of a HttpRequest/HttpResponse in the second position
     * contains either the path (HttpRequest) or a the numeric result code on responses.
     * user is responsible for populating this on outbound addHeaderInterest
     *
     * @return
     * @see .pathRescode
     */
    fun pathResCode(): String {
        return pathRescode.get()
    }

    /**
     * @return
     * @see .pathRescode
     */
    fun pathResCode(pathRescode: String): Rfc822HeaderState = apply {
        this.pathRescode.set(pathRescode)
    }


    /**
     * Dual purpose HTTP protocol header token found on the first line of a HttpRequest/$res in the third position.
     *
     *
     * Contains either the protocol (HttpRequest) or a status line message ($res)
     */
    private val _protocolStatus = AtomicReference<String?>()
    var protocolStatus
        get() = _protocolStatus.get()
        set(v) = _protocolStatus.set(v)

    /**
     * writes method, headersStrings, and cookieStrings to a [String] suitable for Response addHeaderInterest
     *
     *
     * populates addHeaderInterest from [.headerStrings]
     *
     *
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    fun asResponseHeaderString(): String {
        var protocol =
            ((if (null == methodProtocol()) HTTP_1_1 else methodProtocol()) + SPC + pathResCode() + SPC + protocolStatus + CRLF)
        for ((key, value) in headerStrings!!) {
            protocol += key + COLONSPC + value + CRLF
        }
        protocol += CRLF
        return protocol
    }

    /**
     * writes method, headersStrings, and cookieStrings to a [ByteBuffer] suitable for Response addHeaderInterest
     *
     *
     * populates addHeaderInterest from [.headerStrings]
     *
     *
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    fun asResponseHeaderByteBuffer(): ByteBuffer {
        val protocol = asResponseHeaderString()
        return ByteBuffer.wrap(protocol.toByteArray(UTF_8))
    }

    /**
     * writes method, headersStrings, and cookieStrings to a [String] suitable for RequestHeaders
     *
     *
     * populates addHeaderInterest from [.headerStrings]
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    fun asRequestHeaderString(): String {
        val builder = StringBuilder()
        builder.append(methodProtocol()).append(SPC).append(pathResCode()).append(SPC)
            .append(if (null == protocolStatus) HTTP_1_1 else protocolStatus).append(CRLF)
        for ((key, value) in headerStrings!!) builder.append(key).append(COLONSPC).append(value).append(CRLF)
        builder.append(CRLF)
        return builder.toString()
    }

    /**
     * writes method, headersStrings, and cookieStrings to a [ByteBuffer] suitable for RequestHeaders
     *
     *
     * populates addHeaderInterest from [.headerStrings]
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    fun asRequestHeaderByteBuffer(): ByteBuffer {
        val protocol = asRequestHeaderString()
        return ByteBuffer.wrap(protocol.toByteArray(UTF_8))
    }

    /**
     * utliity shortcut method to get the parsed value from the [.headerStrings] map
     *
     * @param headerKey name of a header presumed to be parsed during [.apply]
     * @return the parsed value from the [.headerStrings] map
     */
    fun headerString(headerKey: String?): String? {
        return headerStrings!![headerKey]
    }

    /**
     * utility method to strip quotes off of things that makes couchdb choke
     *
     * @param headerKey name of a header
     * @return same string without quotes
     */
    fun dequotedHeader(headerKey: String?): String? {
        val s = headerString(headerKey)
        return BlobAntiPatternObject.dequote(s)
    }

    /**
     * setter for a header (String)
     *
     * @param key headername
     * @param val header value
     * @return
     * @see .headerStrings
     */
    fun headerString(key: String?, `val`: String): Rfc822HeaderState {
        headerStrings!![key] = `val`
        return this
    }

    /**
     * @return the key
     * @see .sourceKey
     */
    fun sourceKey(): SelectionKey {
        return sourceKey.get()
    }

    override fun toString(): String {
        return "Rfc822HeaderState(headerInterest=$headerInterest, cookies=$cookies, sourceRoute=$sourceRoute, headerBuf=$headerBuf, headerStrings=$headerStrings, methodProtocol=$methodProtocol, pathRescode=$pathRescode, protocolStatus=$protocolStatus, sourceKey=$sourceKey)"
    }

    class HttpRequest(proto: Rfc822HeaderState) : Rfc822HeaderState(proto) {
        private var cookieInterest: Array<ByteBuffer?>? = null
        private var parsedCookies: Pai2<Pai2<ByteBuffer, ByteBuffer>, out Pai2<*, *>>? = null

        init {
            val protocol = protocol()
            if (null != protocol && !protocol.startsWith(HTTP)) {
                protocol(null)
            }
        }

        fun method(): String {
            return methodProtocol()!! //To change body of overridden methods use File | Settings | File Templates.
        }

        fun method(method: HttpMethod): HttpRequest {
            return method(method.name) //To change body of overridden methods use File | Settings | File Templates.
        }

        fun method(s: String?): HttpRequest {
            return methodProtocol(s!!) as HttpRequest
        }

        fun path(): String {
            return pathResCode() //To change body of overridden methods use File | Settings | File Templates.
        }

        fun path(path: String?): HttpRequest {
            return pathResCode(path!!) as HttpRequest
        }

        fun protocol(): String? {
            return protocolStatus //To change body of overridden methods use File | Settings | File Templates.
        }

        fun protocol(protocol: String?): HttpRequest {
            return apply {
                protocolStatus = (protocol!!)
            } //To change body of overridden methods use File | Settings | File Templates.
        }

        override fun toString(): String {
            return asRequestHeaderString()
        }

        override fun <T> `as`(clazz: Class<T>): T {
            if (ByteBuffer::class.java == clazz) {
                if (null == protocol()) {
                    protocol(HTTP_1_1)
                }
                return asRequestHeaderByteBuffer() as T
            }
            return super.`as`(clazz)
        }

        /**
         * warning !!! interns your keys. thinking of high tx here.
         *
         * @param keys
         * @return
         */
        fun cookieInterest(vararg keys: String): HttpRequest {
            if (0 == keys.size) { //rare event
                val strings: MutableSet<String> = CopyOnWriteArraySet(Arrays.asList(*headerInterest))
                strings.remove(HttpHeaders.Cookie.header)
                headerInterest = strings.toTypedArray()
                cookieInterest = null
            } else {
                addHeaderInterest(HttpHeaders.Cookie)
                cookieInterest = arrayOfNulls(keys.size)
                for (i in keys.indices) {
                    val s = keys[i]
                    cookieInterest!![i] = ByteBuffer.wrap(s.intern().toByteArray(UTF_8))
                }
            }
            return this
        }

        /**
         * @return slist of cookie pairs
         */
        fun parsedCookies(): Pai2<Pai2<ByteBuffer, ByteBuffer>, out Pai2<*, *>>? {
            if (null != parsedCookies) return parsedCookies else {
                cookieInterest = if (null == cookieInterest) EMPTY_BBAR else cookieInterest
                var p1 = headerExtract(HttpHeaders.Cookie.token)
                parsedCookies = null
                while (null != p1) {
                    val p2: Pai2<Pai2<ByteBuffer, ByteBuffer>, out Pai2<*, *>> =
                        CookieRfc6265Util.parseCookie(p1.second as ByteBuffer) as Pai2<Pai2<ByteBuffer, ByteBuffer>, out Pai2<*, *>>
                    if (parsedCookies != null) { //seek to null of prev.
                        var p3 = parsedCookies
                        var p4 = parsedCookies
                        while (p3 != null) p3 =
                            p3.also { p4 = it }.second as Pai2<Pai2<ByteBuffer, ByteBuffer>, out Pai2<*, *>>
                        parsedCookies = Pai2<Pai2<ByteBuffer, ByteBuffer>, Pai2<*, *>>(p4!!.first, p2)
                    } else {
                        parsedCookies = p2
                    }
                    p1 = p1.second as Pai2<ByteBuffer?, out Pai2<*, *>>
                }
            }
            return parsedCookies
        }

        /**
         * warning: interns the keys.  make them count!
         *
         * @param keys optional list of keys , default is full cookieInterest
         * @return stringy cookie map
         */
        fun getCookies(vararg keys: String): Map<String, String> {
            val k: Array<ByteBuffer?>
            if (0 >= keys.size) {
                k = cookieInterest!!
            } else {
                k = arrayOfNulls(keys.size)
                for (i in keys.indices) {
                    val key = keys[i]
                    k[i] = ByteBuffer.wrap(key.intern().toByteArray(UTF_8)) as ByteBuffer
                }
            }
            val ret: MutableMap<String, String> = TreeMap()
            var pair = parsedCookies()
            val kl: MutableList<ByteBuffer?> = LinkedList(Arrays.asList(*k))
            while (null != pair && !kl.isEmpty()) {
                val a1 = pair.first
                val ckey = a1.first
                val ki = kl.listIterator()
                while (ki.hasNext()) {
                    val interestKey = ki.next()
                    if (interestKey == ckey) {
                        ret[UTF_8.decode(interestKey).toString().intern()] =
                            UTF_8.decode(a1.second).toString()
                        ki.remove()
                        break
                    }
                }
                pair = pair.second as Pai2<Pai2<ByteBuffer, ByteBuffer>, out Pai2<*, *>>
            }
            return ret
        }

        /**
         * warning!  interns the key.  make it count!
         *
         * @param key
         * @return cookie value
         */
        fun getCookie(key: String): String? {
            val k = ByteBuffer.wrap(key.intern().toByteArray(UTF_8)).mark() as ByteBuffer
            var pair = parsedCookies()
            while (null != pair) {
                val a1 = pair.first
                val a = a1.first
                if (a == k) {
                    return UTF_8.decode(BlobAntiPatternObject.avoidStarvation(MimeType.b as ByteBuffer))
                        .toString()
                }
                pair = pair.second as Pai2<Pai2<ByteBuffer, ByteBuffer>, out Pai2<*, *>>
            }
            return null
        }

        companion object {
            val EMPTY_BBAR = arrayOfNulls<ByteBuffer>(0)
        }
    }

    class HttpResponse(proto: Rfc822HeaderState) : Rfc822HeaderState(proto) {
        init {
            val protocol = protocol()
            if (null != protocol && !protocol.startsWith(HTTP)) {
                protocol(null)
            }
        }

        fun statusEnum(): HttpStatus? {
            try {
                return HttpStatus.valueOf('$'.toString() + resCode())
            } catch (e: Exception) {
                e.printStackTrace() //todo: verify for a purpose
            }
            return null
        }

        override fun toString(): String {
            return asResponseHeaderString()
        }

        fun protocol(): String? {
            return methodProtocol()
        }

        fun resCode(): String {
            return pathResCode()
        }

        fun status(): String {
            return protocolStatus!!
        }

        fun protocol(protocol: String?): HttpResponse {
            return methodProtocol(protocol!!) as HttpResponse
        }

        fun resCode(res: String?): HttpResponse {
            return pathResCode(res!!) as HttpResponse
        }

        fun resCode(resCode: HttpStatus): HttpResponse {
            return pathResCode(resCode.name.substring(1)) as HttpResponse
        }

        fun status(status: String?): HttpResponse = apply {
            protocolStatus = (status!!)
        }

        /**
         * convenience method ^2 -- sets rescode and status captions from same enum
         *
         * @param httpStatus
         * @return
         */
        fun status(httpStatus: HttpStatus): HttpResponse = apply {
            protocolStatus = (httpStatus.caption)
        }.resCode(httpStatus)

        override fun <T> `as`(clazz: Class<T>): T {
            if (ByteBuffer::class.java == clazz) {
                if (null == protocol()) {
                    protocol(HTTP_1_1)
                }
                return asResponseHeaderByteBuffer() as T
            }
            return super.`as`(clazz) //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    companion object {
        const val HTTP = "HTTP"
        const val HTTP_1_1 = HTTP + "/1.1"
        const val SPC = ' '
        const val CR = '\r'
        const val LF = '\n'
        const val CRLF = "" + CR + LF
        private const val COLON = ':'
        const val COLONSPC = "" + COLON + SPC
        fun staticHeaderStrings(vararg replaceInterest: HttpHeaders) =
            Array(replaceInterest.size) { replaceInterest[it].header }
    }

    fun moveCaretToDoubleEol(buffer: ByteBuffer): ByteBuffer {
        var distance: Int
        var eol = buffer.position()
        do {
            val prev = eol
            while (buffer.hasRemaining() && LF.code.toByte() != buffer.get());
            eol = buffer.position()
            distance = Math.abs(eol - prev)
            if (2 == distance && CR.code.toByte() == buffer[eol - 2]) break
        } while (buffer.hasRemaining() && 1 < distance)
        return buffer
    }
}
