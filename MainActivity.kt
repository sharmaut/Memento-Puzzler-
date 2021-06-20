package com.example.finalproject

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.initials.BoardSize
import com.example.finalproject.initials.Memento_Game
import com.example.finalproject.initials.userimagelist
import com.example.finalproject.variables.BOARD_SIZE
import com.example.finalproject.variables.GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CODE = "MainActivity"
        private const val Create_a_request_code = 61
    }

    private lateinit var c_layout: ConstraintLayout
    private lateinit var memoryGame: Memento_Game
    private lateinit var rvboard: RecyclerView
    private lateinit var tvnummoves: TextView
    private lateinit var tvnumpairs: TextView

    private val db = Firebase.firestore
    private var gameName: String? = null
    private val firebaseAnalytics = Firebase.analytics 
    private val remoteConfig = Firebase.remoteConfig
    private var customgameimages: List<String>? = null
    private lateinit var memory_adapter: BoardAdapter
    private var boardSize: BoardSize = BoardSize.BEGINNER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        c_layout = findViewById(R.id.c_layout)
        rvboard = findViewById(R.id.rvboard)
        tvnummoves = findViewById(R.id.tvnummoves)
        tvnumpairs = findViewById(R.id.tvnumpairs)

        setup_your_board()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.homepage, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_pause -> {
                if (memoryGame.findnummoves() > 0 && !memoryGame.havewongame()) {
                    showalertdialogbox("Quit your game?", null, View.OnClickListener {
                        setup_your_board()
                    })
                } else {
                    setup_your_board()
                }
                return true
            }
            R.id.mi_gamecentre -> {
                shownewsizedialog()
                return true
            }
            R.id.mi_custom -> {
                showcreationdialog()
                return true
            }
            R.id.mi_download -> {
                showdownloaddialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Create_a_request_code && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(GAME_NAME)
            if (customGameName == null) {
                Log.e(CODE, "Got null custom game from CreateActivity")
                return
            }
            downloadgame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showdownloaddialog() {
        val boarddownloadview = LayoutInflater.from(this).inflate(R.layout.dialogdownloadboard, null)
        showalertdialogbox("Fetch memory game", boarddownloadview, View.OnClickListener {
            // Grab text
            val edittextdownloadgame = boarddownloadview.findViewById<EditText>(R.id.edittextdownloadgame)
            val gametodownload = edittextdownloadgame.text.toString().trim()
            downloadgame(gametodownload)
        })
    }

    private fun downloadgame(customGameName: String) {
        if (customGameName.isBlank()) {
            Snackbar.make(c_layout, "Game name is not blank", Snackbar.LENGTH_LONG).show()
            Log.e(CODE, "Try to retrieve empty game")
            return
        }
        firebaseAnalytics.logEvent("download game attempt") {
            param("game name", customGameName)
        }
        db.collection("games").document(customGameName!!).get().addOnSuccessListener { document ->
            val userimagelist = document.toObject(userimagelist::class.java)
            if (userimagelist?.images == null) {
                Log.e(CODE, "Invalid custom game data")
                Snackbar.make(c_layout, "Sorry, we can't find your game, '$gameName'", Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            firebaseAnalytics.logEvent("download game success") {
                param("game name", customGameName)
            }
            val numcards = userimagelist.images.size * 2
            boardSize = BoardSize.getByValue(numcards)
            customgameimages = userimagelist.images
            gameName = customGameName

            // Download images for faster game loading
            for (imageurl in userimagelist.images) {
                Picasso.get().load(imageurl).fetch()
            }
            Snackbar.make(c_layout, "You're playing '$customGameName'!", Snackbar.LENGTH_LONG).show()
            gameName = customGameName
            setup_your_board()
        }.addOnFailureListener { exception ->
            Log.e(CODE, "exception when retrieving game", exception)
        }
    }

    private fun param(s: String, customGameName: String) {

    }

    private fun showcreationdialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialogboardsize, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radiogroup)
        showalertdialogbox("Choose your own gaming level", null, View.OnClickListener {
            val desiredboardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.radiobuttonbeginner -> BoardSize.BEGINNER
                R.id.radiobuttonsemipro -> BoardSize.SEMIPRO
                else -> BoardSize.PROFESSIONAL
            }
            // Navigate to a new activity
            val intent = Intent(this, Creation::class.java)
            intent.putExtra(BOARD_SIZE, desiredboardSize)
            startActivityForResult(intent, Create_a_request_code)
        })
    }

    private fun shownewsizedialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialogboardsize, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radiogroup)

        when (boardSize) {
            BoardSize.BEGINNER -> radioGroupSize.check(R.id.radiobuttonbeginner)
            BoardSize.SEMIPRO -> radioGroupSize.check(R.id.radiobuttonsemipro)
            BoardSize.PROFESSIONAL -> radioGroupSize.check(R.id.radiobuttonprofessional)
        }
        showalertdialogbox("Choose new gameplay", boardSizeView, View.OnClickListener {
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.radiobuttonbeginner -> BoardSize.BEGINNER
                R.id.radiobuttonsemipro -> BoardSize.SEMIPRO
                else -> BoardSize.PROFESSIONAL
            }
            gameName = null
            customgameimages = null
            setup_your_board()
        })
    }

    private fun showalertdialogbox(title: String, view: View?, positiveclicklistener: View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK") { _, _ ->
                    positiveclicklistener.onClick(null)
                }.show()
    }

    private fun setup_your_board() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        memoryGame = Memento_Game(boardSize, customgameimages)
        when (boardSize) {
            BoardSize.BEGINNER -> {
                tvnummoves.text = "Beginner: 4 x 2"
                tvnumpairs.text = "Pairs: 0 / 4"
            }
            BoardSize.SEMIPRO -> {
                tvnummoves.text = "SemiPro: 6 x 3"
                tvnumpairs.text = "Pairs: 0 / 9"
            }
            BoardSize.PROFESSIONAL -> {
                tvnummoves.text = "Professional: 6 x 4"
                tvnumpairs.text = "Pairs: 0/12"
            }
        }
        tvnumpairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memory_adapter = BoardAdapter(this, boardSize, memoryGame.cards, object : BoardAdapter.CardClickListener
                {
                    override fun onCardClicked(position: Int) {
                        updateGameWithFlip(position)
                    }
                })

        rvboard.adapter = memory_adapter
        rvboard.setHasFixedSize(true)
        rvboard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    @SuppressLint("InvalidAnalyticsName")
    private fun updateGameWithFlip(position: Int) {
       // Handling errors
       if (memoryGame.havewongame()) {
           Snackbar.make(c_layout, "Victory!", Snackbar.LENGTH_LONG).show()
           return
       }
       if (memoryGame.iscardfacingup(position)) {
           Snackbar.make(c_layout, "Oops, Invalid Move", Snackbar.LENGTH_LONG).show()
           return
       }

       // Flip the cards
       if (memoryGame.flipCard(position)) {
           Log.i(CODE, "Match found!! Found Num pairs: ${memoryGame.numPairsFound}")
           val color = ArgbEvaluator().evaluate(
                   memoryGame.numPairsFound.toFloat() / boardSize.findNumPairs(),
                   ContextCompat.getColor(this, R.color.color_progress_none),
                   ContextCompat.getColor(this, R.color.color_progress_full)
           ) as Int
           tvnumpairs.setTextColor(color)
           tvnumpairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.findNumPairs()}"

           if (memoryGame.havewongame()) {
               Snackbar.make(c_layout, "Victory.", Snackbar.LENGTH_LONG).show()
               CommonConfetti.rainingConfetti(c_layout, intArrayOf(Color.RED, Color.BLUE, Color.GREEN)).oneShot()
               firebaseAnalytics.logEvent("won game") {
                   param("game", gameName ?: "[default]")
                   param("board size", boardSize.name)
               }
           }
       }
       tvnummoves.text = "Moves: ${memoryGame.findnummoves()}"
       memory_adapter.notifyDataSetChanged()
    }
}