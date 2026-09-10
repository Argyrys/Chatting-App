package com.chat.app

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.URL
import java.util.concurrent.TimeUnit

class ChatAdapter(
    private val messages: List<Message>,
    private val currentUsername: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT_TEXT = 1
        private const val VIEW_TYPE_RECEIVED_TEXT = 2
        private const val VIEW_TYPE_SYSTEM = 3
        private const val VIEW_TYPE_SENT_MEDIA = 4
        private const val VIEW_TYPE_RECEIVED_MEDIA = 5
    }

    class SentTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    class ReceivedTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSenderName)
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvSystemMessage)
    }

    class SentMediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMedia: ImageView = view.findViewById(R.id.ivMedia)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    class ReceivedMediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSenderName)
        val ivMedia: ImageView = view.findViewById(R.id.ivMedia)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val isMedia = message.messageType != "TEXT" && message.fileUrl.isNotEmpty()
        return when {
            message.type == "CHAT" && isMedia && message.from == currentUsername -> VIEW_TYPE_SENT_MEDIA
            message.type == "CHAT" && isMedia -> VIEW_TYPE_RECEIVED_MEDIA
            message.type == "CHAT" && message.from == currentUsername -> VIEW_TYPE_SENT_TEXT
            message.type == "CHAT" -> VIEW_TYPE_RECEIVED_TEXT
            else -> VIEW_TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> SentTextViewHolder(
                inflater.inflate(R.layout.item_message_sent, parent, false)
            )
            VIEW_TYPE_RECEIVED_TEXT -> ReceivedTextViewHolder(
                inflater.inflate(R.layout.item_message_received, parent, false)
            )
            VIEW_TYPE_SENT_MEDIA -> SentMediaViewHolder(
                inflater.inflate(R.layout.item_message_sent_media, parent, false)
            )
            VIEW_TYPE_RECEIVED_MEDIA -> ReceivedMediaViewHolder(
                inflater.inflate(R.layout.item_message_received_media, parent, false)
            )
            else -> SystemViewHolder(
                inflater.inflate(R.layout.item_message_system, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val timeString = getRelativeTime(message.timestamp)
        val baseUrl = "http://10.0.2.2:8080"

        when (holder) {
            is SentTextViewHolder -> {
                holder.tvContent.text = message.content
                holder.tvTimestamp.text = timeString
            }
            is ReceivedTextViewHolder -> {
                holder.tvSender.text = message.from
                holder.tvContent.text = message.content
                holder.tvTimestamp.text = timeString
            }
            is SentMediaViewHolder -> {
                holder.tvTimestamp.text = timeString
                bindMedia(holder.ivMedia, holder.tvFileName, holder.tvContent, message, baseUrl)
            }
            is ReceivedMediaViewHolder -> {
                holder.tvSender.text = message.from
                holder.tvTimestamp.text = timeString
                bindMedia(holder.ivMedia, holder.tvFileName, holder.tvContent, message, baseUrl)
            }
            is SystemViewHolder -> {
                val text = when (message.type) {
                    "JOIN" -> message.content.ifEmpty { "${message.from} joined the chat" }
                    "LEAVE" -> message.content.ifEmpty { "${message.from} left the chat" }
                    else -> message.content
                }
                holder.tvMessage.text = text
            }
        }
    }

    private fun bindMedia(ivMedia: ImageView, tvFileName: TextView, tvContent: TextView, message: Message, baseUrl: String) {
        val fullUrl = "$baseUrl${message.fileUrl}"

        when (message.messageType) {
            "IMAGE" -> {
                ivMedia.visibility = View.VISIBLE
                tvContent.visibility = View.GONE
                tvFileName.visibility = View.GONE
                loadBitmap(ivMedia, fullUrl)
            }
            "VIDEO" -> {
                ivMedia.visibility = View.VISIBLE
                tvFileName.visibility = View.VISIBLE
                tvContent.visibility = View.GONE
                tvFileName.text = "Video: ${message.fileName}"
                ivMedia.setImageResource(android.R.drawable.ic_media_play)
            }
            "AUDIO" -> {
                ivMedia.visibility = View.GONE
                tvFileName.visibility = View.VISIBLE
                tvContent.visibility = View.GONE
                tvFileName.text = "Audio: ${message.fileName}"
            }
            else -> {
                ivMedia.visibility = View.GONE
                tvFileName.visibility = View.VISIBLE
                tvContent.visibility = View.GONE
                tvFileName.text = "File: ${message.fileName}"
            }
        }

        if (message.content.isNotEmpty() && message.messageType != "IMAGE") {
            tvContent.visibility = View.VISIBLE
            tvContent.text = message.content
        }
    }

    private fun loadBitmap(imageView: ImageView, url: String) {
        Thread {
            try {
                val connection = URL(url).openConnection()
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                imageView.post {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                imageView.post {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }.start()
    }

    override fun getItemCount(): Int = messages.size

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.SECONDS.toMillis(1) -> "now"
            diff < TimeUnit.MINUTES.toMillis(1) -> "${TimeUnit.MILLISECONDS.toSeconds(diff)}s"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
            else -> "${TimeUnit.MILLISECONDS.toDays(diff)}d"
        }
    }
}
