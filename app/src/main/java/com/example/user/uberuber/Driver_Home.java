package com.example.user.uberuber;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;


import com.example.user.uberuber.Common.Common;
import com.example.user.uberuber.Model.Token;
import com.example.user.uberuber.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Driver_Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener ,OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{






    private GoogleMap mMap;

    //Play Services
    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;



    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;


    DatabaseReference drivers;
    GeoFire geoFire;

    Marker mCurrent;

    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;



    //Car Animation
    private List<LatLng> polyLineList;
    private Marker carMarker;//ummmmmmmmm
    private float v;
    private double lat , lng;
    private Handler handler;
    private LatLng startPosition,endPosition , currentPosition;//ummmmm
    private int index,next;
    //private Button btnGo;
    private PlaceAutocompleteFragment places;
    AutocompleteFilter typeFilter;
    private String destination;
    private PolylineOptions polylineOptions,blackPolyLineOptions;
    private Polyline blackPolyline , greyPolyline;


    private IGoogleAPI mService;


    //Presence System
    DatabaseReference onlineRef , currentUserRef;


    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if (index<polyLineList.size()-1){
                index++;
                next = index+1;
            }

            if(index < polyLineList.size()-1){
                startPosition = polyLineList.get(index);
                endPosition = polyLineList.get(next);
            }


            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);//whaaaaaaaaaat about the final keyword huuuuh lol cant be final
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    v = valueAnimator.getAnimatedFraction();
                    lng = v*endPosition.longitude+(1-v)*startPosition.longitude;
                    lat = v*endPosition.latitude+(1-v)*startPosition.latitude;
                    LatLng newPos = new LatLng(lat,lng);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f, 0.5f);
                    carMarker.setRotation(getBearing(startPosition,newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(newPos)
                                    .zoom(15.5f)
                                    .build()

                    ));
                }
            });

            valueAnimator.start();
            handler.postDelayed(this , 3000);

        }
    };


    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat = Math.abs(startPosition.latitude - endPosition.latitude);
        double lng = Math.abs(startPosition.longitude - endPosition.latitude);

        if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat)));

        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude < endPosition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+90);

        else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat))+180);

        else if (startPosition.latitude < endPosition.latitude && startPosition.longitude >= endPosition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+270);
        return -1;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver__home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);




        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        //Presense System
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");
        currentUserRef = FirebaseDatabase.getInstance().getReference(Common.driver_tb1)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //We will remove value from Driver tb1 when driver disconnected
                currentUserRef.onDisconnect().removeValue();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        location_switch = (MaterialAnimatedSwitch) findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline ) {
                if (isOnline){

                    FirebaseDatabase.getInstance().goOnline(); //set connected when switched to on


                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(),"You are online" , Snackbar.LENGTH_SHORT).show();
                }
                else
                {
                    FirebaseDatabase.getInstance().goOffline(); //Set disconnected when switched to off


                    stopLocationUpdates();
                    mCurrent.remove();
                    mMap.clear();
                    //handler.removeCallbacks(drawPathRunnable); //i removed this part because it was giving me problems it was crashing works perfectly fine
                    Snackbar.make(mapFragment.getView(),"You are offline" , Snackbar.LENGTH_SHORT).show();

                }
            }
        });


        polyLineList = new ArrayList<>();


        //Places Api
        typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .setTypeFilter(3)
                .build();
        places = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (location_switch.isChecked())
                {
                    destination = place.getAddress().toString();
                    destination = destination.replace("" , "+");

                    getDirection();
                }


                else {
                    Toast.makeText(Driver_Home.this ,"Please change your status to ONLINE", Toast.LENGTH_SHORT ).show();
                }


            }

            @Override
            public void onError(Status status) {
                Toast.makeText(Driver_Home.this , ""+status.toString() , Toast.LENGTH_SHORT).show();
            }
        });





        //Geo Fire
        drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tb1);
        geoFire = new GeoFire(drivers);

        setUpLocation();

        mService = Common.getGoogleAPI();

        updateFirebaseToken();
    }


    private void updateFirebaseToken() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tb1);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);
    }

    private void getDirection() {
        currentPosition = new LatLng(Common.mLastLocation.getLatitude() , Common.mLastLocation.getLongitude());

        String requestApi = null;

        try {

            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination="+destination+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);
            Log.d("EDMTDEV" , requestApi);//Print URL FOR DEBUG
            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for (int i = 0 ; i<jsonArray.length() ; i++){

                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyline = poly.getString("points");
                                    polyLineList = decodePoly(polyline);

                                }

                                //Adjusting bounds
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng latLng:polyLineList)
                                    builder.include(latLng);
                                LatLngBounds bounds = builder.build();
                                CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds , 2);
                                mMap.animateCamera(mCameraUpdate);

                                polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.GRAY);
                                polylineOptions.width(5);
                                polylineOptions.startCap(new SquareCap());
                                polylineOptions.endCap(new SquareCap());
                                polylineOptions.jointType(JointType.ROUND);
                                polylineOptions.addAll(polyLineList);
                                greyPolyline = mMap.addPolyline(polylineOptions);


                                blackPolyLineOptions = new PolylineOptions();
                                blackPolyLineOptions.color(Color.BLACK);
                                blackPolyLineOptions.width(5);
                                blackPolyLineOptions.startCap(new SquareCap());
                                blackPolyLineOptions.endCap(new SquareCap());
                                blackPolyLineOptions.jointType(JointType.ROUND);
                                blackPolyline = mMap.addPolyline(blackPolyLineOptions);

                                mMap.addMarker(new MarkerOptions()
                                        .position(polyLineList.get(polyLineList.size()-1))
                                        .title("Pickup Location"));

                                //Animation
                                ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0 , 100 );
                                polyLineAnimator.setDuration(2000);
                                polyLineAnimator.setInterpolator(new LinearInterpolator());
                                polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        List<LatLng> points = greyPolyline.getPoints();
                                        int percentValue = (int)valueAnimator.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int)(size * (percentValue/100.0f));
                                        List<LatLng> p = points.subList( 0 , newPoints);
                                        blackPolyline.setPoints(p);
                                    }
                                });

                                polyLineAnimator.start();

                                carMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                        .flat(true)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                                handler = new Handler();
                                index=-1;
                                next=1;
                                handler.postDelayed(drawPathRunnable , 3000);











                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(Driver_Home.this , ""+t.getMessage() , Toast.LENGTH_SHORT).show();
                        }
                    });

        }

        catch (Exception e){
            e.printStackTrace();
        }

    }

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
    //Because we request runtime permission , we need to override OnRequestPermissionResult

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(checkPlayServices()){

                        buildGoogleApiClient();
                        createLocationRequest();

                        if(location_switch.isChecked())
                            displayLocation();

                    }
                }

        }
    }

    private void setUpLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )
        {
            //Request runtime permission
            ActivityCompat.requestPermissions(this , new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION


            },MY_PERMISSION_REQUEST_CODE);
        }
        else
        {

            if(checkPlayServices()){

                buildGoogleApiClient();
                createLocationRequest();

                if(location_switch.isChecked())
                    displayLocation();

            }




        }

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);


    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this , PLAY_SERVICE_RES_REQUEST).show();

            else{
                Toast.makeText(this, "This device is not supported" , Toast.LENGTH_SHORT).show();
                finish();
            }

            return false;

        }

        return true;
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )
        {
            return;
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )
        {
            return;
        }

        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation  != null)
        {
            if (location_switch.isChecked())
            {
                final double latitude = Common.mLastLocation.getLatitude();
                final double longitude = Common.mLastLocation.getLongitude();


                LatLng center = new LatLng(latitude,longitude);
                LatLng northSide = SphericalUtil.computeOffset(center, 10000 , 0 );
                LatLng southSide = SphericalUtil.computeOffset(center, 10000 , 180 );

                LatLngBounds bounds = LatLngBounds.builder()
                        .include(northSide)
                        .include(southSide)
                        .build();

                places.setBoundsBias(bounds);
                places.setFilter(typeFilter);

                //UPDATE TO FIREBASE
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if(mCurrent != null)
                            mCurrent.remove(); //Remove already marker
                        mCurrent = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude , longitude))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                                .title("You Location"));



                        //Move the camera to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude), 15.0f));



                    }
                });

            }
        }
        else
        {
            Log.d("ERROR","Cannot get your location");
        }

    }


    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )
        {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, this);


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.driver__home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_trip_history) {
            Intent intent = new Intent(Driver_Home.this,Hospitals.class);
            startActivity(intent);
        } else if (id == R.id.nav_way_bill) {
            //this is our kotlin file
          //  Intent intent2 = new Intent(Driver_Home.this,MapsActivity.class);
            //startActivity(intent2);

        } else if (id == R.id.nav_help) {

        } else if (id == R.id.nav_sign_out) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation = location;
        displayLocation();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }
}
