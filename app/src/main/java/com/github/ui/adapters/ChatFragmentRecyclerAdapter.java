package com.github.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.MainActivity;
import com.github.R;
import com.github.db.contact.Contact;
import com.github.db.conversation.ConversationMessage;
import com.github.utils.Cryptography;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class ChatFragmentRecyclerAdapter extends RecyclerView.Adapter<ChatFragmentRecyclerAdapter.ViewHolder> {
    private ArrayList<ChatData> contacts = new ArrayList<>();
    private Consumer<View> callback;

    public static class ChatData {
        Contact contact;
        String lastMessage;
        boolean neew = false;

        public ChatData(Contact contact, String lastMessage) {
            this.contact = contact;
            this.lastMessage = lastMessage;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }

    public ChatFragmentRecyclerAdapter(Consumer<View> c) {
        this.callback = c;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatview_recyclerview, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatData currentInfo = contacts.get(position);

        TextView last = holder.itemView.findViewById(R.id.last_message);
        TextView alias = holder.itemView.findViewById(R.id.chat_alias);
        TextView userName = holder.itemView.findViewById(R.id.chat_username);
        CardView card = holder.itemView.findViewById(R.id.chat_cardview);
        ImageView notification = holder.itemView.findViewById(R.id.notification_dot);

        userName.setText(currentInfo.contact.username);
        alias.setText(currentInfo.contact.alias);
        last.setText(currentInfo.lastMessage);

        //check unread
        if (currentInfo.contact.unread || currentInfo.neew)
            notification.setVisibility(View.VISIBLE);
        else
            notification.setVisibility(View.INVISIBLE);

        card.setOnClickListener((view) -> {
            new Thread(() -> MainActivity.databaseManager.setContactUnread(currentInfo.contact.username, false)).start();
            this.callback.accept(view);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setAll(List<ChatData> contacts) {
        this.contacts.clear();
        this.contacts.addAll(contacts);
        this.notifyDataSetChanged();
    }

    public void updateChat(ConversationMessage msg) {
        int indx = 0;
        boolean found = false;
        Iterator<ChatData> iter = contacts.iterator();
        while (iter.hasNext() && !found) {
            ChatData currentData = iter.next();
            if (msg.conversation.equals(currentData.contact.username)) {
                currentData.lastMessage = Cryptography.parseCrypted(msg.content).plain;

                //set unread,
                //also, variable for making immediatly changes is used, as a query to the database may take longer than the ui refreshes
                new Thread(() -> MainActivity.databaseManager.setContactUnread(msg.conversation, true)).start();
                currentData.neew = true;

                found = true;
            } else {
                indx++;
            }
        }
        this.notifyItemChanged(indx);
    }
}
