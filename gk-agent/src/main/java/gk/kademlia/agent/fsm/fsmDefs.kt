@file:Suppress("FunctionName", "KDocMissingDocumentation")

package gk.kademlia.agent.fsm

import gk.kademlia.codec.SmCodec
import vec.macros.*
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.channels.SelectionKey

typealias KeyAction = (SelectionKey) -> FsmNode?

/**like RFC822 smtp message*/
typealias SimpleMessage = Pai2<Vect0r<Pai2<String, String>>, String>
typealias ReifiedMessage = Pair<List<Pair<String, String>>, String>

val SimpleMessage.reify: ReifiedMessage get() = first.toList().map { it.pair } to second
val ReifiedMessage.virtualize: SimpleMessage get() = first α { Tw1n(it) } t2 second

val SimpleMessage.toChunk: Tripl3<String, Int, ByteBuffer>
    get() = SmCodec.send(this).run { Tripl3("BYTE", limit(), this) }
val ByteBuffer.fromChunk: Pai2<Pai2<Int, (Int) -> Pai2<String, String>>, String>?
    get() = SmCodec.recv(also { long }.slice())


/**
IFF Chunk Spec http://www.martinreddy.net/gfx/2d/IFF.txt
```!C
typedef struct {
UBYTE[4] ckID;
UInt	 ckSize;	/* sizeof(ckData) */
UBYTE	 ckData[/* ckSize */];
} Chunk;
```
 */
typealias Chunk = Tripl3<String, Int, ByteBuffer>

fun WriteChunk(chunk: Chunk, next: FsmNode): WriteNode = ByteBuffer.allocate(8).apply {
    val bytes = chunk.first.toByteArray().sliceArray(0..3)
    this.put(bytes)
    repeat(4 - bytes.size) { this.put(' '.code.toByte()) }
    this.putInt(chunk.second)
}.flip().let { buf: ByteBuffer ->
    WriteNode { key: SelectionKey ->
        (key.channel() as ByteChannel).let { chan ->
            chan.write(buf)
            val block = chunk.third.rewind()
            chan.write(block)
            if (block.hasRemaining()) {
                WriteNode {
                    chan.write(block)
                    if (block.hasRemaining()) null
                    else next
                }
            } else next
        }
    }
}


/**
 * reads a chunk.  when the chunk is done the "yeild" is called
 */
@JvmOverloads
fun ReadChunk(yeild: ((Chunk) -> Unit)?, next: FsmNode = Terminal()): ReadNode = run {
    lateinit var typ: String
    var ckSize: Int
    var buf: ByteBuffer = ByteBuffer.allocate(8)
    ReadNode { key ->
        (key.channel() as ByteChannel).let { chan ->
            chan.read(buf)
            if (buf.hasRemaining()) null else {
                buf.flip()
                val byteArray = ByteArray(4)
                buf.get(byteArray)
                typ = String(byteArray, Charsets.UTF_8)
                ckSize = buf.int
                buf = if (ckSize > 2047) ByteBuffer.allocateDirect(ckSize) else ByteBuffer.allocate(ckSize)
                ReadNode {
                    chan.read(buf)
                    if (buf.hasRemaining()) null
                    else {
                        yeild?.invoke(typ t2 ckSize t3 buf.flip())
                        next
                    }
                }
            }
        }
    }
}