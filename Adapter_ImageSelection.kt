package com.example.finalproject

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.initials.BoardSize
import kotlin.math.min

class Adapter_ImageSelection(
    private val context: Context,
    private val imagesuris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageclicklistener: ImageClickListener
)
    : RecyclerView.Adapter<Adapter_ImageSelection.ViewHolder>() {

    companion object {
        private const val CODE = "Adapter_ImageSelection"
    }

    interface ImageClickListener {
        fun placeholderclicker()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.i(CODE, "onCreateViewHolder")
        val view = LayoutInflater.from(context).inflate(R.layout.card_images, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.findHeight()
        val cardSideLength = min(cardWidth, cardHeight)
        Log.i(CODE, "onCreateViewHolder $cardSideLength, $cardWidth, $cardHeight")
        val layoutParams = view.findViewById<ImageView>(R.id.ivcustomimage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.findNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < imagesuris.size) {
            holder.bind(imagesuris[position])
        } else {
            holder.bind()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val customimage = itemView.findViewById<ImageView>(R.id.ivcustomimage)

        fun bind() {
            customimage.setOnClickListener {
                imageclicklistener.placeholderclicker()
            }
        }

        fun bind(uri: Uri) {
            customimage.setImageURI(uri)
            customimage.setOnClickListener(null)
        }
    }

}