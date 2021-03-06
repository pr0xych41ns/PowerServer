package de.poweruser.powerserver.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import de.poweruser.powerserver.logger.LogLevel;
import de.poweruser.powerserver.logger.Logger;
import de.poweruser.powerserver.main.PowerServer;

public class UDPSender {

    private DatagramSocket udpSocket;
    private HashMap<SocketAddress, DatagramPacket> queries;
    private HashMap<SocketAddress, DatagramPacket> broadcasts;

    public UDPSender(DatagramSocket udpSocket) {
        this.queries = new HashMap<SocketAddress, DatagramPacket>();
        this.broadcasts = new HashMap<SocketAddress, DatagramPacket>();
        this.udpSocket = udpSocket;
    }

    public void queueHeartBeatBroadcast(List<InetAddress> masterServers, DatagramPacket packet) {
        if(packet != null) {
            for(InetAddress ms: masterServers) {
                this.broadcasts.put(new InetSocketAddress(ms, PowerServer.MASTERSERVER_UDP_PORT), packet);
            }
        }
    }

    public void queueQuery(InetSocketAddress server, DatagramPacket packet) {
        packet.setAddress(server.getAddress());
        packet.setPort(server.getPort());
        this.queries.put(server, packet);
    }

    public void flush() {
        for(DatagramPacket packet: this.queries.values()) {
            try {
                this.udpSocket.send(packet);
            } catch(IOException e) {
                Logger.logStatic(LogLevel.VERY_LOW, e.toString() + "\nFailed to send a server query to " + packet.getSocketAddress().toString() + " - Content: " + new String(packet.getData()));
            }
        }
        this.queries.clear();
        for(Entry<SocketAddress, DatagramPacket> entry: this.broadcasts.entrySet()) {
            DatagramPacket packet = entry.getValue();
            try {
                packet.setSocketAddress(entry.getKey());
                this.udpSocket.send(packet);
            } catch(IOException e) {
                Logger.logStatic(LogLevel.VERY_LOW, e.toString() + "\nFailed to send a heartbeatbroadcast to a masterserver at " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ". Content: " + new String(packet.getData()));
            }
        }
        this.broadcasts.clear();
    }
}
