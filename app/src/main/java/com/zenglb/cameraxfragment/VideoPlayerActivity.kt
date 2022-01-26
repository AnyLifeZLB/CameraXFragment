package com.zenglb.cameraxfragment


import android.media.MediaPlayer.OnCompletionListener
import android.os.Bundle
import android.text.TextUtils
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_video_player.*

/**
 * 这里仅仅是为了测试视频是否有问题
 *
 *
 */
class VideoPlayerActivity : AppCompatActivity() {
    private var mMP4Path: String? = null
    var mMediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        mMP4Path=intent.getStringExtra("mMP4Path");
        mMediaController = MediaController(this)

        if (!TextUtils.isEmpty(mMP4Path)) {
            video_view.setVideoPath(mMP4Path)
            video_view.setMediaController(mMediaController)
            video_view.seekTo(0)
            video_view.requestFocus()
            //这里循环播放好一点
            video_view.start()
        }
        mMediaController?.show(4000)

        video_view.setOnCompletionListener(OnCompletionListener {
            if (!TextUtils.isEmpty(mMP4Path)) {
                video_view.setVideoPath(mMP4Path)
                video_view.start()
            }
        })


        close_btn.setOnClickListener {
            finish()
        }

    }
}