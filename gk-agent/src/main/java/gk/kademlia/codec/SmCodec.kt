package gk.kademlia.codec

import gk.kademlia.agent.fsm.ReifiedMessage
import gk.kademlia.agent.fsm.SimpleMessage
import gk.kademlia.agent.fsm.deserialize
import gk.kademlia.agent.fsm.serialize
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vec.macros.`➤`
import vec.macros.t2
import vec.macros.toVect0r
import java.nio.ByteBuffer

//import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack

object SmCodec : Codec<SimpleMessage, ByteBuffer> {
    override fun send(event: SimpleMessage): ByteBuffer = event.let { (hdr, body) ->
        val s = hdr.`➤`.map { (a, c) -> "$a: $c" }.joinToString("\n", postfix = "\n")
        ByteBuffer.wrap(s.toByteArray(Charsets.UTF_8) + body.toByteArray(Charsets.UTF_8))
    }

    override fun recv(ser: ByteBuffer): SimpleMessage? =
        ser.asCharBuffer().toString().split("\n\n".toRegex(), 2).takeUnless { it.size != 2 }?.let { (hdrs, bod) ->
            hdrs.split("\n").map { it.split(":\\s?".toRegex(), 2).let { (a, b) -> a t2 b } }.toVect0r() t2 bod
        }
}

object SmJson : Codec<SimpleMessage, ByteBuffer> {
    override fun send(event: SimpleMessage): ByteBuffer? {
        val serializer = event.serialize
        return ByteBuffer.wrap(Json.Default.encodeToString(serializer).encodeToByteArray())
    }

    override fun recv(ser: ByteBuffer): SimpleMessage {
        val byteArray = ByteArray(ser.remaining())
        ser.put(byteArray)
        val string = String(byteArray, Charsets.UTF_8)
        val rm = Json.decodeFromString<ReifiedMessage>(string)
        return rm.deserialize
    }
}



