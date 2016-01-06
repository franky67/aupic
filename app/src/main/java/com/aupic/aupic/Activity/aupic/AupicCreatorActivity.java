package com.aupic.aupic.Activity.aupic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aupic.aupic.Activity.AupicDisplay.AupicDisplayActivity;
import com.aupic.aupic.Activity.base.AupFragmentActivity;
import com.aupic.aupic.Activity.camera.CameraActivityNew;
import com.aupic.aupic.Activity.gallery.GalleryAlbumActivity;
import com.aupic.aupic.Activity.media.MediaAudioActivity;
import com.aupic.aupic.Activity.ringdroid.EditAudioFileActivity;
import com.aupic.aupic.Adaptors.Aupic_Creator.AupicSideBarImageAdaptor;
import com.aupic.aupic.Constant.IntentConstants;
import com.aupic.aupic.Constant.StringConstants;
import com.aupic.aupic.Event.AppBus;
import com.aupic.aupic.Helper.GenerateFileNames;
import com.aupic.aupic.Helper.GenerateVideo;
import com.aupic.aupic.Helper.SeekBarHelper;
import com.aupic.aupic.Holder.Aupic_Creator.AupicSideBarViewHolder;
import com.aupic.aupic.Holder.Aupic_Creator.ChooseImagesViewHolder;
import com.aupic.aupic.Holder.FFmpeg.AudioMixingHolder;
import com.aupic.aupic.Holder.Media.MediaAudioDto;
import com.aupic.aupic.Holder.Media.RecordAudioViewHolder;
import com.aupic.aupic.Holder.Media.SelectedImagesDTO;
import com.aupic.aupic.MainActivity;
import com.aupic.aupic.R;
import com.aupic.aupic.Storage.TransientDataRepo;
import com.aupic.aupic.Task.FFmpeg.AudioMixingTask;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by saheb on 5/11/15.
 */
