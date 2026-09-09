package com.chat.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: List<Message>,
    private val currentUsername: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSenderName)
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvSystemMessage)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when (message.type) {
            "CHAT" -> if (message.from == currentUsername) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            else -> VIEW_TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(
                inflater.inflate(R.layout.item_message_sent, parent, false)
            )
            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(
                inflater.inflate(R.layout.item_message_received, parent, false)
            )
            else -> SystemViewHolder(
                inflater.inflate(R.layout.item_message_system, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = timeFormat.format(Date(message.timestamp))

        when (holder) {
            is SentViewHolder -> {
                holder.tvContent.text = message.content
                holder.tvTimestamp.text = timeString
            }
            is ReceivedViewHolder -> {
                holder.tvSender.text = message.from
                holder.tvContent.text = message.content
                holder.tvTimestamp.text = timeString
            }
            is SystemViewHolder -> {
                val text = when (message.type) {
                    "JOIN" -> "${message.from} joined the chat"
                    "LEAVE" -> "${message.from} left the chat"
                    else -> message.content
                }
                holder.tvMessage.text = text
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
