package com.github.utils;

import android.util.Log;

import com.github.db.conversation.ConversationMessage;
import com.rest.net.AcknPacket;
import com.rest.net.AuthPacket;
import com.rest.net.CreaPacket;
import com.rest.net.KeepAlivePacket;
import com.rest.net.Packet;
import com.rest.net.Packet.PacketType;
import com.rest.net.PacketWriter;
import com.rest.net.ProducePacket;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PublicWriter {
    /**
     * Thread safe, used to store pending ACKs for messages from server.
     */
    public static ConcurrentLinkedQueue<ConversationMessage> producedWithoutAck  = new ConcurrentLinkedQueue<>();

    private PacketWriter pWriter;

    private class SenderRunnable implements Runnable {
        private Packet p;
        public SenderRunnable(Packet p) {
            this.p = p;
        }
        @Override
        public void run() {
            try {
                if (p != null && pWriter != null)
                    pWriter.sendPacket(p);
            } catch (IOException e) {
                //not sent
            }
        }
    }

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
        Log.d("PROD", "Producing: " + topic );
        ProducePacket prod = new ProducePacket(topic, content);
        process(prod);
    }

    public void sendCREA(String user, String password) {
        CreaPacket crea = new CreaPacket(user, password);
        process(crea);
    }

    private void process(Packet packet) {
        new Thread(new SenderRunnable(packet)).start();
    }
}
