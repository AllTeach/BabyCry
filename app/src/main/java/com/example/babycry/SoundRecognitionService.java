package com.example.babycry;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;
import org.tensorflow.lite.task.core.BaseOptions;

public class SoundRecognitionService extends Service {


    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String STOP_ACTION = "com.example.app.STOP_ACTION";
    private MediaPlayer mediaPlayer;
    private String TAG = "MainActivity";
    private int playCounter = 0;
    private int maxPlayCount = 2; // Number of times to play the audio file
    private String modelPath = "lite-model_yamnet_classification_tflite_1.tflite"; // defines the model to be used
    private float probabilityThreshold = 0.5f; // defining the minimum threshold

    @Override
    public void onCreate() {
        super.onCreate();
     //   mediaPlayer = MediaPlayer.create(this, R.raw.dog1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP_ACTION.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        Intent stopIntent = new Intent(this, SoundRecognitionService.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Listen For Baby")
                .setContentText("Started Listening")
                .setSmallIcon(R.drawable.ic_stat_dog)
                .addAction(R.drawable.ic_stat_dog, "Stop Listening", stopPendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
// Initialization


                    AudioClassifier.AudioClassifierOptions options =
                            AudioClassifier.AudioClassifierOptions.builder()
                                    //         .setBaseOptions(BaseOptions.builder().useGpu().build())
                                    .setMaxResults(1)
                                    .build();
                    AudioClassifier classifier =
                            null;
                    try {
                        classifier = AudioClassifier.createFromFileAndOptions(this, modelPath, options);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

// Start recording
                    AudioRecord record = classifier.createAudioRecord();
                    TensorAudio audioTensor = classifier.createInputTensorAudio();

                    record.startRecording();
                    Timer timer = new Timer();
                    AudioClassifier finalClassifier = classifier;
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {


// Load latest audio samples
                            audioTensor.load(record);

// Run inference
                            List<Classifications> results = finalClassifier.classify(audioTensor);
                            // Filter out classifications with low probability
                            List<Category> filteredModelOutput = new ArrayList<>();
                            for (Category category : results.get(0).getCategories()) {
                                if (category.getScore() > probabilityThreshold) {
                                    filteredModelOutput.add(category);
                                }
                            }

                            // Sort the results by score in descending order
                            Collections.sort(filteredModelOutput, (o1, o2) -> Float.compare(o2.getScore(), o1.getScore()));

                            for (Category category : filteredModelOutput) {
                                Log.d(TAG, "onStartCommand: " + category.getLabel() + " -> " + category.getScore());
                                if(category.getLabel().contains("cry")  || category.getLabel().contains("Crying"))
                                {
                                    showNotification(category.getLabel(), category.getScore());
                                    Log.d("SOUND_CLASS", "MATCH !!!: " + category.getLabel() + " -> " + category.getScore());

                                }
                                Log.d("SOUND_CLASS", "onStartCommand: " + category.getLabel() + " -> " + category.getScore());
                            }
                        }
                    }, 1, 500);






        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private List<String> loadLabels() throws IOException {
        InputStream inputStream = getAssets().open("yamnet_class_map.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<String> labels = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length > 1) {
                labels.add(parts[1]);
            }
        }
        reader.close();
        return labels;
    }

    private void showNotification(String label, float score) {
    // Create a PendingIntent to stop the foreground service
    Intent stopIntent = new Intent(this, SoundRecognitionService.class);
    stopIntent.setAction(STOP_ACTION);
    PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    // Create the notification
    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sound Detected: " + label)
            .setContentText("Score: " + score)
            .setSmallIcon(R.drawable.ic_stat_dog)
            .addAction(R.drawable.ic_stat_dog, "Stop Listening", stopPendingIntent)
            .build();

    // Get the NotificationManager and show the notification
    NotificationManager notificationManager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
    notificationManager.notify(NOTIFICATION_ID, notification);
}
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SoundRecognitionServiceChannel";
            String description = "Channel for Sound Recognition Service";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


}