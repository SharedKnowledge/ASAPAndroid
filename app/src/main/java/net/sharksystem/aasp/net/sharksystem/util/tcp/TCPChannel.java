package net.sharksystem.aasp.net.sharksystem.util.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

abstract class TCPChannel {
    static final long WAIT_LOOP_IN_MILLIS = 1000;
    private Socket socket = null;

    void close() throws IOException {
        if(this.socket != null) {
            this.socket.close();
        }
    }

    protected void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Create an new socket
     * @throws IOException
     */
    abstract void createSocket() throws IOException;

    /**
     * Throws an IOException if no connection has been established yet.
     *
     * @throws IOException
     */
    private void checkConnected() throws IOException {
        if(!this.isConnected()) {
            throw new IOException("no connection established - cannot get streams.");
        };
    }

    InputStream getInputStream() throws IOException {
        this.checkConnected();
        return this.socket.getInputStream();
    }

    OutputStream getOutputStream() throws IOException {
        this.checkConnected();
        return this.socket.getOutputStream();
    }

    /**
     * A server can provide more than one connection - depending on client
     * attempts for connections. This method can be called, if a server channel with
     * multiple-flag was created. If not, an IOException is called immediately. If
     * so, the thread is stopped
     *
     * @throws IOException
     */

    void nextConnection() throws IOException {
        throw new IOException("no further connections possible");
    }

    boolean isConnected() {
        return this.socket != null;
    }
}
