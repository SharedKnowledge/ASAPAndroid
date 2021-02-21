package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.android.lora.exceptions.ASAPLoRaException;
import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessageInterface;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.utils.DateTimeHelper;

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

/**
 * This class manages the Stream-Operations in communication with the ASAPLoRaBTModule and the
 * corresponding ASAPPeers, negotiating data flow from or to the BluetoothSocket to or from the
 * LoRaASAPInput- or OutputStreams by using the Hardware-address from the Packet-Header.
 */
public class LoRaBTInputOutputStream {
    private static final String CLASS_LOG_TAG = "ASAPLoRaBTIOStream";
    private static final long READ_WAIT_TIMEOUT = 10 * 1000; //10 seconds in ms
    private final BluetoothSocket btSocket;
    private final LoRaBTInputStream is;
    private final LoRaBTOutputStream os;
    private final HashMap<String, LoRaASAPInputStream> loRaASAPInputStreams = new HashMap<>();
    private final HashMap<String, LoRaASAPOutputStream> loRaASAPOutputStreams = new HashMap<>();

    /**
     * Initialize our managementclass, opening Sockets from and to the ASAPLoRaBTModule
     *
     * @param btSocket
     * @throws IOException
     */
    public LoRaBTInputOutputStream(BluetoothSocket btSocket) throws IOException {
        this.btSocket = btSocket;
        this.is = new LoRaBTInputStream(btSocket.getInputStream());
        this.os = new LoRaBTOutputStream(btSocket.getOutputStream());
    }

    /**
     * Close all open Sockets and Resources, wait for them to close down
     */
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

    /**
     * Tries to retrieve the {@link LoRaASAPOutputStream} corresponding to the MAC-Address in mac
     * If no {@link LoRaASAPOutputStream} exists yet, it creates one.
     *
     * @param mac
     * @return
     */
    public LoRaASAPOutputStream getASAPOutputStream(String mac) {
        if (this.loRaASAPOutputStreams.containsKey(mac))
            return this.loRaASAPOutputStreams.get(mac);

        this.loRaASAPOutputStreams.put(mac, new LoRaASAPOutputStream(mac));
        return this.getASAPOutputStream(mac);
    }


    /**
     * Tries to retrieve the {@link LoRaASAPInputStream} corresponding to the MAC-Address in mac
     * If no {@link LoRaASAPInputStream} exists yet, it creates one.
     *
     * @param mac
     * @return
     */
    public LoRaASAPInputStream getASAPInputStream(String mac) {
        if (this.loRaASAPInputStreams.containsKey(mac))
            return this.loRaASAPInputStreams.get(mac);

        this.loRaASAPInputStreams.put(mac, new LoRaASAPInputStream(mac));
        return this.getASAPInputStream(mac);
    }

    /**
     * Checks if there is an active {@link LoRaASAPInputStream} for this mac
     * @param macAddress
     * @return
     */
    public boolean hasASAPInputStream(String macAddress) {
        return this.loRaASAPInputStreams.containsKey(macAddress);
    }

    public LoRaBTInputStream getInputStream() {
        return is;
    }

    public LoRaBTOutputStream getOutputStream() {
        return os;
    }

    /**
     * Triggers sending of Packages residing in the {@link LoRaASAPOutputStream}s
     * @throws IOException
     */
    public void flushASAPOutputStreams() throws IOException {
        for (LoRaASAPOutputStream os : this.loRaASAPOutputStreams.values())
            os.flush();
    }

    /**
     * Closes the {@link LoRaASAPInputStream} and waits for it to actually close, before removing
     * it from our HashMap, so that it gets garbage collected. This is necessary, so that it has
     * time to signal the ASAPPeer the EOF of the stream, closing the ASAPPeers connection. The
     * corresponding {@link LoRaASAPOutputStream} is closed and removed from the HashMap, too.
     * @param mac
     */
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

