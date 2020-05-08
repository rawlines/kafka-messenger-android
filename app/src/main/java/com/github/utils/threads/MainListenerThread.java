package com.github.utils.threads;

import android.util.Log;

import com.github.activities.MainActivity;
import com.github.db.conversation.ConversationMessage;
import com.github.utils.PublicWriter;
import com.github.utils.SSLFactory;
import com.rest.net.AcknPacket;
import com.rest.net.ConsumePacket;
import com.rest.net.Packet;
import com.rest.net.Packet.PacketType;
import com.rest.net.PacketReader;
import com.rest.net.PacketWriter;

import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class MainListenerThread extends Thread {
      private final short RETRY_TIMEOUT = 5000;
      private final short KEEPALIVE_TIMEOUT = 5000;
      private final short CHECKUNSENT_TIMEOUT = 30000;

      private PacketReader pReader;

      private Thread keepAliveThread;
      private Thread unSentMessagesThread;

      private Runnable keepAliveRunnable = () -> {
            try {
                  while (!Thread.interrupted()) {
                        Thread.sleep(KEEPALIVE_TIMEOUT);
                        MainActivity.publicWriter.sendKEEP();
                  }
            } catch (Exception ignored) { }
      };

      private Runnable unSentMessagesRunnable = () -> {
            List<ConversationMessage> unsuccess = MainActivity.databaseManager.getUnsuccessMessages();
            PublicWriter.producedWithoutAck.clear();
            PublicWriter.producedWithoutAck.addAll(unsuccess);
            try {
                  while (!Thread.interrupted()) {
                        Log.d("CONS", "Without ACK:" + PublicWriter.producedWithoutAck.size());
                        for (ConversationMessage msg : PublicWriter.producedWithoutAck) {
                              MainActivity.publicWriter.sendPROD(msg.conversation, msg.content);
                        }
                        Thread.sleep(CHECKUNSENT_TIMEOUT);
                  }
            } catch (Exception ignored) {}
      };

      @Override
      public void run() {
            while (true) {
                  try {
                        if (MainActivity.globalCredentials == null)
                              continue;

                        prepareEnvironment();
                        sendAuth();
                        prepareSubThreads();


                        while (!Thread.interrupted()) {
                              Packet packet = pReader.readPacket();

                              switch (packet.getPacketType()) {
                                    case CONS:
                                          consPacket((ConsumePacket) packet);
                                          break;
                                    case ACKN:
                                          acknPacket((AcknPacket) packet);
                                          break;
                                    default:
                                          //default
                                          break;
                              }
                        }
                  } catch (Exception e) {
                        e.printStackTrace();
                  }

                  if (keepAliveThread != null)
                        keepAliveThread.interrupt();

                  if (unSentMessagesThread != null)
                        unSentMessagesThread.interrupt();

                  Log.d("CONS","Disconnected, retrying in 5 secs...");
                  try {
                        Thread.sleep(RETRY_TIMEOUT);
                  } catch (Exception ignored) {}
            }
      }

      private void prepareSubThreads() {
            keepAliveThread = new Thread(keepAliveRunnable);
            keepAliveThread.start();

            unSentMessagesThread = new Thread(unSentMessagesRunnable);
            unSentMessagesThread.start();
      }

      /**
       * Provisional keystore credentials and server address
       * This method gets the ssl config from constructor and inits a connection with server
       *
       * @throws Exception - exception
       */
      private void prepareEnvironment() throws Exception {
            String[] splitted = MainActivity.globalCredentials.ipAddress.split(":");
            SSLSocketFactory factory = SSLFactory.getSSLFactory(MainActivity.trustStore, MainActivity.keyStore);
            SSLSocket socket = (SSLSocket) factory.createSocket(splitted[0], Integer.parseInt(splitted[1]));

            this.pReader = new PacketReader(socket.getInputStream());
            MainActivity.publicWriter = new PublicWriter(new PacketWriter(socket.getOutputStream()));

            Log.d("CONS", "Succesfully connected");
      }

      /**
       * Method for sending an {@link com.rest.net.AuthPacket} to the server
       */
      private void sendAuth() {
            MainActivity.publicWriter.sendAUTH(MainActivity.globalCredentials.username, MainActivity.globalCredentials.password);
            Log.d("CONS", "Auth packet sent");
      }

      /**
       * Handler when a {@link ConsumePacket} is received
       *
       * @param p - The packet received
       */
      private void consPacket(ConsumePacket p) {
            try {
                  Log.d("CONS", "Consuming...");
                  ConversationMessage msg = ConversationMessage.fromCryptedBytes(p.getContent());
                  msg.messageType = ConversationMessage.RECEIBED;
                  msg.success = true;

                  MainActivity.databaseManager.insertConversationMessage(msg);
            } catch (Exception e) {
                  Log.d("CONS", "Well, that msg is not mine");
                  e.printStackTrace();
            }
            MainActivity.publicWriter.sendACKN(PacketType.CONS);
      }

      /**
       * Handler when a {@link AcknPacket} is receibed
       *
       * @param p - The packet received
       */
      private void acknPacket(AcknPacket p) {
            if (p.getCommand() == PacketType.PROD) {
                  ConversationMessage msg = PublicWriter.producedWithoutAck.poll();
                  if (msg != null)
                        MainActivity.databaseManager.setMessageAsSuccess(msg);
            }
      }
}
