package com.github.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.MainActivity;
import com.github.R;
import com.github.activities.ChatActivity;
import com.github.db.contact.Contact;
import com.github.db.conversation.ConversationMessage;
import com.github.ui.adapters.ChatFragmentRecyclerAdapter;
import com.github.ui.adapters.ChatFragmentRecyclerAdapter.ChatData;
import com.github.ui.adapters.ContactsFragmentRecyclerAdapter;
import com.github.utils.Cryptography;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewFragment extends Fragment {
    private RecyclerView recyclerView;
    private ContactsFragmentRecyclerAdapter mContactAdapter;
    private ChatFragmentRecyclerAdapter mChatAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private int tabResource;

    private boolean resumed = false;

    private Runnable chatsFetchRunnable = () -> {
        List<ChatData> info = new ArrayList<>();
        List<Contact> contacts = MainActivity.databaseManager.getChats();
        if (contacts != null) {
            for (Contact c : contacts) {
                ConversationMessage currentMsg =
                      MainActivity.databaseManager.getConversationMessages(c.lastMessageTime, c.lastMessageType);

                String plain = Cryptography.parseCrypted(currentMsg.content).plain;
                info.add(new ChatData(c, plain));
            }
            mChatAdapter.setAll(info);
        }
    };

    private Runnable contactsFetchRunnable = () -> {
        List<Contact> contacts = MainActivity.databaseManager.getContacts();
        if (contacts != null)
            mContactAdapter.setAll(contacts);
    };

    public RecyclerViewFragment(int tabResource) {
        this.tabResource = tabResource;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        switch (tabResource) {
            case R.string.tab_chats:
                chatsFragment();
                break;
            case  R.string.tab_contacts:
                contactsFragment();
                break;
        }
    }

    private void chatsFragment() {
        mChatAdapter = new ChatFragmentRecyclerAdapter(this::onChatClick);
        recyclerView.setAdapter(mChatAdapter);
        new Thread(chatsFetchRunnable).start();
    }

    private void contactsFragment() {
        mContactAdapter = new ContactsFragmentRecyclerAdapter(this::onContactClick);
        recyclerView.setAdapter(mContactAdapter);
        new Thread(contactsFetchRunnable).start();
    }

    private void onContactClick(View v) {
        TextView username = v.findViewById(R.id.contact_username);
        TextView alias = v.findViewById(R.id.contact_alias);
        MainActivity.currentConversation = username.getText().toString();

        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("alias", alias.getText().toString());

        startActivity(intent);
    }

    private void onChatClick(View v) {
        TextView username = v.findViewById(R.id.chat_username);
        TextView alias = v.findViewById(R.id.chat_alias);
        MainActivity.currentConversation = username.getText().toString();

        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("alias", alias.getText().toString());

        startActivity(intent);
    }
}
