package net.sharksystem.util.tcp;

public interface TCPChannelMakerListener {
    public void onConnectionEstablished(TCPChannel channel);

    public void onConnectionEstablishmentFailed(TCPChannel channel, String reason);

}