    /**
     * Subclass for the Communication with ASAPMessages over BluetoothStreams
     */
    static class LoRaBTInputStream extends FilterInputStream {
        /**
         * Blocking reads a Line from the underlying BluetoothInputStream and tries to create an
         * {@link ASAPLoRaMessageInterface} from its {@link AbstractASAPLoRaMessage} Factory.
         *
         * @return ASAPLoRaMessageInterface
         * @throws IOException
         * @throws ASAPLoRaException
         */
        public ASAPLoRaMessageInterface readASAPLoRaMessage() throws IOException, ASAPLoRaException {
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

    /**
     * Subclass for the Communication with ASAPMessages over BluetoothStreams
     */
    static class LoRaBTOutputStream extends FilterOutputStream {
        private static final String CLASS_LOG_TAG = "ASAPLoRaBTOutputStream";

        public LoRaBTOutputStream(OutputStream out) {
            super(out);
        }

        /**
         * Writes the Payload of an Instance of {@link ASAPLoRaMessageInterface} onto the
         * underlying OutputStream.
         *
         * @param msg
         * @throws IOException
         * @throws ASAPLoRaException
         */
        public void write(ASAPLoRaMessageInterface msg) throws IOException, ASAPLoRaException {
            synchronized (this) {
                String msgString = msg.getPayload();
                Log.i(CLASS_LOG_TAG, "Writing Message to BT Board: " + msgString);
                this.write(msgString.getBytes());
                this.write('\n');
            }
        }
    }

    /**
     * Subclass for the communication to ASAPPeers
     */
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

        /**
         * Append a {@link ByteArrayInputStream} to Read Data from, created from the bytes in data.
         * Then notify a potential reading thread about the new data arriving.
         *
         * @param data
         */
        public void appendData(byte[] data) {
            //discard empty data arrays
            if (data.length == 0)
                return;

            synchronized (this.threadLock) {
                this.inputStreams.add(new ByteArrayInputStream(data));
                this.threadLock.notify();
            }
        }

        /**
         * Returns the sum of all available bytes in all Streams of our InputStream-Queue.
         *
         * @return
         * @throws IOException
         */
        @Override
        public int available() throws IOException {
            int availableBytes = 0;
            for (InputStream is : inputStreams)
                availableBytes += is.available();

            return availableBytes;
        }

        /**
         * Tell our Stream that we're about to close and notify a potential reading thread.
         */
        @Override
        public void close() {
            this.shouldClose = true;

            // Check if someone is currently reading.
            // If so, notify, else just assume we are closed
            if (this.isReading) {
                synchronized (this.threadLock) {
                    this.inputStreams.clear();
                    this.threadLock.notify();
                }
            } else {
                this.inputStreams.clear();
                this.wasClosed = true;
            }
        }

        /**
         * Reads from our Stream.
         * Blocks until data arrives or the Stream is closed from the {@link LoRaCommunicationManager}
         *
         * @return New Data or -1
         * @throws IOException
         */
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


    /**
     * Subclass for the communication from ASAPPeers.
     * Divides all data into LoRa-Transmissible chunks.
     * Derived from @{@link java.io.BufferedOutputStream}
     */
    class LoRaASAPOutputStream extends OutputStream {
        private final String LoRaAddress;
        private byte chunk[] = new byte[174]; //we can't deliver messages that are longer over LoRa
        private int count = 0;

        public LoRaASAPOutputStream(String mac) {
            this.LoRaAddress = mac;
        }

        /**
         * Flushes our current chunk to the {@link LoRaBTOutputStream} if there is any data
         * @throws IOException
         */
        @Override
        public void flush() throws IOException {
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
         * Writes a Byte-Array to our chunk, subdivides if necessary and flushes our buffer if needed.
         * This is a modified write from BufferedOutputStream to create a chunked stream.
         *
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

        /**
         * Writes a single byte and flushes the chunk if it would overflow otherwise.
         *
         * @param b
         * @throws IOException
         */
        @Override
        public synchronized void write(int b) throws IOException {
            if (count >= chunk.length) {
                this.flush();
            }
            chunk[count++] = (byte)b;
        }
    }
}
