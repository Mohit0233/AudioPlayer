package com.example.audioplayer

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.audio_list_layout.view.*
import kotlin.math.roundToInt

class MyAdapter(
    private val audioList: List<AudioItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val titleTextView: TextView = itemView.titleTextRecyclerView
        val sizeTextView: TextView = itemView.sizeTextRecyclerView
        val albumTextView: TextView = itemView.albumTextRecyclerView
        val artistTextView: TextView = itemView.artistTextRecyclerView
        val thumbnail: ImageView = itemView.thumbnail

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position: Int = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {

        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.audio_list_layout, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentItem = audioList[position]

        holder.titleTextView.text = currentItem.titleP1
        holder.artistTextView.text = currentItem.artistP5
        var size: Any = currentItem.sizeP2
        size = if (size.toString().toFloat() / 1024 > 1) {
            if (size.toString().toFloat() / 1048576 > 1) {
                (((size.toString().toFloat() / 1024) / 1024 * 100).roundToInt()
                    .toFloat() / 100).toString() + " MB"
            } else {
                ((size.toString().toFloat() / 1024 * 100).roundToInt()
                    .toFloat() / 100).toString() + " KB"
            }
        } else {
            "$size B"
        }
        holder.sizeTextView.text = size
        holder.albumTextView.text = currentItem.albumP4
        Log.e("Album Art", currentItem.albumArtP6.toString())
        holder.thumbnail.setImageResource(R.drawable.ic_music)

        if (position == listener.selectedPosition) {
            holder.apply {
                titleTextView.setTextColor(Color.parseColor("#FFEA3448"))
                artistTextView.setTextColor(Color.parseColor("#FFEA3448"))
                sizeTextView.setTextColor(Color.parseColor("#FFEA3448"))
                albumTextView.setTextColor(Color.parseColor("#FFEA3448"))
            }
        } else if (position == listener.previousPosition) {
            holder.apply {
                titleTextView.setTextColor(Color.parseColor("#FFFFFFFF"))
                artistTextView.setTextColor(Color.parseColor("#FFFFFFFF"))
                sizeTextView.setTextColor(Color.parseColor("#FFFFFFFF"))
                albumTextView.setTextColor(Color.parseColor("#FFFFFFFF"))
            }
        }
    }

    override fun getItemCount() = audioList.size

    interface OnItemClickListener {
        var selectedPosition: Int
        var previousPosition: Int
        fun onItemClick(position: Int)
    }
}