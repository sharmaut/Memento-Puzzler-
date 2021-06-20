@file:Suppress("DEPRECATION")

package com.example.finalproject

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.initials.BoardSize
import com.example.finalproject.variables.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class Creation : AppCompatActivity() {

    companion object {
        private const val CODE = "Creation"
        private const val Pick_a_photo_code = 91
    }

    private lateinit var rvimagepicker: RecyclerView
    private lateinit var edittextgamename: EditText
    private lateinit var buttonsave: Button
    private lateinit var progressBarUploading: ProgressBar

    private lateinit var imagepickeradapter: Adapter_ImageSelection
    private lateinit var boardSize: BoardSize
    private var imagesrequired = -1
    private val chosenImagesUris = mutableListOf<Uri>()
    private val firebaseAnalytics = Firebase.analytics
    private val remoteConfig = Firebase.remoteConfig
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvimagepicker = findViewById(R.id.rvimagepicker)
        edittextgamename = findViewById(R.id.edittextgamename)
        buttonsave = findViewById(R.id.buttonsave)
        progressBarUploading = findViewById(R.id.progressBarUploading)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(BOARD_SIZE) as BoardSize
        imagesrequired = boardSize.findNumPairs()
        supportActionBar?.title = "Choose pictures (0 / $imagesrequired)"

        buttonsave.setOnClickListener {
            save_your_data_to_Firebase()
        }

        edittextgamename.filters = arrayOf(InputFilter.LengthFilter(14))
        edittextgamename.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    buttonsave.isEnabled = shouldEnableSaveButton()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int, ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        imagepickeradapter = Adapter_ImageSelection(this, chosenImagesUris, boardSize, object : Adapter_ImageSelection.ImageClickListener {
            override fun placeholderclicker()  {
                launchIntentForPhotos()
            }
        })
        rvimagepicker.adapter = imagepickeradapter
        rvimagepicker.setHasFixedSize(true)
        rvimagepicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != Pick_a_photo_code || resultCode != RESULT_OK || data == null) {
            Log.w(CODE, "Did not receive data from the launched activity")
            return
        }
        Log.i(CODE, "onActivityResult")
        val selectedUri = data.data
        val clipData = data.clipData
        if (clipData != null) {
            Log.i(CODE, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImagesUris.size < imagesrequired) {
                    chosenImagesUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(CODE, "data: $selectedUri")
            chosenImagesUris.add(selectedUri)
        }
        imagepickeradapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pictures (${chosenImagesUris.size} / $imagesrequired"
        buttonsave.isEnabled = shouldEnableSaveButton()
    }

    private fun save_your_data_to_Firebase() {
        Log.i(CODE, "save Data To Firebase")
        val customGameName = edittextgamename.text.toString().trim()
        firebaseAnalytics.logEvent("creation_save_your_attempt") {
            param("game_name", customGameName)
        }

        // I'm going to check if I'm not copying someone else's data
        buttonsave.isEnabled = false
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game with the same name already exists '$customGameName'. Choose another name")
                    .setPositiveButton("OK", null)
                    .show()
                buttonsave.isEnabled = true
            } else {
                handleimageuploading(customGameName)
            }
        }.addOnFailureListener { exception ->
            Log.e(CODE, "Error encountered while saving your game", exception)
            Toast.makeText(this, "Error encountered while saving your game", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleimageuploading(gamename: String) {
        progressBarUploading.visibility = View.VISIBLE
        val uploadedimageurls = mutableListOf<String>()
        var didencountererror = false
        for ((index, photoUri) in chosenImagesUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gamename/$(System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photouploadtask ->
                    Log.i(CODE, "Uploaded bytes: ${photouploadtask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(CODE, "Exception with Firebase storage", downloadUrlTask.exception)
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        didencountererror = true
                        return@addOnCompleteListener
                    }
                    if (didencountererror) {
                        progressBarUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    progressBarUploading.progress = uploadedimageurls.size * 100 / chosenImagesUris.size
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedimageurls.add(downloadUrl)
                    Log.i(CODE, "Upploading fininshed $photoUri, num uploaded ${uploadedimageurls.size}"
                    )
                    if (uploadedimageurls.size == chosenImagesUris.size) {
                        handleallimagesuploaded(gamename, uploadedimageurls)
                    }
                }
        }
    }

    private fun handleallimagesuploaded(gamename: String, imageurls: MutableList<String>) {
        // TODO: upload info to firestore
        db.collection("games").document(gamename)
            .set(mapOf("images" to imageurls))
            .addOnCompleteListener { gameCreationTask ->
                progressBarUploading.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(CODE, "Exception during game's creation", gameCreationTask.exception)
                    Toast.makeText(this, "Game creation failed", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                firebaseAnalytics.logEvent("creation_save_success") {
                    param("game_name", gamename)
                }
                Log.i(CODE, "Game created successfully $gamename")
                AlertDialog.Builder(this)
                    .setTitle("Upload completed! Start your game '$gamename'")
                    .setPositiveButton("OK") { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(GAME_NAME, gamename)
                        setResult(RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(CODE, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(
            originalBitmap,
            remoteConfig.getLong("scaled height").toInt()
        )
        Log.i(CODE, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, remoteConfig.getLong("compress quality").toInt(), byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImagesUris.size != imagesrequired) {
            return false
        }
        if (edittextgamename.text.isBlank() || edittextgamename.text.length < 3) {
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pictures"), Pick_a_photo_code)
    }
}