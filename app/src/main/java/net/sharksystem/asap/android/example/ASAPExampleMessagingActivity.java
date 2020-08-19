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
import java.util.Iterator;

public class ASAPExampleMessagingActivity extends ASAPExampleRootActivity {
    private static final CharSequence EXAMPLE_URI ="asap://exampleURI";
    private static final CharSequence EXAMPLE_MESSAGE = "ASAP example message";
    private ASAPMessageReceivedListener receivedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_messaging_layout);

        // set URI - your app can or your users can choose any valid uri.
        TextView uriTextView = findViewById(R.id.exampleMessagingUri);

        uriTextView.setText("channel URI: " + EXAMPLE_URI);

        EditText messageEditView = findViewById(R.id.exampleMessagingMessageText);
        messageEditView.setText(EXAMPLE_MESSAGE);

        TextView receivedMessagesTV = this.findViewById(R.id.exampleMessagingReceivedMessages);
        receivedMessagesTV.setText(this.getYourAreNotice());

        this.receivedListener = new ExampleMessageReceivedListener();

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

    private CharSequence getYourAreNotice() {
        return "You go by id: " + this.getASAPApplication().getASAPOwnerID() + "\n";
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
    }

    // handle incoming messages
    private void doHandleReceivedMessages(ASAPMessages asapMessages) {
        Log.d(this.getLogStart(), "going to handle received messages with uri: "
                + asapMessages.getURI());

        TextView receivedMessagesTV = this.findViewById(R.id.exampleMessagingReceivedMessages);
        StringBuilder sb = new StringBuilder();
        sb.append(this.getYourAreNotice());

        try {
            Iterator<CharSequence> messagesAsCharSequence = asapMessages.getMessagesAsCharSequence();
            sb.append("new messages:\n");
            while(messagesAsCharSequence.hasNext()) {
                sb.append(messagesAsCharSequence.next());
            }
            receivedMessagesTV.setText(sb.toString());
        } catch (IOException e) {
            Log.e(this.getLogStart(), "problems when handling received messages: "
                    + e.getLocalizedMessage());
        }
    }

    private class ExampleMessageReceivedListener implements ASAPMessageReceivedListener {
        @Override
        public void asapMessagesReceived(ASAPMessages asapMessages) {
            Log.d(getLogStart(), "asapMessageReceived");
            ASAPExampleMessagingActivity.this.doHandleReceivedMessages(asapMessages);
        }
    }
}
