package net.sharksystem.asap.android.example;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.instacart.library.truetime.TrueTime;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPHop;
import net.sharksystem.asap.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ASAPExamplePerformanceTestActivity extends ASAPActivity implements ASAPMessageReceivedListener {
    private static final CharSequence PERFORMANCE_TEST_URI = "asap://performanceTest";
    private List<String> receivedMessages = new ArrayList<>();
    private List<PerformanceTestPDU> receivedPDU = new ArrayList<>();
    private ListView receivedMessagesListView;
    EditText messageSizeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_hub_performance_test_layout);

        Button sendButton = findViewById(R.id.sendTestMessageButton);
        sendButton.setOnClickListener(view -> sendTestMessage());
        messageSizeEditText = findViewById(R.id.performanceTestMessageSize);
        TextView uriTextView = findViewById(R.id.currentUriTextView);
        uriTextView.setText(PERFORMANCE_TEST_URI);
        receivedMessagesListView = findViewById(R.id.listViewReceivedMessages);
        // set listener to get informed about newly arrived messages
        this.getASAPAndroidPeer().addASAPMessageReceivedListener(
                ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME, // listen to this app
                ASAPExamplePerformanceTestActivity.this);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.getASAPAndroidPeer().removeASAPMessageReceivedListener(
                ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME,
                ASAPExamplePerformanceTestActivity.this);
    }

    // send message with ASAP peer
    private void sendTestMessage() {
        // asap messages are bytes
        int messageSize = Integer.parseInt(messageSizeEditText.getText().toString());
        PerformanceTestPDU performanceTestPDU = new PerformanceTestPDU(messageSize);
        Log.d(this.getLogStart(), String.format("going to send %d bytes", messageSize));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out;

        try {
            out = new ObjectOutputStream(bos);
            Long timeFromNtp = getTrueTime();
            if (timeFromNtp == null) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to get current time from NTP server. " +
                        "Abort.", Toast.LENGTH_LONG).show());
                return;
            }
            performanceTestPDU.setTimestamp(timeFromNtp);
            Log.d("mytimestamp", new Date(timeFromNtp).toString());
            out.writeObject(performanceTestPDU);
            out.flush();
            byte[] byteContent = bos.toByteArray();
            this.getASAPPeer().sendASAPMessage(
                    ExampleAppDefinitions.ASAP_EXAMPLE_APPNAME,
                    PERFORMANCE_TEST_URI,
                    byteContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ASAPException e) {
            Log.e(this.getLogStart(), "when sending asap message: " + e.getLocalizedMessage());
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    private void addReceivedMessage(String message) {
        this.receivedMessages.add(message);
        runOnUiThread(() -> {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(ASAPExamplePerformanceTestActivity.this,
                    android.R.layout.simple_list_item_1,
                    receivedMessages.toArray(new String[0]));
            receivedMessagesListView.setAdapter(arrayAdapter);
        });
    }

    // handle incoming messages
    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String s, List<ASAPHop> list) throws IOException {
        Log.d(getLogStart(), "going to handle received messages with uri: "
                + asapMessages.getURI());
        try {
            Iterator<byte[]> messages = asapMessages.getMessages();
            while (messages.hasNext()) {
                byte[] receivedMessage = messages.next();
                ByteArrayInputStream bis = new ByteArrayInputStream(receivedMessage);
                ObjectInput in = null;
                try {
                    in = new ObjectInputStream(bis);
                    Object o = in.readObject();
                    if (o instanceof PerformanceTestPDU) {
                        PerformanceTestPDU performanceTestPDU = (PerformanceTestPDU) o;
                        Long currentTime = getTrueTime();
                        if (currentTime == null) {
                            runOnUiThread(() -> Toast.makeText(this, "Failed to get current time from NTP server. " +
                                    "Abort.", Toast.LENGTH_LONG).show());
                        } else {
                            long diff = currentTime - performanceTestPDU.getTimestamp();
                            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);

                            if (receivedPDU.contains(performanceTestPDU)) {
                                Log.d(getLogStart(), "PDU was already received. ");
                            } else {
                                Log.d(getLogStart(), "add PDU to received messages. ");
                                receivedPDU.add(performanceTestPDU);
                                String message = String.format("received %d bytes in %dms.", performanceTestPDU.getData().length, diff);
                                addReceivedMessage(message);
                            }

                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException ex) {
                        // ignore close exception
                    }
                }
            }
        } catch (IOException e) {
            Log.e(this.getLogStart(), "problems when handling received messages: "
                    + e.getLocalizedMessage());
        }
    }

    public static void init() {
        (new Thread(() -> {
            try {
                TrueTime.clearCachedInfo();
                TrueTime.build().withNtpHost("time.google.com").withLoggingEnabled(true).withConnectionTimeout(1500).initialize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        })).start();
    }

    public static Long getTrueTime() {
        Date trueDate = null;
        if (TrueTime.isInitialized()) {
            trueDate = TrueTime.now();
        }
        return trueDate != null ? trueDate.getTime() : null;
    }
}

class PerformanceTestPDU implements Serializable {
    private static final long serialVersionUID = 1L;
    private long timestamp;
    private byte[] data;

    public PerformanceTestPDU(int dataSize) {
        this.data = new byte[dataSize];
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object object) {
        boolean sameSame = false;
        if (object != null && object instanceof PerformanceTestPDU) {
            sameSame = this.timestamp == ((PerformanceTestPDU) object).timestamp;
        }
        return sameSame;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
