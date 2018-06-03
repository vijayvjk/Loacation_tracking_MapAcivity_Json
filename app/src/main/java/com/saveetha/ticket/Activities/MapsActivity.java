package com.saveetha.ticket.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.saveetha.ticket.Models.Route;
import com.saveetha.ticket.Models.Stage;
import com.saveetha.ticket.R;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.saveetha.ticket.Constants.Constants.LOCATION_UPDATE_TIME;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Timer timer;

    private LatLng USER_LOCATION ;

    Route selectedRoute = new Route();
    ArrayList<Stage> stages;

    Marker prevMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getIntent().getExtras().getString("route_name"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded() {
        // check if we have got the googleMap already
        if (mMap != null) {
            addLines();
        }
    }

    private void addLines() {

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        stages = (ArrayList<Stage>)bundle.getSerializable("stages");
        selectedRoute = (Route)bundle.getSerializable("route");

        for (int i = 0; i < stages.size() - 1; i++) {

            if (i == 0){
                createMarker(new LatLng(Double.valueOf(stages.get(i).getLatitude()), Double.valueOf(stages.get(i).getLongitude())), stages.get(i).getName().toUpperCase());
            }else if (i == 7){
                createMarker(new LatLng(Double.valueOf(stages.get(i+1).getLatitude()), Double.valueOf(stages.get(i+1).getLongitude())), stages.get(i+1).getName().toUpperCase());
            }else if (i == 3){
                USER_LOCATION = new LatLng(Double.valueOf(stages.get(i).getLatitude()), Double.valueOf(stages.get(i).getLongitude()));
            }

            LatLng src = new LatLng(Double.valueOf(stages.get(i).getLatitude()), Double.valueOf(stages.get(i).getLongitude()));
            LatLng dest = new LatLng(Double.valueOf(stages.get(i+1).getLatitude()), Double.valueOf(stages.get(i+1).getLongitude()));

            // mMap is the Map Object
            Polyline line = mMap.addPolyline(
                    new PolylineOptions().add(
                            new LatLng(src.latitude, src.longitude),
                            new LatLng(dest.latitude,dest.longitude)
                    ).width(15).color(Color.BLUE).geodesic(true)
            );
        }


        // move camera to zoom on map
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(USER_LOCATION,
                12));


        final int[] stageIndex = {0};
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int index = stageIndex[0];
                if (index==0){
                    showToast("Your bus was started from " + selectedRoute.getOrigin() + " 1 hour back" + "and your bus is in " + stages.get(index).getName() + " right now. " +
                            "  we will update you further.");
                    updateLocation(index);
                    stageIndex[0] += 1;
                    return;
                }else if (index == 3){
                    showToast("Your bus has reached " + stages.get(index).getName() + ". Kindly get into the bus. We will update you further stages and your destination. Have a nice trip!");
                    updateLocation(index);
                    stageIndex[0] += 1;
                    return;
                }else if (index == 8){
                    timer.cancel();
                    showToast("You have reached " + stages.get(index).getName() + ". Kindly get down from the bus. Have a good day!");
                    updateLocation(index);
                    return;
                }

                showToast("Your bus has crossed " + stages.get(index).getName());
                updateLocation(index);
                stageIndex[0] += 1;
            }
        }, 0, LOCATION_UPDATE_TIME);

    }

    private void updateLocation(final int index){
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            public void run() {

                if (prevMarker != null){
                    prevMarker.remove();
                }

                prevMarker = moveMarker(new LatLng(Double.valueOf(stages.get(index).getLatitude()), Double.valueOf(stages.get(index).getLongitude())), stages.get(index).getName().toUpperCase());
            }
        });

    }

    protected Marker moveMarker(LatLng loc, String title) {
        int height = 100;
        int width = 100;
        BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.buspin);
        Bitmap b=bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

        return mMap.addMarker(new MarkerOptions()
                .position(loc)
                .anchor(0.5f, 0.5f)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                );

    }


    protected Marker createMarker(LatLng loc, String title) {

        return mMap.addMarker(new MarkerOptions()
                .position(loc)
                .anchor(0.5f, 0.5f)
                .title(title));

    }

    private void showToast(final String message){
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            public void run() {
                Toast.makeText(MapsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setUpMapIfNeeded();

//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
