package com.jw.develop.tts_saysomething;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.*;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, Spinner.OnItemSelectedListener {

    private String TAG = "MainActivity";

    private EditText input_et;
    private Button say_btn, store_btn, browser_btn;
    private TextToSpeech tts;
    private InterstitialAd interstitial;
    private SeekBar pitch_sb, speech_sb;
    private Spinner language;
    private ImageView icon_iv;

    private int count;
    float pitch = 1.0f, rate = 1.0f;

    //extra egg
    private String secretString = "你按的我好舒服阿恩恩摁摁恩恩摁啪啪啪啪啪啪啪摁摁恩恩啵啵啵啵棒";

    //line's package name & selectChat class name
    public static final String PACKAGE_NAME = "jp.naver.line.android";
    public static final String CLASS_NAME = "jp.naver.line.android.activity.selectchat.SelectChatActivity";
    private List<ApplicationInfo> m_appList;
    private boolean lineInstallFlag = false;

    //check network AlertDialog
    private AlertDialog.Builder adBuider;
    private AlertDialog ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Build Alert Dialog : isNetworkOnline?
        adBuider = new AlertDialog.Builder(this);
        ad = adBuider.create();


        //Initialize TextToSpeech  element
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.CHINESE);
                }
            }
        });

        //initView
        initialView();

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-8638499673748784~1105249952");
        // Create the interstitial and set the adUnitId.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                /*
                                String input = input_et.getText().toString();
                                tts.speak(input, TextToSpeech.QUEUE_FLUSH, null);
                                AdRequest adRequest_interstitial = new AdRequest.Builder().addTestDevice("406BF9DC46AF8ED519B01B5720270AC5").build();
                                interstitial.loadAd(adRequest_interstitial);
                                */
            }
        });
        AdRequest adRequest_interstitial = new AdRequest.Builder().addTestDevice("406BF9DC46AF8ED519B01B5720270AC5").build();
        interstitial.loadAd(adRequest_interstitial);
        //showInterstitial();   //show Interstitial Ad

        // Load an ad into the AdMob banner view.
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest_banner = new AdRequest.Builder()/*.addTestDevice("406BF9DC46AF8ED519B01B5720270AC5")*/.build();
        adView.loadAd(adRequest_banner);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        if (!ad.isShowing()) {
            Log.d(TAG, "onResume: check Internet");
            checkInternet_ad();
        }

        if (isNetworkOnline() && ad.isShowing()) ad.dismiss();  //check alertDialog, if alertDialog is showing and internet is online than dismiss the alertDiaolog.
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }

    /*initialize elements on view.*/
    private void initialView() {
        input_et = (EditText) findViewById(R.id.editText_input);

        say_btn = (Button) findViewById(R.id.button_say);
        store_btn = (Button) findViewById(R.id.button_store);
        browser_btn = (Button) findViewById(R.id.button_browser);
        say_btn.setOnClickListener(this);
        store_btn.setOnClickListener(this);
        browser_btn.setOnClickListener(this);

        pitch_sb = (SeekBar) findViewById(R.id.seekBar_pitch);
        speech_sb = (SeekBar) findViewById(R.id.seekBar_speech);
        pitch_sb.setOnSeekBarChangeListener(this);
        speech_sb.setOnSeekBarChangeListener(this);

        language = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> language_list = ArrayAdapter.createFromResource(MainActivity.this,
                R.array.language_list,
                android.R.layout.simple_spinner_dropdown_item);
        language.setAdapter(language_list);
        language.setOnItemSelectedListener(this);

        icon_iv = (ImageView) findViewById(R.id.imageView_icon);
        icon_iv.setOnClickListener(this);
    }

    private void checkInternet_ad() {
        if (isNetworkOnline()) {
            Log.d(TAG, "Internet status = " + isNetworkOnline());
            if (ad != null) ad.dismiss();
        } else {
            Log.d(TAG, "Internet status = " + isNetworkOnline() + ", show Alert Dialog");
            adBuider.setTitle("WARNING").setMessage("此APP需開啟網路連線，是否開啟網路?");
            adBuider.setCancelable(false);
            adBuider.setPositiveButton("開啟WFI設定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                }
            });
            adBuider.setNeutralButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            adBuider.setNegativeButton("開啟行動網路", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    startActivity(intent);
                }
            });
            ad = adBuider.create();
            ad.show();
        }
    }

    /*call this function to show Interstitial Ad.*/
    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        if (interstitial != null && interstitial.isLoaded()) {
            interstitial.show();
        } else {
            Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_say:
                //showInterstitial();
                String input = input_et.getText().toString();
                if(!checkEditTextNull(input)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ttsGreater21(input);
                    } else {
                        ttsUnder20(input);
                    }
                }else{
                    Toast.makeText(this,R.string.text_null_alarm,Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.button_store:
                String input_save = input_et.getText().toString();
                if(!checkEditTextNull(input_save)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ttsSaveGreater21(input_save);
                    } else {
                        ttsSaveUnder21(input_save);
                    }

                    if(checkLineInstalled()){
                        sendWavToLine();
                    } else{
                        Toast.makeText(this,R.string.line_install_alarm,Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(this,R.string.text_null_alarm,Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.button_browser:
                Uri uri = Uri.parse(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/");
                Log.i("URI", uri+"");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "resource/folder");
                if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
                {
                    startActivity(Intent.createChooser(intent, "Open folder"));
                }
                else
                {
                    // if you reach this place, it means there is no any file
                    // explorer app installed on your device
                }
                break;

            case R.id.imageView_icon:
                count++;
                if (count == 10) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ttsGreater21(secretString);
                    } else {
                        ttsUnder20(secretString);
                    }
                    count = 0;
                }
                break;
        }
    }

    /**
     * Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Menu
     */

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch (seekBar.getId()) {

            case R.id.seekBar_pitch:
                switch (i) {
                    case 0:
                        pitch = 0.25f;
                        tts.setPitch(pitch);
                        break;
                    case 1:
                        pitch = 0.5f;
                        tts.setPitch(pitch);
                        break;
                    case 2:
                        pitch = 1.0f;
                        tts.setPitch(pitch);
                        break;
                    case 3:
                        pitch = 2.0f;
                        tts.setPitch(pitch);
                        break;
                    case 4:
                        pitch = 3.0f;
                        tts.setPitch(pitch);
                        break;
                }
                Log.i(TAG, "pitch = " + pitch);
                break;

            case R.id.seekBar_speech:
                switch (i) {
                    case 0:
                        rate = 0.25f;
                        tts.setSpeechRate(rate);
                        break;
                    case 1:
                        rate = 0.5f;
                        tts.setSpeechRate(rate);
                        break;
                    case 2:
                        rate = 1.0f;
                        tts.setSpeechRate(rate);
                        break;
                    case 3:
                        rate = 2.0f;
                        tts.setSpeechRate(rate);
                        break;
                    case 4:
                        rate = 3.0f;
                        tts.setSpeechRate(rate);
                        break;

                }
                Log.i(TAG, "rate = " + rate);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (i) {
            case 0:
                tts.setLanguage(Locale.CHINESE);
                break;
            case 1:
                tts.setLanguage(Locale.US);
                break;
            case 2:
                tts.setLanguage(Locale.JAPANESE);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    /*********************************
     * TTS.speak()判斷API是否再20 以上
     ***************************************/
    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP & Build.VERSION_CODES.M & Build.VERSION_CODES.N)
    private void ttsGreater21(String text) {
        String utteranceId = this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }
    /**************************************************************************************************/


    /*********************************
     * TTS.synthesizeToFile() ，存檔 ，判斷API是否再20 以上(SonyXperiaZ 測試機存檔路徑為 /storage/emulated/0/Android/data/com.jw.develop.tts_saysomething/files/Download/test.wav)
     **********************************/
    @SuppressWarnings("deprecation")
    private void ttsSaveUnder21(String text) {
        HashMap<String, String> myHashRender = new HashMap();
        String destinationFileName = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/test.wav";
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
        int i = tts.synthesizeToFile(text, myHashRender, destinationFileName);
        Log.d("ttsSAVE_APIUnder21", i +",     " + destinationFileName);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP & Build.VERSION_CODES.M & Build.VERSION_CODES.N)
    private void ttsSaveGreater21(String text) {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "test.wav");
        //file.getParentFile().mkdirs();
        int i = tts.synthesizeToFile(text, null, file, "test1");
        Log.d("ttsSAVE_APIGreater21", i +",     " + file);
    }
    /**************************************************************************************************/

    public void sendWavToLine(){
        Log.i(TAG, "sendWavToLine");
        String fileName = "test.wav";
        String filePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + fileName;
        Uri uri = Uri.fromFile(new File(filePath));
        Log.i(TAG, "sendWavToLine :  filePath = " + filePath);

//        Intent sendIntent = new Intent();
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
//        sendIntent.setType("audio/*");
//        startActivity(sendIntent);

//        //sendWAV
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setClassName(PACKAGE_NAME,CLASS_NAME);
//        intent.setType("audio/wav");
//        intent.putExtra(Intent.EXTRA_STREAM,uri);
//        startActivity(intent);

        //another finction to send WAV
        ComponentName cn = new ComponentName(PACKAGE_NAME, CLASS_NAME);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM,uri);
        shareIntent.setComponent(cn);//跳到指定APP的Activity
        startActivity(Intent.createChooser(shareIntent,""));

//        //sendTEXT
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_TEXT, "Hello World!!");
//        startActivity(intent);
    }

    /**
     * Check editText's text is null or not
     * @param text
     * @return boolean
     */
    private boolean checkEditTextNull(String text){
        if(text.trim().equals("")){
            return true;
        }else {
            return false;
        }
    }

    /**
     * Check device has installed or hasn't installed LINE app
     * @return true or false
     */
    private boolean checkLineInstalled(){
        PackageManager pm = getPackageManager();
        m_appList = pm.getInstalledApplications(0);
        lineInstallFlag = false;
        for (ApplicationInfo ai : m_appList) {
            if(ai.packageName.equals(PACKAGE_NAME)){
                lineInstallFlag = true;
                break;
            }
        }
        return lineInstallFlag;
    }

    /**
     * Check device's network is/isn't connecting.
     */
    public boolean isNetworkOnline() {
        boolean status = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                status = true;
            } else {
                netInfo = cm.getNetworkInfo(1);
                if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED)
                    status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return status;
    }

}
