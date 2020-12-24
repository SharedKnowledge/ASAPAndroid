package net.sharksystem.asap.android.lora;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import net.sharksystem.asap.android.lora.messages.AbstractASAPLoRaMessage;
import net.sharksystem.asap.android.lora.messages.RawASAPLoRaMessage;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class LoRaBTInputOutputStream {
    /**
     * Die IS / OS in dieser Klasse schreiben/lesen auf einem BluetoothSocket
     * Alle Adressen nutzen I/O Stream des BluetoothSocket, daher kapseln wir diesen in einel LoraBTInputStream
     * --> Adresse wird mit an uC gesendet, damit dieser an die richtige Stelle schreibt.
     * Syntax (erstidee): "ADDR:datadatadatadata"
     */
    private static final String CLASS_LOG_TAG = "ASAPLoRaBTIOStream";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private BluetoothSocket btSocket;
    private LoRaBTInputStream is;
    private LoRaBTOutputStream os;

    LoRaBTInputOutputStream(/*String mac, */BluetoothSocket btSocket) throws IOException {
        //this.LoRaAddress = mac;
        this.btSocket = btSocket;
        this.is = new LoRaBTInputStream(btSocket.getInputStream());
        this.os = new LoRaBTOutputStream(btSocket.getOutputStream());

        //Use Polymorphic Type Detection for JSON Object Mapping
        objectMapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().build(), ObjectMapper.DefaultTyping.NON_FINAL);
    }

    public void close() {
        try {
            if (this.btSocket != null)
                btSocket.close();
        }catch (IOException e){
            Log.e(this.CLASS_LOG_TAG, e.getMessage());
        }
    }

    public LoRaBTInputStream getInputStream() {
        return is;
    }

    public LoRaBTOutputStream getOutputStream() {
        return os;
    }

    class LoRaBTInputStream extends FilterInputStream {

        public AbstractASAPLoRaMessage readASAPLoRaMessage() throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(this));
            return objectMapper.readValue(br.readLine(), AbstractASAPLoRaMessage.class);
        }

        public LoRaBTInputStream(InputStream in) {
            super(in);
        }
    }

    class LoRaBTOutputStream extends FilterOutputStream {
        public LoRaBTOutputStream(OutputStream out) {
            super(out);
        }

        public void write(AbstractASAPLoRaMessage msg) throws IOException {
            if (msg instanceof RawASAPLoRaMessage)
                this.write(msg.toString().getBytes());
            else
                this.write(objectMapper.writeValueAsString(msg).getBytes());
        }
    }
}
