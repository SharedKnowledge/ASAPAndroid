package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import net.sharksystem.asap.android.lora.messages.ASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.RawASAPLoRaMessage;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class LoRaBTInputOutputStream {
    /**
     * Die IS / OS in dieser Klasse schreiben/lesen auf einem BluetoothSocket
     * Alle Adressen nutzen I/O Stream des BluetoothSocket, daher kapseln wir diesen in einel LoraBTInputStream
     * --> Adresse wird mit an uC gesendet, damit dieser an die richtige Stelle schreibt.
     * Syntax (erstidee): "ADDR:datadatadatadata"
     */
    private static final String CLASS_LOG_TAG = "ASAPLoRaBTIOStream";
    //TODO private final ObjectMapper objectMapper = new ObjectMapper();
    private BluetoothSocket btSocket;
    private LoRaBTInputStream is;
    private LoRaBTOutputStream os;
    private HashMap<String, LoRaASAPInputStream> loRaASAPInputStreams = new HashMap<>();
    private HashMap<String, BufferedOutputStream> loRaASAPOutputStreams = new HashMap<String, BufferedOutputStream>();

    LoRaBTInputOutputStream(BluetoothSocket btSocket) throws IOException {
        this.btSocket = btSocket;
        this.is = new LoRaBTInputStream(btSocket.getInputStream());
        this.os = new LoRaBTOutputStream(btSocket.getOutputStream());

        //Use Polymorphic Type Detection for JSON Object Mapping
        //TODO objectMapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().build(), ObjectMapper.DefaultTyping.NON_FINAL);
    }

    public void close() {
        try {
            if (this.btSocket != null)
                btSocket.close();
        } catch (IOException e) {
            Log.e(this.CLASS_LOG_TAG, e.getMessage());
        }
    }

    public OutputStream getASAPOutputStream(String mac) {
        if (this.loRaASAPOutputStreams.containsKey(mac))
            return this.loRaASAPOutputStreams.get(mac);

        this.loRaASAPOutputStreams.put(mac, new BufferedOutputStream(new LoRaASAPOutputStream(mac),20)); //TODO increase buffer size
        return this.getASAPOutputStream(mac); //TODO rewrite to make sure to never have endless loop?
    }

    public LoRaASAPInputStream getASAPInputStream(String mac) {
        if (this.loRaASAPInputStreams.containsKey(mac))
            return this.loRaASAPInputStreams.get(mac);

        this.loRaASAPInputStreams.put(mac, new LoRaASAPInputStream(mac));
        return this.getASAPInputStream(mac); //TODO rewrite to make sure to never have endless loop?
    }

    public LoRaBTInputStream getInputStream() {
        return is;
    }

    public LoRaBTOutputStream getOutputStream() {
        return os;
    }

    public void flushASAPOutputStreams() throws IOException {
        for(BufferedOutputStream bufferedOutputStream : this.loRaASAPOutputStreams.values())
            bufferedOutputStream.flush();
    }

    class LoRaBTInputStream extends FilterInputStream {

        public AbstractASAPLoRaMessage readASAPLoRaMessage() throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(this));
            return null;//TODO objectMapper.readValue(br.readLine(), AbstractASAPLoRaMessage.class);
        }

        public LoRaBTInputStream(InputStream in) {
            super(in);
        }
    }

    class LoRaBTOutputStream extends FilterOutputStream {
        private static final String CLASS_LOG_TAG = "ASAPLoRaBTOutputStream";

        public LoRaBTOutputStream(OutputStream out) {
            super(out);
        }

        public void write(AbstractASAPLoRaMessage msg) throws IOException {
            if (msg instanceof RawASAPLoRaMessage)
                this.write(msg.toString().getBytes());
            else {
                String msgString = ""; //TODO objectMapper.writeValueAsString(msg);
                Log.i(this.CLASS_LOG_TAG, "Writing Message to BT Board: "+msgString);
                this.write(msgString.getBytes());
            }
            this.write('\n');
        }
    }

    class LoRaASAPInputStream extends InputStream {
        private final String LoRaAddress;

        private SequenceInputStream sis;

        public LoRaASAPInputStream(String mac) {
            super();
            this.sis = new SequenceInputStream(new ByteArrayInputStream(new byte[0]), new ByteArrayInputStream(new byte[0]));
            this.LoRaAddress = mac;
        }

        public synchronized void appendData(byte[] data) {
            this.sis = new SequenceInputStream(this.sis, new ByteArrayInputStream(data)); //TODO this can't be right.
        }

        @Override
        public synchronized int read() throws IOException {
            while(sis.available() <= 0) { //TODO Timeout
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //return -1; //No Data
                }
            }
            return sis.read();
        }
    }

    class LoRaASAPOutputStream extends ByteArrayOutputStream {
        private final String LoRaAddress;

        public LoRaASAPOutputStream(String mac) {
            super(250); //TODO Prüfen ob wir wirklich immer 250 bytes schicken können
            this.LoRaAddress = mac;
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            //TODO...? Ist das sinnig?
            try {
                LoRaBTInputOutputStream.this.getOutputStream().write(new ASAPLoRaMessage(this.LoRaAddress, b));
            } catch (IOException e) {
                e.printStackTrace(); //TODO...
            }
        }

        @Override
        public synchronized void write(int b) {
            //TODO...? Ist das sinnig?
            byte[] byteArray = ByteBuffer.allocate(1).putInt(b).array();
            this.write(byteArray, 0, 1);
        }
    }
}
