package net.sharksystem.util.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TCP creates a virtual channel between to communication partners. Only during binding procedure,
 * there are two roles: server and client. Server opens a port to which clients can connect to.
 *
 * After connection, there are an open socket. That class implements that view.
 * Objects of that class can be retrieved by some static factory methods. Again:
 * getting e.g. a server channel makes no difference as soon as a connection is established.
 *
 * After object creation, start that channel thread simply by calling start().
 * Call @see isConnected() to check if a connection was created or @see waitUntilConnectionEstablished()
 * which blocks the calling thread until a connection was established.
 *
 * After connection establishment, methods by @see getInputStream() and @see getOutputStream()
 * can be used to get I/O streams to use the TCP channel.
 *
 * When creating a server, createSocket can be called more than once, if and only if,
 * a server was created with multiple-flag set true (@see getTCPServerCreator()).
 * In that case, the server accepts multiple client connection attempts and tries to create
 * a new connection with each call on createSocket.
 *
 * @author thsc
 */
public class TCPChannelMaker extends Thread {
    private final int port;
    private final boolean asServer;
    private final String hostname;
    private final boolean multiple;

    private boolean fatalError = false;
    private boolean threadRunning = false;

    public static int wait_for_next_connection_try;
    public static final int WAIT_FOR_NEXT_CONNECTION_TRY_DEFAULT = 10000; // any 10 sec

    public static int max_connection_loops;
    public static final int MAX_CONNECTION_LOOPS_DEFAULT = 100; // hundred times

    private TCPChannel channel;

    static {
        wait_for_next_connection_try = WAIT_FOR_NEXT_CONNECTION_TRY_DEFAULT;
        max_connection_loops = MAX_CONNECTION_LOOPS_DEFAULT;
    }

    /**
     * Create a tcp channel as client
     * @param hostname remote host - only used when client
     * @param port port - local port for server or remote port for client
     */
    public static TCPChannelMaker getTCPClientCreator(String hostname, int port) {
        return new TCPChannelMaker(hostname, port, false, false);
    }

    /**
     * Create a tcp channel as server
     * @param port port - local port for server or remote port for client
     */
    public static TCPChannelMaker getTCPServerCreator(int port) {
        return new TCPChannelMaker(null, port, true, false);
    }

    /**
     * Create a tcp channel as server
     * @param port port - local port for server or remote port for client
     * @param multiple - allow multiple connection on server, createSocket can be more than once
     */
    public static TCPChannelMaker getTCPServerCreator(int port, boolean multiple) {
        return new TCPChannelMaker(null, port, true, multiple);
    }

    /**
     * @param hostname remote host - only used when client
     * @param port port - local port for server or remote port for client
     * @param asServer - act as server or client
     * @param multiple - allow multiple connection on server - only used when server
     */
    private TCPChannelMaker(String hostname, int port, boolean asServer, boolean multiple) {
        this.hostname = hostname;
        this.port = port;
        this.asServer = asServer;
        this.multiple = multiple;
    }

    /**
     * connection maker thread already started?
     */
    public boolean running() {
        return this.threadRunning;
    }

    /**
     * Called when calling @see createSocket. Do not call this method directly.
     */
    @Override
    public void run() {
        this.threadRunning = true;
        try {
            if(this.asServer) {
                this.channel = new TCPServer(this.port, this.multiple);
            } else {
                this.channel = new TCPClient(this.hostname, this.port);
            }

            // this can take a while
            this.channel.createSocket();

        } catch (IOException ex) {
            //<<<<<<<<<<<<<<<<<<debug
            String s = "couldn't esatblish connection";
            System.out.println(s);
            this.fatalError = true;
        }
    }

    public void close() throws IOException {
        if(this.channel != null) {
            this.channel.close();
            //<<<<<<<<<<<<<<<<<<debug
            System.out.println("socket closed");
        }
    }

    /**
     * holds thread until a connection is established
     */
    public void waitUntilConnectionEstablished() throws IOException {
        if(!this.threadRunning) {
            /* in unit tests there is a race condition between the test
            thread and those newly created tests to establish a connection.

            Thus, this call could be in the right order - give it a
            second chance
            */

            try {
                Thread.sleep(wait_for_next_connection_try);
            } catch (InterruptedException ex) {
                // ignore
            }

            if(!this.threadRunning) {
                // that's probably wrong usage:
                throw new IOException("must start TCPChannel thread first by calling start()");
            }
        }

        while(!this.fatalError && !this.isConnected()) {
            try {
                Thread.sleep(wait_for_next_connection_try);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
    }

    /**
     * Starts a thread that runs until a connection was established or it failed
     *
     * @param listener
     */
    public void notifyWhenConnectionEstablishmened(TCPChannelMakerListener listener) {
        Thread waiterThread = new WaitAndNotifyThread(listener, this.channel);
        waiterThread.start();
    }

    private class WaitAndNotifyThread extends Thread {
        private final TCPChannelMakerListener listener;
        private final TCPChannel channel;

        WaitAndNotifyThread(TCPChannelMakerListener listener, TCPChannel channel) {
            this.listener = listener;
            this.channel = channel;
        }

        public void run() {
          try {
            waitUntilConnectionEstablished();
            // done
              this.listener.onConnectionEstablished(this.channel);
            } catch (IOException e) {
                this.listener.onConnectionEstablishmentFailed(this.channel, e.getLocalizedMessage());
            }
        }
    }

    /**
     *
     * @return connection established or not yet
     */
    public boolean isConnected() {
        return this.channel.isConnected();
    }

    public InputStream getInputStream() throws IOException {
        return this.channel.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return this.channel.getOutputStream();
    }

    /**
     * A server can provide more than one connection - depending on client
     * attempts for connections. This method can be called, if a server channel with
     * multiple-flag was created. If not, an IOException is called immediately. If
     * so, the thread is stopped
     *
     * @throws IOException
     */
    public void nextConnection() throws IOException {
        this.channel.nextConnection();
    }
}
