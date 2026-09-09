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
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(android.R.id.text1)
        val tvTimestamp: TextView = view.findViewById(android.R.id.text2)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when (message.type) {
            "CHAT" -> if (message.from == currentUsername) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            else -> VIEW_TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                MessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                MessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                MessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeString = timeFormat.format(Date(message.timestamp))

        when (message.type) {
            "CHAT" -> {
                val prefix = if (message.from == currentUsername) "You" else message.from
                holder.tvContent.text = "$prefix: ${message.content}"
                holder.tvTimestamp.text = timeString
                holder.tvTimestamp.visibility = View.VISIBLE
            }
            "JOIN" -> {
                holder.tvContent.text = "${message.from} joined the chat"
                holder.tvContent.alpha = 0.6f
                holder.tvTimestamp.visibility = View.GONE
            }
            "LEAVE" -> {
                holder.tvContent.text = "${message.from} left the chat"
                holder.tvContent.alpha = 0.6f
                holder.tvTimestamp.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
