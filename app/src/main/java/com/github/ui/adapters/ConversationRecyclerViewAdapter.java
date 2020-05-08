package com.github.ui.adapters;

import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.github.R;
import com.github.db.conversation.ConversationMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.github.db.conversation.ConversationMessage.RECEIBED;
import static com.github.db.conversation.ConversationMessage.SENT;

public class ConversationRecyclerViewAdapter extends RecyclerView.Adapter<ConversationRecyclerViewAdapter.ViewHolder> {
    private List<ConversationMessage> messages = new ArrayList<>();
    private RecyclerClickListener onLongClick;
    private RecyclerClickListener onClick;

    public interface RecyclerClickListener {
        boolean action(View v, ConversationMessage msg);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }

    public ConversationRecyclerViewAdapter(RecyclerClickListener longCLickListener, RecyclerClickListener clickListener) {
        this.onLongClick = longCLickListener;
        this.onClick = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bubble_conversation_message_layout, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationMessage currentMsg = messages.get(position);
        boolean isSender = currentMsg.messageType == SENT;
        TextView messageText;
        RelativeLayout bubble_quote;
        ConstraintLayout bubble_sender = holder.itemView.findViewById(R.id.bubble_sender);
        ConstraintLayout bubble_receiber = holder.itemView.findViewById(R.id.bubble_receiber);
        ImageView status_icon = holder.itemView.findViewById(R.id.message_status);

        if (isSender)  {
            bubble_quote = holder.itemView.findViewById(R.id.bubble_sender_quote);
            bubble_receiber.setVisibility(View.GONE);
            bubble_sender.setVisibility(View.VISIBLE);
            messageText = holder.itemView.findViewById(R.id.bubble_sender_text_message);
            status_icon.setVisibility(View.VISIBLE);

            if (currentMsg.success) {
                status_icon.setImageIcon(Icon.createWithResource("com.github", R.drawable.ic_done_24px));
            } else {
                status_icon.setImageIcon(Icon.createWithResource("com.github", R.drawable.ic_clock_24px));
            }

            if (position == 0 || messages.get(position - 1).messageType != SENT) {
                bubble_quote.setVisibility(View.VISIBLE);
            } else {
                bubble_quote.setVisibility(View.GONE);
            }

            CardView sender_card = holder.itemView.findViewById(R.id.sender_card);
            sender_card.setOnLongClickListener((v) -> this.onLongClick.action(v, currentMsg));
            sender_card.setOnClickListener((v) -> this.onClick.action(v, currentMsg));
        } else {
            bubble_quote = holder.itemView.findViewById(R.id.bubble_receiber_quote);
            bubble_receiber.setVisibility(View.VISIBLE);
            bubble_sender.setVisibility(View.GONE);
            messageText = holder.itemView.findViewById(R.id.bubble_receiber_text_message);
            status_icon.setVisibility(View.GONE);

            if (position == 0 || messages.get(position - 1).messageType != RECEIBED) {
                bubble_quote.setVisibility(View.VISIBLE);
            } else {
                bubble_quote.setVisibility(View.GONE);
            }

            CardView receiver_card = holder.itemView.findViewById(R.id.receiver_card);
            receiver_card.setOnLongClickListener((v) -> this.onLongClick.action(v, currentMsg));
            receiver_card.setOnClickListener((v) -> this.onClick.action(v, currentMsg));
        }

        //here will have to decrypt
        messageText.setText(new String(currentMsg.content, StandardCharsets.ISO_8859_1));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setAsSuccess(ConversationMessage msg) {
        Iterator<ConversationMessage> iter = this.messages.iterator();
        int i = 0;
        while (iter.hasNext()) {
            ConversationMessage m = iter.next();
            if (m.timestamp == msg.timestamp && m.messageType == msg.messageType) {
                m.success = true;
                break;
            }
            i++;
        }
        this.notifyItemChanged(i);
    }

    public void addMessage(ConversationMessage message) {
        this.messages.add(message);
        this.notifyItemInserted(messages.size() - 1);
    }

    public void addMessages(List<ConversationMessage> messages) {
        int start = this.messages.size();
        this.messages.addAll(messages);
        this.notifyItemRangeInserted(start, messages.size());
    }

    public void removeIndexes(List<ConversationMessage> messages) {
        messages.forEach((msg) -> {
            int i = this.messages.indexOf(msg);
            this.messages.remove(i);
            this.notifyItemRemoved(i);
        });
    }
}
