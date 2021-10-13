package rxf.server;

import one.xio.HttpMethod;
import rxf.server.driver.RxfBootstrap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static one.xio.HttpHeaders.*;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.CouchNamespace.COUCH_DEFAULT_ORGNAME;
import static rxf.server.RelaxFactoryServerImpl.wheresWaldo;

/**
 * <a href='http://www.antipatterns.com/briefing/sld024.htm'> Blob Anti Pattern </a>
 * used here as a pattern to centralize the antipatterns
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {

    public static final boolean RXF_CACHED_THREADPOOL = "true".equals(RxfBootstrap.getVar("RXF_CACHED_THREADPOOL", "false"));
    public static final int CONNECTION_POOL_SIZE = Integer.parseInt(RxfBootstrap.getVar("RXF_CONNECTION_POOL_SIZE", "20"));
    public static final byte[] CE_TERMINAL = "\n0\r\n\r\n".getBytes(UTF8);
    //"premature optimization" s/mature/view/
    public static final String[] STATIC_VF_HEADERS = Rfc822HeaderState.staticHeaderStrings(ETag, Content$2dLength, Transfer$2dEncoding);
    public static final String[] STATIC_JSON_SEND_HEADERS = Rfc822HeaderState.staticHeaderStrings(ETag, Content$2dLength, Content$2dEncoding);
    public static final String[] STATIC_CONTENT_LENGTH_ARR = Rfc822HeaderState.staticHeaderStrings(Content$2dLength);
    public static final byte[] HEADER_TERMINATOR = "\r\n\r\n".getBytes(UTF8);
    public static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);
    public static final int REALTIME_CUTOFF = Integer.parseInt(RxfBootstrap.getVar("RXF_REALTIME_CUTOFF", "3"));
    public static final String PCOUNT = "-0xdeadbeef.2";
    public static final String GENERATED_METHODS = "/*generated methods vsd78vs0fd078fv0sa78*/";
    public static final String IFACE_FIRE_TARGETS = "/*fire interface ijnoifnj453oijnfiojn h*/";
    public static final String FIRE_METHODS = "/*embedded fire terminals j63l4k56jn4k3jn5l63l456jn*/";
    private static final LinkedBlockingDeque<SocketChannel> couchConnections = new LinkedBlockingDeque<>(CONNECTION_POOL_SIZE);
    public static boolean DEBUG_SENDJSON = System.getenv().containsKey("DEBUG_SENDJSON");
    public static final TimeUnit REALTIME_UNIT = TimeUnit.valueOf(RxfBootstrap.getVar("RXF_REALTIME_UNIT", isDEBUG_SENDJSON() ? TimeUnit.HOURS.name() : TimeUnit.SECONDS.name()));
    public static InetAddress LOOPBACK;
    public static int receiveBufferSize;
    public static int sendBufferSize;
    public static InetSocketAddress COUCHADDR;
    public static ExecutorService EXECUTOR_SERVICE = RXF_CACHED_THREADPOOL ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 3);

    static {

        String rxfcouchprefix = RxfBootstrap.getVar("RXF_COUCH_PREFIX", "http://localhost:5984");
        try {
            URI uri = new URI(rxfcouchprefix);
            int port = uri.getPort();
            port = -1 != port ? port : 80;
            setCOUCHADDR(new InetSocketAddress(uri.getHost(), port));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static SocketChannel createCouchConnection() {
        while (!HttpMethod.killswitch) {
            SocketChannel poll = couchConnections.poll();
            if (null != poll) {
                // If there was at least one entry, try to use that
                // Note that we check both connected&&open, its possible to be connected but not open, at least in 1.7.0_45
                if (poll.isConnected() && poll.isOpen()) {
                    return poll;
                }
                //non null entry, but invalid, continue in loop to grab the next...
            } else {
                // no recycled connections available for reuse, make a new one
                try {
                    SocketChannel channel = SocketChannel.open(getCOUCHADDR());
                    channel.configureBlocking(false);
                    return channel;
                } catch (Exception e) {
                    // if something went wrong in the process of creating the connection, continue in loop...
                    e.printStackTrace();
                }
            }
        }
        // killswitch, return null
        return null;
    }

    public static void recycleChannel(SocketChannel channel) {
        try {
            // Note that we check both connected&&open, its possible to be connected but not open, at least in 1.7.0_45
            if (!channel.isConnected() || !channel.isOpen() || !couchConnections.offerLast(channel)) {
                channel.close();
            }
        } catch (IOException e) {
            //eat all exceptions, recycle should be brain-dead easy
            e.printStackTrace();
        }
    }

    public static <T> String deepToString(T... d) {
        return Arrays.deepToString(d) + wheresWaldo();
    }

    public static <T> String arrToString(T... d) {
        return Arrays.deepToString(d);
    }

    public static int getReceiveBufferSize() {
        switch (receiveBufferSize) {
            case 0:
                try {
                    SocketChannel couchConnection = createCouchConnection();
                    receiveBufferSize = couchConnection.socket().getReceiveBufferSize();
                    recycleChannel(couchConnection);
                } catch (IOException ignored) {
                }
                break;
        }

        return receiveBufferSize;
    }

    public static void setReceiveBufferSize(int receiveBufferSize) {
        receiveBufferSize = receiveBufferSize;
    }

    public static int getSendBufferSize() {
        if (0 == sendBufferSize) {
            try {
                SocketChannel couchConnection = createCouchConnection();
                sendBufferSize = couchConnection.socket().getReceiveBufferSize();
                recycleChannel(couchConnection);
            } catch (IOException ignored) {
            }
        }
        return sendBufferSize;
    }

    public static void setSendBufferSize(int sendBufferSiz) {
        sendBufferSize = sendBufferSiz;
    }

    public static String dequote(String s) {
        String ret = s;
        if (null != s && ret.startsWith("\"") && ret.endsWith("\"")) {
            ret = ret.substring(1, ret.lastIndexOf('"'));
        }

        return ret;
    }

    /**
     * 'do the right thing' when handed a buffer with no remaining bytes.
     *
     * @param buf
     * @return
     */
    public static ByteBuffer avoidStarvation(ByteBuffer buf) {
        if (0 == buf.remaining()) {
            buf.rewind();
        }
        return buf;
    }

    public static String getDefaultOrgName() {
        return COUCH_DEFAULT_ORGNAME;
    }

    /**
     * byte-compare of suffixes
     *
     * @param terminator  the token used to terminate presumably unbounded growth of a list of buffers
     * @param currentBuff current ByteBuffer which does not necessarily require a list to perform suffix checks.
     * @param prev        a linked list which holds previous chunks
     * @return whether the suffix composes the tail bytes of current and prev buffers.
     */
    public static boolean suffixMatchChunks(byte[] terminator, ByteBuffer currentBuff, ByteBuffer... prev) {
        ByteBuffer tb = currentBuff;
        int prevMark = prev.length;
        int bl = terminator.length;
        int rskip = 0;
        int i = bl - 1;
        while (0 <= i) {
            rskip++;
            int comparisonOffset = tb.position() - rskip;
            if (0 > comparisonOffset) {
                prevMark--;
                if (0 <= prevMark) {
                    tb = prev[prevMark];
                    rskip = 0;
                    i++;
                } else {
                    return false;

                }
            } else if (terminator[i] != tb.get(comparisonOffset)) {
                return false;
            }
            i--;
        }
        return true;
    }

    public static boolean isDEBUG_SENDJSON() {
        return DEBUG_SENDJSON;
    }

    public static void setDEBUG_SENDJSON(boolean DEBUG_SENDJSON) {
        BlobAntiPatternObject.DEBUG_SENDJSON = DEBUG_SENDJSON;
    }

    public static void setLOOPBACK(InetAddress LOOPBACK) {
        BlobAntiPatternObject.LOOPBACK = LOOPBACK;
    }

    public static InetSocketAddress getCOUCHADDR() {
        return COUCHADDR;
    }

    public static void setCOUCHADDR(InetSocketAddress COUCHADDR) {
        BlobAntiPatternObject.COUCHADDR = COUCHADDR;
    }

    public static ExecutorService getEXECUTOR_SERVICE() {
        return EXECUTOR_SERVICE;
    }

}
