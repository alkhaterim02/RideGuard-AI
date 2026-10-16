package com.rideguard.ai.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.rideguard.ai.R;
import com.rideguard.ai.util.Extras;

/** Fullscreen video with play, pause and seek controls. */
public class VideoPlayerActivity extends AppCompatActivity {

    public static Intent intent(Context c, String url, String title) {
        return new Intent(c, VideoPlayerActivity.class)
                .putExtra(Extras.VIDEO_URL, url)
                .putExtra(Extras.VIDEO_TITLE, title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        String url = getIntent().getStringExtra(Extras.VIDEO_URL);
        ((TextView) findViewById(R.id.tvVideoTitle)).setText(getIntent().getStringExtra(Extras.VIDEO_TITLE));
        VideoView video = findViewById(R.id.videoView);
        View progress = findViewById(R.id.progress);
        View error = findViewById(R.id.tvError);

        if (url == null) {
            progress.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            return;
        }
        MediaController controller = new MediaController(this);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setVideoURI(Uri.parse(url));
        video.setOnPreparedListener(mp -> {
            progress.setVisibility(View.GONE);
            video.start();
            controller.show(3000);
        });
        video.setOnErrorListener((mp, what, extra) -> {
            progress.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            return true;
        });
    }
}
