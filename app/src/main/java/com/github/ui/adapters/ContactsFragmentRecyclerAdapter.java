package com.github.ui.adapters;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.MainActivity;
import com.github.R;
import com.github.activities.ChatActivity;
import com.github.db.contact.Contact;
import com.github.ui.fragments.RecyclerViewFragment;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ContactsFragmentRecyclerAdapter extends RecyclerView.Adapter<ContactsFragmentRecyclerAdapter.ViewHolder> {
    private ArrayList<Contact> contacts = new ArrayList<>();
    private Consumer<View> callback;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }

    public ContactsFragmentRecyclerAdapter(Consumer<View> c) {
        this.callback = c;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contactview_recyclerview, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact currentContact = contacts.get(position);

        TextView userName = holder.itemView.findViewById(R.id.contact_username);
        TextView alias = holder.itemView.findViewById(R.id.contact_alias);
        CardView card = holder.itemView.findViewById(R.id.contact_cardview);

        alias.setText(currentContact.alias);
        userName.setText(currentContact.username);
        card.setOnClickListener(this.callback::accept);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void setAll(List<Contact> contacts) {
        this.contacts.clear();
        this.contacts.addAll(contacts);
        this.notifyDataSetChanged();
    }
}
