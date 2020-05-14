package com.github.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.activities.MainActivity;
import com.github.R;
import com.github.activities.ChatActivity;
import com.github.db.contact.Contact;
import com.github.db.conversation.ConversationMessage;
import com.github.ui.adapters.ChatFragmentRecyclerAdapter;
import com.github.ui.adapters.ChatFragmentRecyclerAdapter.ChatData;
import com.github.ui.adapters.ContactsFragmentRecyclerAdapter;
import com.github.crypto.Cryptography;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.VIBRATOR_SERVICE;

public class RecyclerViewFragment extends Fragment {
    private RecyclerView contactsRecyclerView;
    private RecyclerView chatsRecyclerView;

    private static ContactsFragmentRecyclerAdapter mContactAdapter;
    private static ChatFragmentRecyclerAdapter mChatAdapter;

    private MainActivity mainActivity;

    private int tabResource;

    //Handler for filling the contacts fragment
    private final Handler contactPutter = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            ArrayList<Contact> info = (ArrayList<Contact>) msg.getData().getSerializable("contacts");
            mContactAdapter.setAll(info);
        }
    };

    //Handler for filling the chats fragment
    private final Handler chatPutter = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            ArrayList<ChatData> info = (ArrayList<ChatData>) msg.getData().getSerializable("chats");
            mChatAdapter.setAll(info);
        }
    };

    private final Runnable chatsFetchRunnable = () -> {
        ArrayList<ChatData> info = new ArrayList<>();
        ArrayList<Contact> contacts = new ArrayList<>(MainActivity.databaseManager.getChats());

        //Transforms all the contact into into ChatData array for displaying wonderful
        if (!contacts.isEmpty()) {
            for (Contact c : contacts) {
                ConversationMessage currentMsg =
                      MainActivity.databaseManager.getLastConversationMessage(c.username);

                try {
                    String plain = Cryptography.decryptBytes(currentMsg.content).plain;
                    info.add(new ChatData(c, plain));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Bundle b = new Bundle();
            b.putSerializable("chats", info);
            Message msg = new Message();
            msg.setData(b);
            chatPutter.sendMessage(msg);
        }
    };

    private final Runnable contactsFetchRunnable = () -> {
        ArrayList<Contact> contacts = new ArrayList<>(MainActivity.databaseManager.getContacts());
        if (contacts.isEmpty())
            return;

        Bundle b = new Bundle();
        b.putSerializable("contacts", contacts);
        Message msg = new Message();
        msg.setData(b);
        contactPutter.sendMessage(msg);
    };

    public RecyclerViewFragment(int tabResource, MainActivity mainActivity) {
        this.tabResource = tabResource;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        switch (tabResource) {
            case R.string.tab_chats:
                chatsRecyclerView = view.findViewById(R.id.recycler_view);
                chatsRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
                break;
            case  R.string.tab_contacts:
                contactsRecyclerView = view.findViewById(R.id.recycler_view);
                contactsRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        MainActivity.databaseManager.removeConversationCallback("chatListener", null);
        MainActivity.databaseManager.removeConversationCallback("contactListener", null);
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
        mChatAdapter = new ChatFragmentRecyclerAdapter(this::onChatClick, this::onChatLongClick);
        chatsRecyclerView.setAdapter(mChatAdapter);
        new Thread(chatsFetchRunnable).start();

        //Listener for incoming messages
        MainActivity.databaseManager.addConversationCallback("chatListener", null, (msg) -> {
            if (msg.conversation == null)
                return;

            if (!msg.conversation.equals(MainActivity.currentConversation))
                mChatAdapter.markConversationAsUnread(msg);
        });
    }

    private void contactsFragment() {
        mContactAdapter = new ContactsFragmentRecyclerAdapter(this::onContactClick, this::onContactLongClick);
        contactsRecyclerView.setAdapter(mContactAdapter);
        new Thread(contactsFetchRunnable).start();

        MainActivity.databaseManager.addConversationCallback("contactListener",null, (msg) -> {
            if (msg.conversation == null)
                return;

            if (!msg.conversation.equals(MainActivity.currentConversation))
                mContactAdapter.removeContactFromList(msg.conversation);
        });

    }

    private boolean contactSelection = false;
    private List<Contact> selectedContacts = new ArrayList<>();
    private boolean onContactClick(View v, Contact c) {
        if (!contactSelection) {
            TextView username = v.findViewById(R.id.contact_username);
            TextView alias = v.findViewById(R.id.contact_alias);
            MainActivity.currentConversation = username.getText().toString();

            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("alias", alias.getText().toString());

            startActivity(intent);
        } else {
            boolean selected = v.isSelected();
            if (!selected) {
                v.setSelected(true);
                v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.primaryDarkColor, mainActivity.getTheme())));
                selectedContacts.add(c);
            } else {
                v.setSelected(false);
                v.setBackgroundTintList(null);
                selectedContacts.remove(c);

                if (selectedContacts.isEmpty()) {
                    contactSelection = false;
                    mainActivity.deleteMenuButton.setVisible(false);
                }
            }
        }

        return true;
    }

    private boolean onContactLongClick(View v, Contact c) {
        startSelectionMode();
        contactSelection = true;
        return false;
    }


    //--------------------------------Chat callbacks
    private boolean conversationSelection = false;
    private List<ChatData> selectedConversations = new ArrayList<>();
    private boolean onChatClick(CardView v, ChatData d) {
        if (!conversationSelection) {
            TextView username = v.findViewById(R.id.chat_username);
            TextView alias = v.findViewById(R.id.chat_alias);
            MainActivity.currentConversation = username.getText().toString();

            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("alias", alias.getText().toString());

            startActivity(intent);
        } else {
            boolean selected = v.isSelected();
            if (!selected) {
                v.setSelected(true);
                v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.primaryDarkColor, mainActivity.getTheme())));
                selectedConversations.add(d);
            } else {
                v.setSelected(false);
                v.setBackgroundTintList(null);
                selectedConversations.remove(d);

                if (selectedConversations.isEmpty()) {
                    conversationSelection = false;
                    mainActivity.deleteMenuButton.setVisible(false);
                }
            }
        }

        return true;
    }

    private boolean onChatLongClick(View v, ChatData d) {
        startSelectionMode();
        conversationSelection = true;
        return false;
    }


    //------------------------------------------------common
    private void startSelectionMode() {
        Vibrator vibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null)
            vibrator.vibrate(VibrationEffect.createOneShot(50,50));

        mainActivity.deleteMenuButton.setVisible(true);
        mainActivity.deleteMenuButton.setOnMenuItemClickListener((item) -> {
            deleteCoroutine();
            return true;
        });
    }

    private Handler contactDeleted = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            mContactAdapter.removeContacts(selectedContacts);
            selectedContacts.clear();
            contactSelection = false;
            mainActivity.deleteMenuButton.setVisible(false);
        }
    };

    private Handler conversationDeleted = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            mChatAdapter.removeConversations(selectedConversations);
            selectedConversations.forEach((data) -> {
                mContactAdapter.add(data.contact);
            });

            selectedConversations.clear();
            conversationSelection = false;
            mainActivity.deleteMenuButton.setVisible(false);
        }
    };

    private void deleteCoroutine() {
        if (contactSelection) {
            if (selectedContacts.isEmpty())
                return;

            new Thread(() -> {
                selectedContacts.forEach((contact) -> {
                    MainActivity.databaseManager.deleteContact(contact.username);
                });

                contactDeleted.sendMessage(new Message());
            }).start();

        } else if (conversationSelection) {
            if (selectedConversations.isEmpty())
                return;

            new Thread(() -> {
                selectedConversations.forEach((data) -> {
                    MainActivity.databaseManager.removeConversations(data.contact.username);
                });

                conversationDeleted.sendMessage(new Message());
            }).start();
        }
    }
}
