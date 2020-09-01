package com.example.audioplayer

import android.Manifest
import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), MyAdapter.OnItemClickListener,
    AudioManager.OnAudioFocusChangeListener {

    override var selectedPosition: Int = RecyclerView.NO_POSITION
    override var previousPosition: Int = RecyclerView.NO_POSITION
    private val permissionRequestCode = 0
    private var audioList = ArrayList<AudioItem>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var mediaPlayer: MediaPlayer? = null
    private var currentPosition: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkStoragePermission()

        val t = "No music is selected"
        titleView.text = t
        titleView.isSelected = true
        seekBar.isEnabled = false
        seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seek: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {
                mediaPlayer!!.seekTo(seek.progress)
            }
        })
        playPause.setOnClickListener { togglePlayPause() }
        next.setOnClickListener { playNext() }
        start.setOnClickListener { playPrevious() }
        repeat.setOnClickListener { toggleLooping() }
    }

    private fun togglePlayPause() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            playPause.setImageResource(R.drawable.ic_white_baseline_play_arrow_32)
        } else if (mediaPlayer != null) {
            mediaPlayer!!.start()
            playPause.setImageResource(R.drawable.ic_white_baseline_pause_32)
        }
    }

    private fun playNext() {
        if (currentPosition != null) {
            if (currentPosition == audioList.size - 1) {
                currentPosition = -1
            }
            startPlayback(currentPosition!! + 1)
        }
    }

    private fun playPrevious() {
        if (currentPosition != null) {
            if (currentPosition == 0) {
                currentPosition = audioList.size
            }
            startPlayback(currentPosition!! - 1)
        }
    }

    private fun toggleLooping() {
        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying)
                if (mediaPlayer!!.isLooping) {
                    mediaPlayer!!.isLooping = false
                    repeat.setImageResource(R.drawable.ic_baseline_repeat_32)
                } else {
                    mediaPlayer?.isLooping = true
                    repeat.setImageResource(R.drawable.ic_baseline_repeat_one_32)
                }
        }
    }

    override fun onAudioFocusChange(p0: Int) {
        Toast.makeText(this, "$p0", Toast.LENGTH_SHORT).show()
        Log.e("Dikohi ", "$p0 kya hay")
    }

    override fun onItemClick(position: Int) {
        //viewAdapter.notifyItemChanged(position)
        startPlayback(position)
    }

    private fun startPlayback(position: Int) {

        val id: Long = audioList[position].idP0
        currentPosition = position
        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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
            setOnPreparedListener {
                mediaPlayer!!.start()
                playPause.setImageResource(R.drawable.ic_white_baseline_pause_32)
                seekBar.isEnabled = true
                titleView.text = audioList[position].titleP1
                previousPosition = selectedPosition
                selectedPosition = position
                viewAdapter.notifyItemChanged(selectedPosition)
                viewAdapter.notifyItemChanged(previousPosition)
                lifecycleScope.launch {
                    while (true) {
                        delay(100)
                        asyncSeekBar()
                    }
                }
            }
            setOnErrorListener { mediaPlayer, i, i2 ->
                mediaPlayer.reset()
                mediaPlayer.release()
                Log.e(
                    "Media Player error",
                    "value of second parameter $i and value of third parameter $i2"
                )
                true
            }
            prepareAsync()
            setOnCompletionListener {
                if (!mediaPlayer!!.isLooping)
                    playNext()
            }
        }
    }

    private fun asyncSeekBar() {
        if (mediaPlayer!!.isPlaying) {

            val totalTime = mediaPlayer!!.duration
            val currentTime = mediaPlayer!!.currentPosition
            startTime.text = String.format(
                "%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(currentTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(currentTime.toLong()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentTime.toLong()))
            )
            endTime.text = String.format(
                "%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(totalTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(totalTime.toLong()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTime.toLong()))
            )

            seekBar.max = totalTime
            seekBar.progress = currentTime
            progressBar.max = totalTime
            progressBar.progress = currentTime

        }
    }

    private fun cr() {
        val resolver: ContentResolver = contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val where = MediaStore.Audio.Media.IS_MUSIC + "=1"
        val cursor: Cursor? = resolver.query(uri, null, where, null, null)
        when {
            cursor == null -> {
            }
            !cursor.moveToFirst() -> {
            }
            else -> {
                val titleColumn: Int =
                    cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val idColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val sizeColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val durationColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
                val artistColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)

                do {
                    val thisId = cursor.getLong(idColumn)
                    val thisTitle = cursor.getString(titleColumn)
                    val thisSize = cursor.getInt(sizeColumn)
                    val thisDuration = cursor.getInt(durationColumn)
                    val thisAlbum = cursor.getString(albumColumn)
                    val thisArtist = cursor.getString(artistColumn)

                    val bitmap: Bitmap? = null
                    audioList.plusAssign(
                        AudioItem(
                            thisId,
                            thisTitle,
                            thisSize,
                            thisDuration,
                            thisAlbum,
                            thisArtist,
                            bitmap
                        )
                    )
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()

        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(audioList, this)
        recyclerView = my_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionRequestCode) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cr()
            } else {

                val newFragment = FireMissilesDialogFragment()
                newFragment.show(supportFragmentManager, "missiles")
                if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    CoroutineScope(Default).launch {
                        delay(5000)
                        finish()
                        exitProcess(0)
                    }
                }

            }
        }
    }

    class FireMissilesDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setMessage("You have to manically give the permission by giant to app info of this app. You are exiting app in 5 second")
                    .setNeutralButton(
                        "OK"
                    ) { dialogInterface: DialogInterface, _: Int ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri =
                            Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        intent.data = uri
                        this.startActivity(intent)
                        dialogInterface.cancel()
                    }
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
    }

    private fun checkStoragePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            cr()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                permissionRequestCode
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
