package com.amaze.fileutilities.video_player

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.GenericPagerViewerActivityBinding
import com.amaze.fileutilities.databinding.VideoPlayerDialogActivityBinding
import com.amaze.fileutilities.databinding.VideoPlayerFragmentBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

class VideoPlayerDialogActivity: PermissionActivity() {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        VideoPlayerDialogActivityBinding.inflate(layoutInflater)
    }
    private lateinit var videoModel: LocalVideoModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val videoUri = intent.data
            Log.i(javaClass.simpleName, "Loading video from path ${videoUri?.path} " +
                    "and mimetype $mimeType")
            videoModel = LocalVideoModel(uri = videoUri!!, mimeType = mimeType!!)
            val bundle = bundleOf(
                VideoPlayerFragment.VIEW_TYPE_ARGUMENT to videoModel
            )

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<VideoPlayerFragment>(R.id.fragment_container_view, args = bundle)
            }
            viewBinding.fullScreenButton.setOnClickListener {
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra(VideoPlayerFragment.VIEW_TYPE_ARGUMENT, videoModel)
                }
                startActivity(intent)
                this.finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.video_dialog, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.open_full -> {
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra(VideoPlayerFragment.VIEW_TYPE_ARGUMENT, videoModel)
                }
                startActivity(intent)
                this.finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}