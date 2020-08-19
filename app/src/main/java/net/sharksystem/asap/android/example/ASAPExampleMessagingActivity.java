package net.sharksystem.asap.android.example;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPMessageReceivedListener;
import net.sharksystem.asap.ASAPMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ASAPExampleMessagingActivity extends ASAPExampleRootActivity {
    private static final CharSequence EXAMPLE_URI ="asap://exampleURI";
    private static final CharSequence EXAMPLE_MESSAGE = "ASAP example message";
    private ASAPMessageReceivedListener receivedListener;
    private List<String> sentMessages = new ArrayList<>();
    private List<String> receivedMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_messaging_layout);

        // set URI - your app can or your users can choose any valid uri.
        TextView uriTextView = findViewById(R.id.exampleMessagingUri);

        uriTextView.setText("your owner id: " + this.getASAPApplication().getASAPOwnerID()
                + "channel URI: " + EXAMPLE_URI);

        EditText messageEditView = findViewById(R.id.exampleMessagingMessageText);
        messageEditView.setText(EXAMPLE_MESSAGE);

        this.receivedListener = new ASAPMessageReceivedListener() {
            @Override
            public void asapMessagesReceived(ASAPMessages asapMessages) {
                Log.d(getLogStart(), "asapMessageReceived");
                ASAPExampleMessagingActivity.this.doHandleReceivedMessages(asapMessages);
            }
        };

        // set listener to get informed about newly arrived messages
        this.getASAPApplication().addASAPMessageReceivedListener(
                ASAPExampleActivity.ASAP_EXAMPLE_APPNAME, // listen to this app
                this.receivedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.getASAPApplication().removeASAPMessageReceivedListener(
                ASAPExampleActivity.ASAP_EXAMPLE_APPNAME,
                this.receivedListener);
    }

    public void onAbortClick(View view) {
        this.finish();
    }

    // send ASAP message
    public void onSendClick(View view) {
        EditText messageEditView = findViewById(R.id.exampleMessagingMessageText);
        Editable messageText = messageEditView.getText();

        Log.d(this.getLogStart(), "going to send message: " + messageText);

        // asap messages are bytes
        byte[] byteContent = messageText.toString().getBytes();

        Log.d(this.getLogStart(), "going to send messageBytes: " + byteContent);

        try {
            this.sendASAPMessage(
                    ASAPExampleActivity.ASAP_EXAMPLE_APPNAME,
                    EXAMPLE_URI,
                    byteContent,
                    true);
        } catch (ASAPException e) {
            Log.e(this.getLogStart(), "when sending asap message: " + e.getLocalizedMessage());
        }

        // success - remember sent message
        this.sentMessages.add(messageText.toString());
    }

    // handle incoming messages
    private void doHandleReceivedMessages(ASAPMessages asapMessages) {
        Log.d(this.getLogStart(), "going to handle received messages with uri: "
                + asapMessages.getURI());

        // set up output
        StringBuilder sb = new StringBuilder();

        try {
            Iterator<CharSequence> messagesAsCharSequence = asapMessages.getMessagesAsCharSequence();
            sb.append("new messages:\n");
            while(messagesAsCharSequence.hasNext()) {
                String receivedMessage = messagesAsCharSequence.next().toString();
                this.receivedMessages.add(receivedMessage);
                sb.append(receivedMessage);
            }
            sb.append("your messages: \n");
            for(String msg : this.sentMessages) {
                sb.append(msg);
                sb.append("\n");
            }
            sb.append("received messages: \n");
            for(String msg : this.receivedMessages) {
                sb.append(msg);
            }
        } catch (IOException e) {
            Log.e(this.getLogStart(), "problems when handling received messages: "
                    + e.getLocalizedMessage());
            sb.append(e.getLocalizedMessage());
        }

        TextView receivedMessagesTV = this.findViewById(R.id.exampleMessagingMessages);
        receivedMessagesTV.setText(sb.toString());
    }
}
