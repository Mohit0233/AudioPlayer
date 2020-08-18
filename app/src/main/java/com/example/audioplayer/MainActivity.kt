package com.example.audioplayer

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), MyAdapter.OnItemClickListener,
    AudioManager.OnAudioFocusChangeListener {

    private val permissionRequestCode = 0
    private var audioList = ArrayList<AudioItem>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var mediaPlayer: MediaPlayer? = null
    private var currentPostion: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkStoragePermission()
        cr()
        val t = "No music is selected"
        titleView.text = t
        titleView.isSelected = true
        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(audioList, this)
        recyclerView = my_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter

        }

        play.setOnClickListener { togglePlayPause() }
        next.setOnClickListener { playNext() }
        previous.setOnClickListener { playPrevious() }
        close.setOnClickListener {

        }
    }

    private fun togglePlayPause() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            play.setImageResource(R.drawable.ic_baseline_play_arrow_32)
        } else if (mediaPlayer != null) {
            mediaPlayer!!.start()
            play.setImageResource(R.drawable.ic_baseline_pause_24)
        }
    }

    private fun playNext() {
        if (currentPostion != null) {
            if (currentPostion == audioList.size - 1) {
                currentPostion = -1
            }
            startPlayback(currentPostion!! + 1)
        }
    }

    private fun playPrevious() {
        if (currentPostion != null) {
            if (currentPostion == 0) {
                currentPostion = audioList.size
            }
            startPlayback(currentPostion!! - 1)
        }
    }

    private fun toggleLooping() {
        if (mediaPlayer!!.isPlaying)
            if (mediaPlayer!!.isLooping) {
                mediaPlayer!!.isLooping = false
            } else {
                mediaPlayer?.isLooping = true
            }
    }

    override fun onAudioFocusChange(p0: Int) {
        Toast.makeText(this, "$p0", Toast.LENGTH_SHORT).show()
        Log.e("Dikohi ", "$p0 kya hay")
    }


    override fun onItemClick(position: Int) {
        Toast.makeText(this, "Item $position Clicked", Toast.LENGTH_SHORT).show()
        //viewAdapter.notifyItemChanged(position)
        startPlayback(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onResume() {
        super.onResume()
        cr()
    }


    private fun cr() {
        val resolver: ContentResolver = contentResolver
        val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)
        when {
            cursor == null -> {
                // query failed, handle error.
            }
            !cursor.moveToFirst() -> {
                // no media on the device
            }
            else -> {
                val titleColumn: Int =
                    cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
                val idColumn: Int =
                    cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
                do {
                    val thisId = cursor.getLong(idColumn)
                    val thisTitle = cursor.getString(titleColumn)
                    audioList.plusAssign(AudioItem(thisId, thisTitle))
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()
    }


    private fun startPlayback(position: Int) {


        val id: Long = audioList[position].id
        currentPostion = position
        val contentUri: Uri =
            ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )

        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying)
                mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(applicationContext, contentUri)
            setOnPreparedListener { mediaPlayer!!.start() }
            setOnErrorListener { mediaPlayer, i, i2 ->
                mediaPlayer.reset()
                mediaPlayer.release()
                Log.e(
                    "Media Player error",
                    "value of second parameter $i and value of third parameter $i2"
                )
                true
            }
            prepareAsync() // might take long! (for buffering, etc)
            setOnCompletionListener {
                if (!mediaPlayer!!.isLooping)
                    playNext()
            }

        }
        play.setImageResource(R.drawable.ic_baseline_pause_24)
        titleView.text = audioList[position].text1

        //seekbar dal do frandz
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == permissionRequestCode) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                cr()

                Toast.makeText(applicationContext, "Ok Thanks", Toast.LENGTH_LONG).show()
            } else {
                // Permission request was denied.
                Toast.makeText(applicationContext, "Manu to Chiye hi Chiye", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun checkStoragePermission() {
        // Check if the Camera permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is already available, start camera preview

        } else {
            // Permission is missing and must be requested.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                permissionRequestCode
            )
        }
    }


}
