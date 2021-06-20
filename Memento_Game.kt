package com.example.finalproject.initials

import com.example.finalproject.variables.IMAGES_FOR_THE_GAME

class Memento_Game(private val boardSize: BoardSize, customimages: List<String>?) {

    var cards: List<HiddenCard>
    var numPairsFound = 0

    private var numcardflips = 0
    private var indexOfSingleSelectedCard: Int? = null

    init {
        if (customimages == null) {
            val user_chosenimages = IMAGES_FOR_THE_GAME.shuffled().take(boardSize.findNumPairs())
            val random_images = (user_chosenimages + user_chosenimages).shuffled()
            cards = random_images.map { HiddenCard(it) }
        } else {
            val random_images = (customimages + customimages).shuffled()
            cards = random_images.map { HiddenCard(it.hashCode(), it) }
        }
    }

    fun flipCard(position: Int): Boolean {
        numcardflips++
        val card = cards[position]
        var foundMatch = false

        // Three cases may occur
        // o cards are previously flipped over
        // 1 card is previously flipped over
        // 2 cards are previously flipped over
        if (indexOfSingleSelectedCard == null) {

            // 0 or 2 cards were selected
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {

            // exactly 1 card was selected
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        card.Facing_up = !card.Facing_up
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) {
            return false
        }
        cards[position1].Matched = true
        cards[position2].Matched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards) {
            if (!card.Matched) {
                card.Facing_up = false
            }
        }
    }

    fun havewongame(): Boolean {
        return numPairsFound == boardSize.findNumPairs()
    }

    fun iscardfacingup(position: Int): Boolean {
        return cards[position].Facing_up
    }

    fun findnummoves(): Int {
    return numcardflips / 2
    }
}

