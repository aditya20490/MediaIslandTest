package com.example.mediaislandtest

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : Activity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var status: TextView

    private val songs = listOf(
        Triple("Dummy Song One", "Test Artist A",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        Triple("Dummy Song Two", "Test Artist B",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        status = TextView(this).apply {
            text = "Connecting..."
            textSize = 18f
            gravity = Gravity.CENTER
        }
        root.addView(status)

        fun addButton(label: String, onClick: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { onClick() }
            })
        }

        addButton("Play Song 1") { playSong(0) }
        addButton("Play Song 2") { playSong(1) }
        addButton("Pause / Resume") {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }
        addButton("Stop") { controller?.stop() }

        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get()
            status.text = "Ready. Play a song, then go to home screen."
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    status.text = if (isPlaying) "Playing" else "Paused"
                }
            })
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onStop()
    }

    private fun playSong(index: Int) {
        val (title, artist, url) = songs[index]
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
        controller?.apply {
            setMediaItem(item)
            prepare()
            play()
        }
    }
}
