package com.github.utils;

import android.util.Log;

import com.rest.net.AcknPacket;
import com.rest.net.AuthPacket;
import com.rest.net.KeepAlivePacket;
import com.rest.net.Packet;
import com.rest.net.Packet.PacketType;
import com.rest.net.PacketWriter;
import com.rest.net.ProducePacket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

public class PublicWriter {
    private PacketWriter pWriter;
    private final Queue<Packet> queue = new LinkedList<>();

    private Runnable senderRunnable = new Runnable() {
        @Override
        public void run() {
            Packet p = queue.poll();
            try {
                if (p != null && pWriter != null)
                    pWriter.sendPacket(p);
            } catch (IOException e) {
                //not sent
            }
        }
    };

    public PublicWriter(PacketWriter pWriter) {
        this.pWriter = pWriter;
    }

    public void sendACKN(PacketType ackType) {
        AcknPacket ack = new AcknPacket(ackType);
        process(ack);
    }

    public void sendAUTH(String user, String pass) {
        AuthPacket auth = new AuthPacket(user, pass);
        process(auth);
    }

    public void sendKEEP() {
        KeepAlivePacket keep = new KeepAlivePacket();
        process(keep);
    }

    public void sendPROD(String topic, byte[] content) {
        ProducePacket prod = new ProducePacket(topic, content);
        process(prod);
    }

    private void process(Packet packet) {
        queue.add(packet);
        new Thread(senderRunnable).start();
    }
}
