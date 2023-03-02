package net.sharksystem.asap.android.hub_e2e;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import net.sharksystem.SharkPeerBasic;
import net.sharksystem.SharkPeerBasicImpl;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.android.R;
import net.sharksystem.asap.android.apps.ASAPActivity;
import net.sharksystem.hub.ASAPHubException;
import net.sharksystem.hub.Connector;
import net.sharksystem.hub.peerside.ASAPHubManager;
import net.sharksystem.hub.peerside.HubConnector;
import net.sharksystem.hub.peerside.HubConnectorFactory;
import net.sharksystem.hub.peerside.HubConnectorStatusListener;
import net.sharksystem.hub.peerside.SharedTCPChannelConnectorPeerSide;
import net.sharksystem.hub.peerside.TCPHubConnectorDescriptionImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ASAPHubE2EActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener
        ,HubConnectorStatusListener {

    private Button connectButton;
    private Button disconnectButton;
    private Button refreshListButton;
    private Button startPerformanceTestButton;
    private EditText hostNameEditText;
    private EditText portEditText;
    private EditText peerIdEditText;
    private ListView availablePeersListView;
    private Switch multichannelSwitch;
    private ArrayAdapter<String> adapter;

    private HubConnector hubConnector;
    private String hostName;
    private int port;
    private boolean multiChannel;
    private String peerId;
    private HubConnectorStatusListener hubConnectorStatusListener;
    private List<String> availablePeers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_hub_e2e_layout);

        connectButton = findViewById(R.id.connectButtonASAPHubE2E);
        disconnectButton = findViewById(R.id.disconnectButtonASAPHubE2E);
        refreshListButton = findViewById(R.id.refreshPeerListButton);
        hostNameEditText = findViewById(R.id.editTextASAPHubHostE2E);
        portEditText = findViewById(R.id.editTextASAPHubPortE2E);
        peerIdEditText = findViewById(R.id.editTextASAPHubPeerIdE2E);
        availablePeersListView = findViewById(R.id.listViewAvailablePeers);
        multichannelSwitch = findViewById(R.id.multichannelSwitch);
        startPerformanceTestButton = findViewById(R.id.startPerformanceTestButton);

        // set enabled
        multichannelSwitch.setEnabled(false);
        startPerformanceTestButton.setEnabled(false);

        // setup listView adapter
        availablePeers = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.listview, R.id.textView, availablePeers);
        availablePeersListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();


        // set listeners
        availablePeersListView.setOnItemClickListener(this);
        connectButton.setOnClickListener(this);
        disconnectButton.setOnClickListener(this);
        refreshListButton.setOnClickListener(this);
        startPerformanceTestButton.setOnClickListener(this);
        hubConnectorStatusListener = this;
    }

    public void connectHub() {
        /**
         * Establishes a connection to the ASAPHub.
         */

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // background task
            try {
                hubConnector = SharedTCPChannelConnectorPeerSide.createTCPHubConnector(hostName, port, multiChannel);
                hubConnector.addStatusListener(hubConnectorStatusListener);
                hubConnector.connectHub(peerId);
            } catch (IOException | ASAPException e) {
                throw new RuntimeException(e);
            }
            handler.post(() -> {
                //UI Thread work here
            });
        });

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.connectButtonASAPHubE2E) {
            multiChannel = multichannelSwitch.isActivated();
            hostName = hostNameEditText.getText().toString();
            peerId = peerIdEditText.getText().toString();
            try {
                port = Integer.parseInt(portEditText.getText().toString());
                connectHub();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "port number wasn't set", Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.disconnectButtonASAPHubE2E) {
            Log.d("HUBE2E", "disconnect Hub");

        } else if (id == R.id.refreshPeerListButton) {
            Runnable r = () -> {
                try {
                    // sync hub information; method "notifySynced" is called after information from
                    // hub were received
                    hubConnector.syncHubInformation();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            new Thread(r).start();
        } else if (id == R.id.startPerformanceTestButton) {
            Log.d("HUBE2E", "start performance test");

        }
    }

    /**
     * Gets all registered peer from ASAPHub and displays them using a ListView.
     * This method is called after a connection to the Hub was established or the refresh button was
     * clicked. Needs to be synchronized to prevent race condition if the method "notifySynced"
     * multiple times by the HubConnector.
     */
    public synchronized void getRegisteredPeers() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        availablePeers.clear();

        executor.execute(() -> {
            // background task
            try {
                for (CharSequence peer : hubConnector.getPeerIDs()) {
                    availablePeers.add(peer.toString());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            handler.post(() -> {
                //UI Thread work here
                adapter.notifyDataSetChanged();
            });
        });
    }

    /**
     * Called if connection establishment to ASAPHub was successful
     */
    @Override
    public void notifyConnectedAndOpen() {
        // is called by HubConnector which is not running on the UiThread
            runOnUiThread(() -> {
                connectButton.setEnabled(false);
                hostNameEditText.setEnabled(false);
                portEditText.setEnabled(false);
                peerIdEditText.setEnabled(false);

                disconnectButton.setEnabled(true);
            });
    }

    /**
     * Called by HubConnector if information with ASAPHub were synced.
     */
    @Override
    public void notifySynced(Connector connector, boolean b) {
        // is called by HubConnector which is not running on the UiThread
        runOnUiThread(this::getRegisteredPeers);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("HUBE2E", "clicked item: "+ availablePeers.get(position));
        multichannelSwitch.setEnabled(true);
        startPerformanceTestButton.setEnabled(true);

    }
}
