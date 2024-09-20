package net.sharksystem.asap.android.tcpEncounter;

import android.util.Log;

import net.sharksystem.asap.android.app2serviceMessaging.MessageFactory;
import net.sharksystem.asap.android.apps.ASAPActivity;

/**
 * This class is used to start a TCP Encounter to a target peer from the application side.
 */
public class TCPEncounterManagerApplicationSide {

    private final ASAPActivity asapActivity;
    private TCPEncounterListener tcpEncounterListener;

    public TCPEncounterManagerApplicationSide(ASAPActivity asapActivity) {
        this.asapActivity = asapActivity;
    }

    public void startTCPEncounter(String host, int port) {
        // send message to service side
        Log.d(getLogStart(), "send message to service: start TCP Encounter");
        asapActivity.sendMessage2Service(MessageFactory.createConnectToServerSocketMessage(host, port));
    }

    public void setTcpEncounterListener(TCPEncounterListener tcpEncounterListener) {
        this.tcpEncounterListener = tcpEncounterListener;
    }

    public void notifyTCPEncounterSuccess() {
        Log.d(getLogStart(), "Notify TCP Encounter success");
        if (this.tcpEncounterListener != null) {
            this.tcpEncounterListener.onEncounterSuccess();
        }
    }

    private String getLogStart() {
        return this.getClass().getSimpleName();
    }
}
