package gk.kademlia.codec

import gk.kademlia.agent.fsm.SimpleMessage
import vec.macros.`➤`
import vec.macros.t2
import vec.macros.toVect0r
import java.nio.ByteBuffer

class SmCodec : Codec<SimpleMessage, ByteBuffer> {
    override fun send(event: SimpleMessage): ByteBuffer = event.let { (hdr, body) ->
        val s = hdr.`➤`.map { (a, c) -> "$a: $c" }.joinToString("\n") + "\n"

        ByteBuffer.wrap(s.toByteArray(Charsets.UTF_8) + body.toByteArray(Charsets.UTF_8))
    }

    override fun recv(ser: ByteBuffer): SimpleMessage = ser.toString().split("\n\n".toRegex(), 2).let { (hdrs, bod) ->
        hdrs.split("\n").map { it.split(":\\s?".toRegex(), 2).let { (a, b) -> a t2 b } }.toVect0r() t2 bod
    }
}