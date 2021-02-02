package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;
import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.utils.DateTimeHelper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;


public class LoRaBTInputOutputStream {
    /**
     * Die IS / OS in dieser Klasse schreiben/lesen auf einem BluetoothSocket
     * Alle Adressen nutzen I/O Stream des BluetoothSocket, daher kapseln wir diesen in einel LoraBTInputStream
     * --> Adresse wird mit an uC gesendet, damit dieser an die richtige Stelle schreibt.
     * Syntax (erstidee): "ADDR:datadatadatadata"
     */
    private static final String CLASS_LOG_TAG = "ASAPLoRaBTIOStream";
    private static final long READ_WAIT_TIMEOUT = 10 * 1000; //10 seconds in ms
    private final BluetoothSocket btSocket;
    private final LoRaBTInputStream is;
    private final LoRaBTOutputStream os;
    private final HashMap<String, LoRaASAPInputStream> loRaASAPInputStreams = new HashMap<>();
    private final HashMap<String, LoRaASAPOutputStream> loRaASAPOutputStreams = new HashMap<>();

    public LoRaBTInputOutputStream(BluetoothSocket btSocket) throws IOException {
        this.btSocket = btSocket;
        this.is = new LoRaBTInputStream(btSocket.getInputStream());
        this.os = new LoRaBTOutputStream(btSocket.getOutputStream());
    }

    public void close() {
        try {
            //Cleanup all loRaASAPOutputStreams
            for (OutputStream os : this.loRaASAPOutputStreams.values())
                os.close();
            this.loRaASAPOutputStreams.clear();

            //Cleanup all loRaASAPInputStreams
            for (InputStream is : this.loRaASAPInputStreams.values())
                is.close();

            //Wait for all loRaASAPInputStreams to close
            boolean allClosed;
            do {
                allClosed = true;
                for (LoRaASAPInputStream is : this.loRaASAPInputStreams.values())
                    allClosed = allClosed && is.closed();
            } while (!allClosed);

            //finish cleanup
            this.loRaASAPInputStreams.clear();

            //Close BT Socket if it is still open. Our IS and OS will close on socket disconnect
            if (this.btSocket != null && this.btSocket.isConnected())
                btSocket.close();

            Log.i(CLASS_LOG_TAG, "Successfully closed all Streams");
        } catch (IOException e) {
            Log.e(CLASS_LOG_TAG, "Exception in close(): " + e.getMessage());
        }
    }

    public LoRaASAPOutputStream getASAPOutputStream(String mac) {
        if (this.loRaASAPOutputStreams.containsKey(mac))
            return this.loRaASAPOutputStreams.get(mac);

        this.loRaASAPOutputStreams.put(mac, new LoRaASAPOutputStream(mac)); //TODO do we need a bufferstream here?
        return this.getASAPOutputStream(mac);
    }

    public LoRaASAPInputStream getASAPInputStream(String mac) {
        if (this.loRaASAPInputStreams.containsKey(mac))
            return this.loRaASAPInputStreams.get(mac);

        this.loRaASAPInputStreams.put(mac, new LoRaASAPInputStream(mac));
        return this.getASAPInputStream(mac);
    }

    public LoRaBTInputStream getInputStream() {
        return is;
    }

    public LoRaBTOutputStream getOutputStream() {
        return os;
    }

    public void flushASAPOutputStreams() throws IOException {
        for (LoRaASAPOutputStream os : this.loRaASAPOutputStreams.values())
            os.flush();
    }

