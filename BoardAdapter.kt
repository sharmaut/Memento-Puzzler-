package com.example.finalproject

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.initials.BoardSize
import com.example.finalproject.initials.HiddenCard
import com.squareup.picasso.Picasso
import kotlin.math.min

class BoardAdapter(private val context: Context, private val boardSize: BoardSize,
                   private val cards: List<HiddenCard>,
                   private val Cardclicklistener: CardClickListener) :

        RecyclerView.Adapter<BoardAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 20
        private const val CODE = "BoardAdapter"
    }

    interface CardClickListener {
        fun onCardClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth: Int = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.findHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)
        val view = LayoutInflater.from(context).inflate(R.layout.hidden_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardview).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)

        // Set the appropriate width and height of the view
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard = cards[position]
            if (memoryCard.Facing_up) {
                if (memoryCard.url_image != null) {
                    Picasso.get().load(memoryCard.url_image).placeholder(R.drawable.icimage).into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            } else {
                imageButton.setImageResource(R.drawable.code)
            }
            imageButton.setImageResource(if (memoryCard.Facing_up) memoryCard.identifier
            else R.drawable.ic_launcher_foreground)

            imageButton.alpha = if (memoryCard.Matched) .4f else 1.0f
            val colorStateList = if (memoryCard.Matched) ContextCompat.getColorStateList(context, R.color.black) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)
            imageButton.setOnClickListener{
                Log.i(CODE, "Click $position")
                Cardclicklistener.onCardClicked(position)
            }
        }
    }
}