public class AupicCreatorActivity extends AupFragmentActivity implements AupicSideBarViewHolder.SelectedSideBarImage,
                                                                         RecordAudioViewHolder.audioRecorderCallBack,
                                                                         ChooseImagesViewHolder.ChooseImagesCallBack,
                                                                         SeekBar.OnSeekBarChangeListener,
                                                                         MediaPlayer.OnCompletionListener {

    private Set<String> selectedImagesList = new HashSet<>();
    SelectedImagesDTO selectedImagesDtoFirstImage;
    AupicSideBarImageAdaptor aupicSideBarImageAdaptor;
    private List<SelectedImagesDTO> selectedImagesDTOList;
    private HashMap<String,MediaAudioDto> imageAudioMap = new HashMap<>();

    private int playAudio = StringConstants.RESET_AND_PLAY;
    private MediaRecorder mRecorder = null;
    boolean mStartPlaying = true;
    private String audioFileName;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private Handler mHandler = new Handler();
    private SeekBarHelper utils = new SeekBarHelper();
    private GenerateFileNames generateFileNames = new GenerateFileNames();
    private long songDuration;
    private GenerateVideo generateVideo = new GenerateVideo();
    private Menu menuGlobal;

    @InjectView(R.id.selected_image)
    com.aupic.aupic.Graphics.SquareImageWithoutFade selectedFirstImageView;

    @InjectView(R.id.side_bar_list_view)
    ListView sideBarListView;

    @InjectView(R.id.media_gallery)
    ImageView mediaGallery;

    @InjectView(R.id.record_audio)
    ImageView recordAudio;

    @InjectView(R.id.media_player)
    LinearLayout llMediaPlayer;

    @InjectView(R.id.play_btn)
    ImageView playBtn;

    @InjectView(R.id.seek_bar)
    SeekBar seekBar;

    @InjectView(R.id.edit_audio)
    ImageView editAudio;

    @InjectView(R.id.start_time)
    TextView startTime;

    @InjectView(R.id.total_time)
    TextView totalTime;

    @InjectView(R.id.done_create_aupic)
    ImageView doneCreateAupic;

    @Override
    protected int getTitleText() {

        return R.string.aupic_editor;
    }

    @Override
    protected int getContentViewId() {

        return R.layout.aupic_creator_layout;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppBus.getInstance().register(this);

        ButterKnife.inject(this);

        selectedImagesList = (Set<String>) TransientDataRepo.getInstance().
                                                            getData(StringConstants.SELECTED_IMAGES);

        initialize(null);

        final Context context = this;
        final RecordAudioViewHolder.audioRecorderCallBack audioRecorderCallBackListener = this;
        seekBar.setOnSeekBarChangeListener(this);
        mediaPlayer.setOnCompletionListener(this);

        selectedFirstImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                showDeleteAction();
                return true;
            }
        });

        mediaGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent mediaIntent = new Intent(context , MediaAudioActivity.class);
                startActivityForResult(mediaIntent, Activity.RESULT_FIRST_USER);
            }
        });

        recordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mStartPlaying) {
                    String file = generateFileNames.getRecorderAudioFileName();
                    audioFileName = file;
                    startRecording(file);
                    mStartPlaying = false;

                    showRecordAlertBox(context, audioRecorderCallBackListener);
                }
            }
        });

        editAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent mediaIntent = new Intent(context, EditAudioFileActivity.class);
                mediaIntent.putExtra(IntentConstants.SELECTED_AUDIO, audioFileName);
                startActivityForResult(mediaIntent, Activity.RESULT_FIRST_USER);
            }
        });

        doneCreateAupic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               if (imageAudioMap.size() > 0) {

                   for (HashMap.Entry<String, MediaAudioDto> map: imageAudioMap.entrySet()){

                       String image = map.getKey();
                       String audio = map.getValue().getData();
                       String video = generateFileNames.getVideoFileName();

                       generateVideo.createVideoFromImageAndAudio(image, audio , video);

                       Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                       File f = new File(video);
                       Uri contentUri = Uri.fromFile(f);
                       mediaScanIntent.setData(contentUri);
                       context.sendBroadcast(mediaScanIntent);
                   }
               }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.aupic_creator_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.menuGlobal = menu;
        menu.findItem(R.id.action_delete).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showDeleteImageAlertBox();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        openMainActivity();
    }

    @Subscribe
    public void onAsyncTaskResult(AudioMixingHolder audioMixingHolder) {

        if (audioMixingHolder.getIsMixed()) {

            Toast.makeText(this, "Done mixing audios", Toast.LENGTH_SHORT).show();
            audioFileName = audioMixingHolder.getMixedFile();
            addAudioToMediaScanner();
            replaceOrMakeCurrentAudioFile(audioMixingHolder.getMediaAudioDto(),
                                          audioMixingHolder.getImagePath());

        } else {

            Toast.makeText(this, "Unable to merge File, previous audio kept", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void showDeleteImageAlertBox() {

        new AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to remove this image?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteImage();
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void deleteImage() {

        if (selectedImagesList.size() > 1) {

            String imagePathToBeRemoved = selectedImagesDtoFirstImage.getImagePath();

            selectedImagesList.remove(imagePathToBeRemoved);
            imageAudioMap.remove(imagePathToBeRemoved);
            initialize(null);

        } else {
            openMainActivity();
        }
    }

    private void initialize(String selectedImageFromSideBar) {

        hideDeleteAction();

        if ( null != selectedImagesList && selectedImagesList.size() > 0) {

            initializeSelectedImagesDTOList(selectedImageFromSideBar);
            Uri uri = Uri.fromFile(new File(selectedImagesDtoFirstImage.getImagePath()));
            Picasso.with(this)
                    .load(uri)
                    .placeholder(null)
                    .resize(300, 375)
                    .into(selectedFirstImageView);

            if ( null != selectedImagesDtoFirstImage.getAudioPath() && !selectedImagesDtoFirstImage
                                                                        .getAudioPath().isEmpty()) {
                showSeekBar();
            } else {
                hideSeekBar();
            }

            aupicSideBarImageAdaptor = new AupicSideBarImageAdaptor(this, R.id.side_bar_list_view,
                                                                    selectedImagesDTOList, this, this);

            sideBarListView.setAdapter(aupicSideBarImageAdaptor);
        }
    }

    private void initializeSelectedImagesDTOList(String selectedImageFromSideBar) {

        Integer count = 0;
        SelectedImagesDTO selectedImagesDTO;
        selectedImagesDTOList = new ArrayList<>();

        for (String imageUrl : selectedImagesList) {

            if ((null != selectedImageFromSideBar && imageUrl.equals(selectedImageFromSideBar))
                  || (null == selectedImageFromSideBar && count == 0)) {

                selectedImagesDtoFirstImage = new SelectedImagesDTO();
                selectedImagesDtoFirstImage.setImagePath(imageUrl);

                MediaAudioDto mediaAudioDto = imageAudioMap.get(imageUrl);
                if (null != mediaAudioDto) {

                    selectedImagesDtoFirstImage.setAudioPath(mediaAudioDto.getData());
                    selectedImagesDtoFirstImage.setAudioDuration(mediaAudioDto.getDuration());
                }

            } else {

                selectedImagesDTO = new SelectedImagesDTO();

                selectedImagesDTO.setImage(null);
                selectedImagesDTO.setImagePath(imageUrl);

                MediaAudioDto mediaAudioDto = imageAudioMap.get(imageUrl);
                if (null != mediaAudioDto) {

                    selectedImagesDTO.setAudioPath(mediaAudioDto.getData());
                    selectedImagesDTO.setAudioDuration(mediaAudioDto.getDuration());
                }

                selectedImagesDTOList.add(selectedImagesDTO);
            }
            count++;
        }

        selectedImagesDTO = new SelectedImagesDTO();
        selectedImagesDTO.setImagePath(null);
        selectedImagesDTOList.add(selectedImagesDTO);

    }

    @Override
    public void getSelectedSideBarImage(String selectedSideBarImage) {

        selectedImagesDTOList = new ArrayList<>();
        mStartPlaying = true;

        initializeMediaPlayerFromScratch();

        initialize(selectedSideBarImage);

    }

    @Override
    public void saveAudioCallBack(AlertDialog alertDialog) {
        alertDialog.dismiss();
        stopRecording();
        addAudioToMediaScanner();

        final String imagePath = selectedImagesDtoFirstImage.getImagePath();
        final MediaAudioDto mediaAudioDto = new MediaAudioDto();

        MediaPlayer mP = new MediaPlayer();

        try {
            mP.setDataSource(audioFileName);
            mP.prepare();

            mediaAudioDto.setDuration(mP.getDuration());
            songDuration = mP.getDuration();
            mP.stop();
            mP.release();
            mP = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null != imageAudioMap.get(imagePath)) {

            final MediaAudioDto mediaAudioDtoInternal = imageAudioMap.get(imagePath);

            new AlertDialog.Builder(this)
                    .setTitle("Mix Audio")
                    .setMessage("Do you want your recorded audio to be mixed with your previously " +
                            "selected audio or to overwrite it?")
                    .setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mergeAudioFiles(mediaAudioDtoInternal.getData(), audioFileName,
                                            mediaAudioDto, imagePath);
                        }
                    })
                    .setNegativeButton(R.string.overwrite, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            replaceOrMakeCurrentAudioFile(mediaAudioDto, imagePath);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {

            replaceOrMakeCurrentAudioFile(mediaAudioDto, imagePath);
        }
    }

    @Override
    public void cancelAudioCallBack(AlertDialog alertDialog) {
        alertDialog.dismiss();
        stopRecording();
        mStartPlaying = true;

        File file = new File(audioFileName);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void getChosenImagesCallBack(int type, AlertDialog alertDialog) {

        alertDialog.dismiss();

        if (type == StringConstants.CAMERA_CHOSEN) {

            Intent mediaIntent = new Intent(this , CameraActivityNew.class);
            mediaIntent.putExtra(IntentConstants.NEW_ACTIVITY, false);
            startActivityForResult(mediaIntent, Activity.RESULT_FIRST_USER);

        } else if (type == StringConstants.GALLERY_CHOSEN) {

            Intent mediaIntent = new Intent(this , GalleryAlbumActivity.class);
            mediaIntent.putExtra(IntentConstants.NEW_ACTIVITY, false);
            startActivityForResult(mediaIntent, Activity.RESULT_FIRST_USER);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK ) {

            if (null != data.getSerializableExtra(IntentConstants.SELECTED_AUDIO)) {


                final String imagePath = selectedImagesDtoFirstImage.getImagePath();
                final MediaAudioDto mediaAudioDto = (MediaAudioDto) data.getSerializableExtra(IntentConstants.
                                               SELECTED_AUDIO);

                audioFileName = mediaAudioDto.getData();

                if (null == imageAudioMap.get(imagePath)) {


                    imageAudioMap.put(imagePath, mediaAudioDto);
                    songDuration = Long.valueOf(mediaAudioDto.getDuration().toString());
                    initializeMediaPlayer();

                } else {

                    final MediaAudioDto mediaAudioDtoInternal = new MediaAudioDto();
                    mediaAudioDto.setDuration(Integer.valueOf(mediaAudioDto.getDuration().toString()));

                    new AlertDialog.Builder(this)
                        .setTitle("Merge Audio")
                        .setMessage("Do you want 2nd audio to be mixed with your previously " +
                                "selected audio or to overwrite it?")
                        .setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mergeAudioFiles(imageAudioMap.get(imagePath).getData(), audioFileName,
                                        mediaAudioDtoInternal, imagePath);
                            }
                        })
                        .setNegativeButton(R.string.overwrite, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                replaceOrMakeCurrentAudioFile(mediaAudioDto, imagePath);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                }

            }

            if (data.getBooleanExtra(IntentConstants.SELECTED_IMAGES_MAP, false)) {

                selectedImagesList = (Set<String>) TransientDataRepo.getInstance()
                                                    .getData(StringConstants.SELECTED_IMAGES);
                mStartPlaying = true;
                initialize(null);
            }
        }
    }

    private void mergeAudioFiles(String firstFileName, String secondFileName, MediaAudioDto mediaAudioDto,
                                String imagePath) {

        String mergedFile = generateFileNames.getRecorderAudioFileName();
        new AudioMixingTask(this).execute(firstFileName, secondFileName, mergedFile, imagePath,
                                          mediaAudioDto);

    }

    private void replaceOrMakeCurrentAudioFile(MediaAudioDto mediaAudioDto, String imagePath) {

        mediaAudioDto.setData(audioFileName);
        mediaAudioDto = setDuration(mediaAudioDto);
        imageAudioMap.put(imagePath, mediaAudioDto);

        initializeMediaPlayer();
    }

    private void startRecording(String audioFileName) {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(audioFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("Aupic Creator Activity", "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mStartPlaying = true;
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    private void showRecordAlertBox(Context context, RecordAudioViewHolder.audioRecorderCallBack
                                                                      audioRecorderCallBackListener) {

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.
                LAYOUT_INFLATER_SERVICE);
        View convertView = mInflater.inflate(R.layout.record_dialog_box, null);

        RecordAudioViewHolder recordAudioViewHolder = new RecordAudioViewHolder(convertView,
                                                          audioRecorderCallBackListener);

        recordAudioViewHolder.render(context, alertDialog);

        alertDialog.setView(convertView);
        alertDialog.show();
    }

    private void addAudioToMediaScanner() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(audioFileName);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void initializeMediaPlayer() {

        initializeMediaPlayerFromScratch();

        llMediaPlayer.setVisibility(View.VISIBLE);
        playBtn.setImageResource(R.drawable.play_btn);
        playAudio = StringConstants.RESET_AND_PLAY;

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (playAudio == StringConstants.RESET_AND_PLAY) {

                    initializeSong();
                    playAudio = StringConstants.PAUSE;
                    playBtn.setImageResource(R.drawable.pause_btn);

                } else if (playAudio == StringConstants.PLAY) {
                    playSong();
                    playAudio = StringConstants.PAUSE;
                    playBtn.setImageResource(R.drawable.pause_btn);

                } else if (playAudio == StringConstants.PAUSE) {
                    pauseSong();
                    playAudio = StringConstants.PLAY;
                    playBtn.setImageResource(R.drawable.play_btn);
                }
            }
        });
    }

    private void initializeMediaPlayerFromScratch() {

        stopMediaPlayer();
        startMediaPlayer();
    }

    private void stopMediaPlayer() {

        if ( null != mediaPlayer) {

            //removeMediaPlayerCallBacks();
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void startMediaPlayer() {

        mediaPlayer = new MediaPlayer();
    }

    private void pauseSong() {

        mediaPlayer.pause();
    }

    private void initializeSong() {

        try {
            mediaPlayer.setDataSource(audioFileName);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        playSong();
    }

    private void playSong() {

        try {
            mediaPlayer.start();

            seekBar.setProgress(0);
            seekBar.setMax(100);

            updateProgressBar();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {

            long totalDuration = songDuration;
            long currentDuration = mediaPlayer.getCurrentPosition();

            if ( currentDuration > totalDuration) {
                currentDuration = 0L;
            }

            // Displaying Total Duration time
            totalTime.setText("" + utils.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            startTime.setText("" + utils.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
            seekBar.setProgress(progress);
            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    /**
     * When user starts moving the progress handler
     * */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    /**
     * When user stops moving the progress handler
     * */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mediaPlayer.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mediaPlayer.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    @Override
    public void onCompletion(MediaPlayer arg0) {

        initializeAfterSongCompletion();
    }

    private void hideSeekBar() {

        removeMediaPlayerCallBacks();
        llMediaPlayer.setVisibility(View.GONE);
        mediaPlayer.reset();
        seekBar.setProgress(0);
    }

    private void showSeekBar() {

        audioFileName = selectedImagesDtoFirstImage.getAudioPath();
        songDuration = selectedImagesDtoFirstImage.getAudioDuration();
        removeMediaPlayerCallBacks();
        initializeMediaPlayer();
    }

    private void initializeAfterSongCompletion() {

        removeMediaPlayerCallBacks();
        mediaPlayer.reset();
        playAudio = StringConstants.RESET_AND_PLAY;
        playBtn.setImageResource(R.drawable.play_btn);
        seekBar.setProgress(0);
    }

    private void openMainActivity() {

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity(intent);
    }

    private void removeMediaPlayerCallBacks() {

        mHandler.removeCallbacksAndMessages(mUpdateTimeTask);
    }

    private MediaAudioDto setDuration(MediaAudioDto mediaAudioDto) {

        MediaPlayer mP = new MediaPlayer();

        try {
            mP.setDataSource(audioFileName);
            mP.prepare();

            mediaAudioDto.setDuration(mP.getDuration());
            songDuration = mP.getDuration();
            mP.stop();
            mP.release();
            mP = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mediaAudioDto;
    }

    private void showDeleteAction() {

        if ( null != menuGlobal) {

            menuGlobal.findItem(R.id.action_delete).setVisible(true);
        }
    }

    private void hideDeleteAction() {

        if ( null != menuGlobal) {

            menuGlobal.findItem(R.id.action_delete).setVisible(false);
        }
    }

}
