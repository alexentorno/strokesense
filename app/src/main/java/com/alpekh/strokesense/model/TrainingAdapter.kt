package com.alpekh.strokesense.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alpekh.strokesense.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingAdapter(
    private val onClick: (TrainingSession) -> Unit,
    private val onDelete: (TrainingSession) -> Unit
) : ListAdapter<TrainingSession, TrainingAdapter.TrainingViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.training_history_item, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session, onClick, onDelete)
    }

    class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(session: TrainingSession, onClick: (TrainingSession) -> Unit, onDelete: (TrainingSession) -> Unit) {
            itemView.findViewById<TextView>(R.id.textViewDate).text =
                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(session.startTime))

            itemView.setOnClickListener { onClick(session) }

            itemView.findViewById<ImageView>(R.id.btnDeleteSession).setOnClickListener {
                onDelete(session)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TrainingSession>() {
            override fun areItemsTheSame(oldItem: TrainingSession, newItem: TrainingSession) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TrainingSession, newItem: TrainingSession) =
                oldItem == newItem
        }
    }
}
