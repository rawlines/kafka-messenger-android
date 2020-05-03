package com.github.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.R;
import com.github.db.contact.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatFragmentRecyclerAdapter extends RecyclerView.Adapter<ChatFragmentRecyclerAdapter.ViewHolder> {
    private ArrayList<ChatData> contacts = new ArrayList<>();
    private Consumer<View> callback;

    public static class ChatData {
        Contact contact;
        String lastMessage;

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
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatData currentInfo = contacts.get(position);

        TextView last = holder.itemView.findViewById(R.id.last_message);
        TextView alias = holder.itemView.findViewById(R.id.chat_alias);
        TextView userName = holder.itemView.findViewById(R.id.chat_username);
        CardView card = holder.itemView.findViewById(R.id.chat_cardview);

        userName.setText(currentInfo.contact.username);
        alias.setText(currentInfo.contact.alias);
        last.setText(currentInfo.lastMessage);
        card.setOnClickListener(this.callback::accept);
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
}
