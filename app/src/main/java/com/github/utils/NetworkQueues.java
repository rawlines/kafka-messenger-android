package com.github.utils;

import com.github.db.conversation.ConversationMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkQueues {
      public static ConcurrentLinkedQueue<ConversationMessage> producedWithoutAck  = new ConcurrentLinkedQueue<>();
}
