package com.saveetha.ticket.Activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.saveetha.ticket.Adapters.RoutesListAdapter;
import com.saveetha.ticket.Models.Route;
import com.saveetha.ticket.Models.Stage;
import com.saveetha.ticket.R;

import org.json.JSONArray;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;

import static com.saveetha.ticket.Constants.Constants.LOCATION_UPDATE_TIME;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static AsyncHttpClient client = new AsyncHttpClient();

    private SpeechRecognizer speechRecognizer = null;
    private Intent recognizerIntent;
    private final int REQ_CODE_SPEECH_INPUT_DESTINATION = 100;
    private final int REQ_CODE_SPEECH_INPUT_ROUTE = 200;

    private ListView mListView;
    private ProgressBar loader;
    private static final int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {"android.permission.RECORD_AUDIO"};

    private static String LOG_TAG = "MainActivity";
    ImageButton mVoiceControlButton;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private TextToSpeech tts;

    public static ArrayList<Route> routesList;
    public static Route selectedRoute;
    public static ArrayList<Stage> selectedRouteStagesList;

    String[] values = { "Android", "iPhone", "WindowsMobile",
            "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
            "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
            "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
            "Android", "iPhone", "WindowsMobile" };

    Timer timer;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if (status == TextToSpeech.SUCCESS) {

                    int result = tts.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    } else {
                        speakOut("Hey can you tell us where you want to go?");
                    }

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });

        searchView = (SearchView) findViewById(R.id.searchView);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hello! How can I help you?");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, "1000");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        loader = (ProgressBar) findViewById(R.id.loader);
        loader.setVisibility(View.GONE);

        mListView = (ListView) findViewById(R.id.listView);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, values);

        mListView.setAdapter(adapter);

        mListView.setVisibility(View.GONE);
        getRoutes();
        searchView.setFocusable(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                if (query.toLowerCase().equals("ambattur")){
                    mListView.setVisibility(View.VISIBLE);
                    getRoutes();
                }else{
                    showToast("No Buses Found!");
                    mListView.setVisibility(View.GONE);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        mVoiceControlButton = (ImageButton)findViewById(R.id.search_voice_btn);
        mVoiceControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "Button Clicked!", Toast.LENGTH_SHORT).show();

                speakOut("Hey can you tell us where you want to go?");
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        // Actions to do after 10 seconds
                        askUserDestination();
                    }
                }, 3000);

            }
        });


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // Actions to do after 10 seconds
                askUserDestination();
            }
        }, 5000);

    }

    private void askUserDestination(){

        speechRecognizer.startListening(recognizerIntent);
        try {
            startActivityForResult(recognizerIntent, REQ_CODE_SPEECH_INPUT_DESTINATION);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void askUserRoutePreference(){

        speechRecognizer.startListening(recognizerIntent);
        try {
            startActivityForResult(recognizerIntent, REQ_CODE_SPEECH_INPUT_ROUTE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void speakOut(String text) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                Map<String, Integer> perms = new HashMap<>();

                // Initial
                perms.put("android.permission.RECORD_AUDIO", PackageManager.PERMISSION_GRANTED);

                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                if (perms.get("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission_Status", "RECORD_AUDIO Granted");
                } else {
                    Log.d("Permission_Status", "RECORD_AUDIO Denied");
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            Log.i(LOG_TAG, "destroy");
        }

    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        if (errorCode == SpeechRecognizer.ERROR_NO_MATCH){
            //showToast("We missed it, please try again.");
            Toast.makeText(MainActivity.this, "We missed it, please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
//        ArrayList<String> matches = results
//                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//        Log.i("Voice to Text", matches.get(0));

    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT_DESTINATION: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    speechRecognizer.stopListening();
                    processUserVoiceInput_Destination(result.get(0));
                }
                break;
            }
            case REQ_CODE_SPEECH_INPUT_ROUTE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    speechRecognizer.stopListening();
                    processUserVoiceInput_Route(result.get(0));
                }
                break;
            }

        }
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    public void getRoutes(){
        loader.setVisibility(View.VISIBLE);
        client.get("https://api.myjson.com/bins/18nhrz", new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                Log.d("Success", "" + response.toString());
                loader.setVisibility(View.GONE);
                try{
                    //showToast("SMS has been sent", 0);
                    String json = response.toString();
                    Type listType = new TypeToken<List<Route>>() {}.getType();
                    routesList = new Gson().fromJson(json, listType);
                    updateUI();
                }catch (Exception e){
                    Log.d("JsonErr", "" + e.toString());
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e("Err", "" + responseString.toString());
                loader.setVisibility(View.GONE);
               // showToast("Failed to send SMS, Try again.", 0);
            }
        });


    }


    private void updateUI(){
        mAdapter = new RoutesListAdapter(routesList, this);
        mRecyclerView.setAdapter(mAdapter);

        ((RoutesListAdapter) mAdapter).setOnItemClickListener(new RoutesListAdapter
                .MyClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                showToast("You have selected the route " + routesList.get(position).getName());

                String route = routesList.get(position).getName() + ": " + routesList.get(position).getOrigin().toUpperCase() + " - "+ routesList.get(position).getDestination().toUpperCase();
                ArrayList stages = new ArrayList(routesList.get(position).getStages());
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                Bundle info= new Bundle();
                info.putSerializable("stages",stages);
                info.putSerializable("route", routesList.get(position));
                intent.putExtras(info);
                intent.putExtra("route_name" , route);
                startActivity(intent);
            }
        });
    }

    public static String join(List<String> msgs) {
        return msgs == null || msgs.size() == 0 ? "" : msgs.size() == 1 ? msgs.get(0) : msgs.subList(0, msgs.size() - 1).toString().replaceAll("^.|.$", "") + " and " + msgs.get(msgs.size() - 1);
    }

    private void processUserVoiceInput_Destination(String input){
        if (input.toLowerCase().equals("ambattur")){

            List<String> routes = new ArrayList<>();

            for(int i=0;i<routesList.size();i++) {
                routes.add(routesList.get(i).getName());
            }

            speakOut("We found 5 buses to your destination. " + join(routes) + ". Pick any one of your preferred route.");

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    // Actions to do after 10 seconds
                    askUserRoutePreference();
                }
            }, 12000);

        }else{
            speechRecognizer.stopListening();
            speakOut("Sorry, we cant find any buses in the route. Try again.");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    // Actions to do after 10 seconds
                    askUserDestination();
                }
            }, 5000);
        }
    }

    private void processUserVoiceInput_Route(String input){
        if (!input.equals("")){
            if (checkRoute(input)){
                // get user's location
                // get previous stops list and location
                // alert distance and timing

                final int[] stageIndex = {0};
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        int index = stageIndex[0];
                        if (index==0){
                            speakOut("Your bus was started from " + selectedRoute.getOrigin() + " 1 hour back" + "and your bus is in " + selectedRouteStagesList.get(index).getName() + " right now. " +
                            "  we will update you further.");
                            stageIndex[0] += 1;
                            return;
                        }else if (index == 3){
                            speakOut("Your bus has reached " + selectedRouteStagesList.get(index).getName() + ". Kindly get into the bus. We will update you further stages and your destination. Have a nice trip!");
                            stageIndex[0] += 1;
                            return;
                        }else if (index == 8){
                            timer.cancel();
                            speakOut("You have reached " + selectedRouteStagesList.get(index).getName() + ". Kindly get down from the bus. Have a good day!");
                            return;
                        }

                        speakOut("Your bus has crossed " + selectedRouteStagesList.get(index).getName());
                        stageIndex[0] += 1;
                    }
                }, 0, LOCATION_UPDATE_TIME);

            }else{
                Toast.makeText(MainActivity.this, "We couln't find any routes from your input, Try again!", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(MainActivity.this, "Input seems empty input, Please try again!", Toast.LENGTH_SHORT).show();
        }

    }

    private void showToast(String message){
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }


    private boolean checkRoute(String route_name ){

        // removing spaces from user input text and converting to uppercase
        String route_input = route_name.toUpperCase();
        if (route_input.contains(" ")){
            route_input = route_input.replaceAll(" ", "");
        }

        // finding user input matches with our route list
        for(int i=0;i<routesList.size();i++) {
            String routeName = routesList.get(i).getName();
            if (routeName.equals(route_input)){
                selectedRoute = routesList.get(i);
                selectedRouteStagesList = new ArrayList<Stage>(routesList.get(i).getStages()) ;
                return true;
            }
        }

        return false;
    }


}
