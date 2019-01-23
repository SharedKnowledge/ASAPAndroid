package net.sharksystem.util.tcp;

import java.io.IOException;
import java.net.Socket;

class TCPClient extends TCPChannel {

    private final String hostname;
    private final int port;

    TCPClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    void createSocket() throws IOException {
        for(;;) {
            try {
                //<<<<<<<<<<<<<<<<<<debug
                StringBuilder b = new StringBuilder();
                b.append("TCClient:");
                b.append("try to connect to ");
                b.append(this.hostname);
                b.append(" port: ");
                b.append(port);
                System.out.println(b.toString());
                //>>>>>>>>>>>>>>>>>>>debug

                this.setSocket(new Socket(this.hostname, this.port));

                // break loop if no exception thrown
                return;
            }
            catch(IOException ioe) {
                //<<<<<<<<<<<<<<<<<<debug
                StringBuilder b = new StringBuilder();
                b.append("TCClient:");
                b.append("failed / wait and re-try");
                b.append(port);
                System.out.println(b.toString());
                try {
                    Thread.sleep(TCPChannel.WAIT_LOOP_IN_MILLIS);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }
    }
}