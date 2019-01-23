package net.sharksystem.aasp.net.sharksystem.util.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class TCPServer extends TCPChannel {
    private ServerSocket srvSocket;
    private final int port;
    private final boolean multiple;
    private List<Socket> socketList;

    private Thread acceptThread = null;

    TCPServer(int port, boolean multiple) throws IOException {
        this.port = port;
        this.multiple = multiple;

        // create server socket
        this.srvSocket = new ServerSocket(port);

        // create list oif socket - used if multiple flag set
        socketList = new ArrayList<>();
    }

    /**
     * That method is called within a thread. If multiple is set true
     * each client connection is accept and kept in a socketlist.
     * @return next or only socke4t
     * @throws IOException
     */
    void createSocket() throws IOException {
        // called first time
        if(this.acceptThread == null) {
            // wait for connection attempt
            Socket newSocket = srvSocket.accept();

            // got a socket

            if(multiple) {
                // create a new thread to collect other sockets
                this.acceptThread = new Thread() {
                    public void run() {
                        try {
                            while(multiple) {
                                // loop will be broken when close called which closes srvSocket
                                socketList.add(srvSocket.accept());
                            }
                        } catch (IOException e) {
                            // leave loop
                        }
                        finally {
                            try {
                                srvSocket.close();
                            } catch (IOException e1) {
                                // ignore
                            }
                            srvSocket = null; // remember invalid server socket
                        }
                    }
                };
                this.acceptThread.start();
            }

            // set first found socket on top of the queue
            this.setSocket(newSocket);

        } else {
            // an accept thread was already called

            // was is successful?
            boolean found = false;
            do {
                if (!this.socketList.isEmpty()) {
                    // make first socket on waiting list to current socket
                    this.setSocket(this.socketList.remove(0));
                    found = true;
                } else {
                    // wait
                    try {
                        // TODO: that's polling! replace with thread synchronization
                        Thread.sleep(WAIT_LOOP_IN_MILLIS);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            } while(!found);
        }
    }

    void close() throws IOException {
        super.close();

        if(this.srvSocket != null) {
            this.srvSocket.close();
        }
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
        if(!this.multiple)
            throw new IOException("multiple flag not set - no further connections");

        if(this.srvSocket == null) {
            throw new IOException("no open server socket, cannot create another connection");
        }

        // try to get next socket
        this.createSocket();
    }
}