    public void closeASAPStream(String mac) {
        if (this.loRaASAPInputStreams.containsKey(mac)) {
            LoRaASAPInputStream is = this.loRaASAPInputStreams.get(mac);
            is.close();
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) { /* does not matter */}
            } while (!is.closed());
            this.loRaASAPInputStreams.remove(mac);
        }
        if (this.loRaASAPOutputStreams.containsKey(mac)) {
            this.loRaASAPOutputStreams.get(mac).close();
            this.loRaASAPOutputStreams.remove(mac);
        }
    }

    static class LoRaBTInputStream extends FilterInputStream {

        public AbstractASAPLoRaMessage readASAPLoRaMessage() throws IOException, ASAPLoRaException {
            BufferedReader br = new BufferedReader(new InputStreamReader(this));
            String rawASAPLoRaMessage = "";
            do {
                rawASAPLoRaMessage = br.readLine();
                Log.i(CLASS_LOG_TAG, "Got Message from BT Board: " + rawASAPLoRaMessage);
            } while (rawASAPLoRaMessage.equals("")); //ignore empty lines
            return AbstractASAPLoRaMessage.createASAPLoRaMessage(rawASAPLoRaMessage);
        }

        public LoRaBTInputStream(InputStream in) {
            super(in);
        }
    }

    static class LoRaBTOutputStream extends FilterOutputStream {
        private static final String CLASS_LOG_TAG = "ASAPLoRaBTOutputStream";

        public LoRaBTOutputStream(OutputStream out) {
            super(out);
        }

        public void write(AbstractASAPLoRaMessage msg) throws IOException, ASAPLoRaException {
            synchronized (this) {
                String msgString = msg.getPayload();
                Log.i(CLASS_LOG_TAG, "Writing Message to BT Board: " + msgString);
                this.write(msgString.getBytes());
                this.write('\n');
            }
        }
    }

    static class LoRaASAPInputStream extends InputStream {
        private final String LoRaAddress;
        private Object threadLock = new Object();
        private boolean shouldClose = false;
        private boolean wasClosed = false;
        private boolean isReading = false;

        private LinkedList<InputStream> inputStreams = new LinkedList();

        public LoRaASAPInputStream(String mac) {
            super();
            this.LoRaAddress = mac;
        }

        public boolean closed() {
            return this.wasClosed;
        }

        public void appendData(byte[] data) {
            //discard empty data arrays
            if (data.length == 0)
                return;

            synchronized (this.threadLock) {
                this.inputStreams.add(new ByteArrayInputStream(data));
                this.threadLock.notify();
            }
        }

        @Override
        public int available() throws IOException {
            int availableBytes = 0;
            for (InputStream is : inputStreams)
                availableBytes += is.available();

            return availableBytes;
        }

        @Override
        public void close() {
            this.inputStreams.clear();
            this.shouldClose = true;

            // Check if someone is currently reading.
            // If so, notify, else just assume we are closed
            if (this.isReading)
                synchronized (this.threadLock) {
                    this.threadLock.notify();
                }
            else
                this.wasClosed = true;
        }

        @Override
        public int read() throws IOException {
            this.isReading = true;
            net.sharksystem.utils.Log.writeLog(this, DateTimeHelper.long2ExactTimeString(System.currentTimeMillis()), "read start");
            int returnResult = 0;
            synchronized (this.threadLock) {
                while (this.available() < 1) {

                    if (this.shouldClose) {
                        this.wasClosed = true;
                        this.isReading = false;
                        return -1; //if our stream was closed, return an EOF Signal to ASAPEngine
                    }

                    // no data, wait
                    try {
                        this.threadLock.wait(LoRaBTInputOutputStream.READ_WAIT_TIMEOUT);
                    } catch (InterruptedException e) {/* NOOP, lets check our conditions again. */}
                }

                if (this.inputStreams.isEmpty())
                    throw new IOException("Tried to read from Empty InputStream Queue."); //this *SHOULD* never happen.

                //get our current inputstream and read its data
                InputStream nextIs = this.inputStreams.peek();
                returnResult = nextIs.read();

                if (nextIs.available() == 0) //check if our stream is now empty
                    this.inputStreams.remove(); //if so, remove empty stream from Queue
            }

            this.isReading = false;
            return returnResult;
        }
    }

    class LoRaASAPOutputStream extends OutputStream {
        private final String LoRaAddress;
        private byte chunk[] = new byte[177]; //TODO - is this the right value to maximize the throughput?
        private int count = 0;

        public LoRaASAPOutputStream(String mac) {
            this.LoRaAddress = mac;
        }

        @Override
        public void flush() throws IOException {
            //send data to to LoRaBTInputOutputStream.this.getOutputStream()
            if (this.count > 0) {
                try {
                    ASAPLoRaMessage asapLoRaMessage = new ASAPLoRaMessage(this.LoRaAddress, Arrays.copyOf(this.chunk, this.count));
                    LoRaBTInputOutputStream.this.getOutputStream().write(asapLoRaMessage);
                    this.count = 0;
                } catch (ASAPLoRaException e) {
                    throw new IOException(e); //convert our ASAPLoRaException to an IOException and bubble it.
                }
            }
        }

        @Override
        public void close() {
            // No need for any action. If we get closed, there is no point in sending any more data
        }

        /**
         * Modified write from @BufferedOutputStream to create a chunked stream
         * @param b
         * @param off
         * @param len
         * @throws IOException
         */
        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            //we need to write to a buffer and trigger a flush once one chunk is complete.
            int currentChunkSpace = chunk.length - this.count;
            if(len > currentChunkSpace){ //if the message is bigger than our current space in the buffer
                //cut of a chunk the size of our remaining chunk
                this.write(Arrays.copyOf(b, currentChunkSpace));
                //Flush it down the LoRa Board
                this.flush();
                //handle the rest of the byte array recursively
                this.write(Arrays.copyOfRange(b, currentChunkSpace, b.length));
                return;
            }

            //write the data to our chunk
            System.arraycopy(b, off, chunk, count, len);
            count += len;

            //if chunk is now full, flush it.
            if (count >= chunk.length) {
                this.flush();
            }
        }

        @Override
        public synchronized void write(int b) throws IOException {
            if (count >= chunk.length) {
                this.flush();
            }
            chunk[count++] = (byte)b;
        }
    }
}
