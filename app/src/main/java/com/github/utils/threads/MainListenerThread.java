package com.github.utils.threads;

import android.util.Log;

import com.github.MainActivity;
import com.github.db.conversation.ConversationMessage;
import com.github.utils.NetworkQueues;
import com.github.utils.PublicWriter;
import com.rest.net.AcknPacket;
import com.rest.net.ConsumePacket;
import com.rest.net.Packet;
import com.rest.net.Packet.PacketType;
import com.rest.net.PacketReader;
import com.rest.net.PacketWriter;

import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MainListenerThread extends Thread {
      private final short RETRY_TIMEOUT = 5000;
      private final short KEEPALIVE_TIMEOUT = 5000;
      private final short CHECKUNSENT_TIMEOUT = 30000;

      private KeyStore keyStore;
      private KeyStore trustStore;

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
            NetworkQueues.producedWithoutAck.clear();
            NetworkQueues.producedWithoutAck.addAll(unsuccess);
            try {
                  while (!Thread.interrupted()) {
                        Log.d("CONS", "Without ACK:" + NetworkQueues.producedWithoutAck.size());
                        for (ConversationMessage msg : NetworkQueues.producedWithoutAck) {
                              MainActivity.publicWriter.sendPROD(msg.conversation, msg.content);
                        }
                        Thread.sleep(CHECKUNSENT_TIMEOUT);
                  }
            } catch (Exception ignored) {}
      };

      public MainListenerThread(KeyStore keyStore, KeyStore trustStore) {
            this.keyStore = keyStore;
            this.trustStore = trustStore;
      }

      @Override
      public void run() {
            while (true) {
                  try {
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
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "123456".toCharArray());

            SSLContext sslctx = SSLContext.getInstance("TLSv1.2");
            sslctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            SSLSocketFactory factory = sslctx.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket("192.168.0.100", 8081);

            this.pReader = new PacketReader(socket.getInputStream());
            MainActivity.publicWriter = new PublicWriter(new PacketWriter(socket.getOutputStream()));

            Log.d("CONS", "Succesfully connected");
      }

      /**
       * Method for sending an {@link com.rest.net.AuthPacket} to the server
       *
       * @throws IOException - Stream errors
       */
      private void sendAuth() throws IOException {
            MainActivity.publicWriter.sendAUTH(MainActivity.username, MainActivity.password);
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
            if (p.getCommand() != PacketType.PROD)
                  return;

            ConversationMessage msg = NetworkQueues.producedWithoutAck.poll();
            if (msg != null) {
                  MainActivity.databaseManager.setMessageAsSuccess(msg);
                  Log.d("CONS", "ACK " + msg.conversation + " " +  msg.timestamp);
            }
      }
}
