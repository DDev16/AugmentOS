package com.teamopensmartglasses.convoscope;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import com.teamopensmartglasses.convoscope.events.NewScreenImageEvent;
import com.teamopensmartglasses.convoscope.events.NewScreenTextEvent;
import com.teamopensmartglasses.convoscope.ui.ConvoscopeUi;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.DiarizationOutputEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.GlassesTapOutputEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.SmartGlassesConnectedEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.SmartRingButtonOutputEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.SpeechRecOutputEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.util.ArrayList;

import com.teamopensmartglasses.smartglassesmanager.SmartGlassesAndroidService;
import com.teamopensmartglasses.smartglassesmanager.smartglassescommunicators.SmartGlassesFontSize;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.ASR_FRAMEWORKS;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.AudioWearable;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.SmartGlassesDevice;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.SmartGlassesOperatingSystem;

public class AugmentosSmartGlassesService extends SmartGlassesAndroidService {
    public final String TAG = "AugmentOS_AugmentOSService";

    private final IBinder binder = new LocalBinder();

    String authToken = "";

    ArrayList<String> responsesBuffer;
    ArrayList<String> responsesToShare;
    private final Handler csePollLoopHandler = new Handler(Looper.getMainLooper());
    private Runnable cseRunnableCode;
    private final Handler displayPollLoopHandler = new Handler(Looper.getMainLooper());

    private long currTime = 0;
    private long lastPressed = 0;
    private long lastTapped = 0;

    // Double clicking constants
    private final long doublePressTimeConst = 420;
    private final long doubleTapTimeConst = 600;
    public DisplayQueue displayQueue;

    public AugmentosSmartGlassesService() {
        super(ConvoscopeUi.class,
                "augmentos_app",
                3589,
                "AugmentOS SGM",
                "AugmentOS SmartGlassesManager", R.drawable.ic_launcher_foreground);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //setup event bus subscribers
        this.setupEventBusSubscribers();

        displayQueue = new DisplayQueue();

        String asrApiKey = getResources().getString(R.string.google_api_key);
        saveApiKey(this, asrApiKey);

        saveChosenAsrFramework(this, ASR_FRAMEWORKS.AZURE_ASR_FRAMEWORK);

        this.aioConnectSmartGlasses();
    }

    @Override
    protected void onGlassesConnected(SmartGlassesDevice device) {
        Log.d(TAG, "Glasses connected successfully: " + device.deviceModelName);
        setFontSize(SmartGlassesFontSize.MEDIUM);
        displayQueue.startQueue();
    }

    @Override
    public void onDestroy(){
        EventBus.getDefault().unregister(this);

        if (displayQueue != null) displayQueue.stopQueue();

        super.onDestroy();
    }

    @Subscribe
    public void onGlassesTapSideEvent(GlassesTapOutputEvent event) {
        int numTaps = event.numTaps;
        boolean sideOfGlasses = event.sideOfGlasses;
        long time = event.timestamp;

        Log.d(TAG, "GLASSES TAPPED X TIMES: " + numTaps + " SIDEOFGLASSES: " + sideOfGlasses);
        if (numTaps == 3) {
            Log.d(TAG, "GOT A TRIPLE TAP");
        }
    }

    @Subscribe
    public void onSmartRingButtonEvent(SmartRingButtonOutputEvent event) {
        int buttonId = event.buttonId;
        long time = event.timestamp;
        boolean isDown = event.isDown;

        if(!isDown || buttonId != 1) return;
        Log.d(TAG,"DETECTED BUTTON PRESS W BUTTON ID: " + buttonId);
        currTime = System.currentTimeMillis();

        //Detect double presses
        if(isDown && currTime - lastPressed < doublePressTimeConst) {
            Log.d(TAG, "Double tap - CurrTime-lastPressed: "+ (currTime-lastPressed));
        }

        if(isDown) {
            lastPressed = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onDiarizeData(DiarizationOutputEvent event) {
    }

    @Subscribe
    public void onTranscript(SpeechRecOutputEvent event) {

    }
}