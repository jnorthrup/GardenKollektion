package gk.kademlia.agent

import vec.macros.*
import java.nio.ByteBuffer

interface Codec<Evt, Serde> {
    fun send(event: Evt): Serde
    fun recv(ser: Serde): Evt
}

/**like RFC822 smtp message*/
typealias SimpleMessage = Pai2<Vect02<String, String>, String>

class SmCodec : Codec<SimpleMessage, ByteBuffer> {
    override fun send(event: SimpleMessage): ByteBuffer = event.let { (hdr, body) ->
        val s = hdr.`➤`.map { (a, c) -> "$a: $c" }.joinToString("\n") + "\n"

        ByteBuffer.wrap(s.toByteArray(Charsets.UTF_8) + body.toByteArray(Charsets.UTF_8))
    }

    override fun recv(ser: ByteBuffer): SimpleMessage = ser.toString().split("\n\n".toRegex(), 2).let { (hdrs, bod) ->
        hdrs.split("\n").map { it.split(":\\s?".toRegex(), 2).let { (a, b) -> a t2 b } }.toVect0r() t2 bod
    }
}

