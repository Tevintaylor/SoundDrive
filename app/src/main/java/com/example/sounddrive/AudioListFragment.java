package com.example.sounddrive;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;


/**
 * A simple {@link Fragment} subclass.
 */
public class AudioListFragment extends Fragment implements AudioListAdapter.onItemListClick{

    private ConstraintLayout playerSheet;
    private BottomSheetBehavior bottomSheetBehavior;

    private AudioListAdapter audioListAdapter;

    private RecyclerView audioList;

    private File[] allFiles;

    private MediaPlayer mediaPlayer = null;
    private boolean isPlaying=false;
    private File fileToPlay;

    private SeekBar playerSeekbar;
    private Handler seekbarHandler;
    private Runnable updateSeekbar;

//    UI Elements
    private ImageView playBtn;
    private TextView playerHeader;
    private TextView playerFilename;

    private ImageView fastFarword, rewind;

    public AudioListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerSheet = view.findViewById(R.id.player_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(playerSheet);
        audioList = view.findViewById(R.id.audio_list_view);

        playBtn = view.findViewById(R.id.player_play_btn);
        playerHeader = view.findViewById(R.id.player_header_title);
        playerFilename = view.findViewById(R.id.player_filename);

        fastFarword = view.findViewById(R.id.player_fast_farword);
        rewind = view.findViewById(R.id.player_rewind);


        playerSeekbar = view.findViewById(R.id.player_seekbar);

        String path = getActivity().getExternalFilesDir("/").getAbsolutePath();
        File directory = new File(path);
        allFiles = directory.listFiles();

        audioListAdapter= new AudioListAdapter(allFiles,this);

        audioList.setHasFixedSize(true);
        audioList.setLayoutManager(new LinearLayoutManager(getContext()));
        audioList.setAdapter(audioListAdapter);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState == BottomSheetBehavior.STATE_HIDDEN){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }


            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                we can't do anything here for this app
            }
        });


        playBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                if(isPlaying){ pauseAudio();
                }else{
                    if(fileToPlay != null){ resumeAudio();
                    }
                }
            }
        });

        playerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pauseAudio();
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                mediaPlayer.seekTo(progress);
                resumeAudio();
            }
        });


        fastFarword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fastForword();
            }
        });

        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rewind();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClickListener(File file, int position) {
        fileToPlay = file;
        if (isPlaying){
            stopAudio();
            playAudio(fileToPlay);
        }else{
            playAudio(fileToPlay);
        }
    }

//    function to fast forword
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void fastForword(){
        if (mediaPlayer == null) {
            return;
        }
        int pos = mediaPlayer.getCurrentPosition();
        pos += 1500; // milliseconds
        mediaPlayer.seekTo(pos);
        resumeAudio();
    }

//    function to rewind
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void rewind(){
        if (mediaPlayer == null) {
            return;
        }
        int pos = mediaPlayer.getCurrentPosition();
        pos -= 1500; // milliseconds
        mediaPlayer.seekTo(pos);
        resumeAudio();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void pauseAudio(){
        mediaPlayer.pause();
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn,null));
        isPlaying = false;
        seekbarHandler.removeCallbacks(updateSeekbar);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void resumeAudio(){
        mediaPlayer.start();
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn,null));
        isPlaying = true;
        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar,0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopAudio() {
//        stop the audio
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn,null));
        playerHeader.setText("Stopped");
        isPlaying = false;
        mediaPlayer.stop();
        seekbarHandler.removeCallbacks(updateSeekbar);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void playAudio(File fileToPlay) {
//        play the audio
        mediaPlayer = new MediaPlayer();

        bottomSheetBehavior.setState((BottomSheetBehavior.STATE_EXPANDED));

        try {
            mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn,null));
        playerFilename.setText(fileToPlay.getName());
        playerHeader.setText("Playing");

        isPlaying = true;

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopAudio();
                playerHeader.setText("Finished");
            }
        });

        playerSeekbar.setMax(mediaPlayer.getDuration());

        seekbarHandler = new Handler();
        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar,0);
    }

    private void updateRunnable() {
        updateSeekbar = new Runnable() {
            @Override
            public void run() {
                playerSeekbar.setProgress(mediaPlayer.getCurrentPosition());
                seekbarHandler.postDelayed(this,500);
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onStop() {
        super.onStop();
        if(isPlaying){
            stopAudio();
        }
    }
}
