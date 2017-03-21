package com.fay.david.myfirstmapboxapp;

import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.support.design.widget.FloatingActionButton;
import android.support.annotation.NonNull;
import android.location.Location;
import android.content.pm.PackageManager;
import android.Manifest;


import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;


public class MainActivity extends AppCompatActivity {

    //TODO: ADD NO-FLY ZONE POLYGONS TO THE APP
    //TODO: ADD NO-FLY ZONE SECTION TO DATABASE
    //TODO: IMPLEMENT A BETTER GET CURRENT LOCATION FUNCTION
    //TODO: CREATE A DRONE SIMULATOR APP THAT FLYS WITH SIMULATED LOCATIONS
    //TODO: AND TRANSMIT LOCATION BACK TO SERVER AND CHECKS NO-FLY ZONES

    private static final String TAG = "MainActivity";
    private MapView mapView;
    private LocationServices locationServices;
    private MapboxMap map;
    private FloatingActionButton floatingActionButton;


    private static final int PERMISSIONS_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Call this to cache the database locally
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        //get an instance and reference to the database
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

        //get a reference to the location entries in the database
        DatabaseReference mDatabaseRef = mDatabase.getReference("location");

        //mapbox key
        MapboxAccountManager.start(this,getString(R.string.access_token));

        // GeoFire database rference
        DatabaseReference geoFireDBref = FirebaseDatabase.getInstance().getReference("GeoFire");
        GeoFire geoFire = new GeoFire(geoFireDBref);

        setContentView(R.layout.activity_main);

        locationServices = LocationServices.getLocationServices(MainActivity.this);

        //create the compass icon
        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        Drawable iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.compass);
        final Icon icon = iconFactory.fromDrawable(iconDrawable);
        // a much simpler way to create custom icons from png files
        final Icon dogicon = iconFactory.fromAsset("dog.png");
        final Icon drone = iconFactory.fromAsset("003-drone.png");
        final Icon house = iconFactory.fromAsset("house.png");


        //create a mapview
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);


        //declare the initial two markers
        final MarkerViewOptions home = new MarkerViewOptions().icon(house)
                .position(new LatLng(39.700931, -83.743719)).title("The big red house").snippet("look out for the dog poop");
        final MarkerViewOptions dog = new MarkerViewOptions().icon(dogicon).position(new LatLng(0,0)).title("moving marker").snippet("watch me go");

        // the creates the the MapboxMap display and places one marker on it
        // the style is also set here
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap){
                map = mapboxMap;
                // customize map with markers, polylines etc
                map.setStyleUrl(Style.SATELLITE_STREETS);
                map.addMarker(home);

            }
        });


        // These next couple of lines get the curren location of the user
        // They need to be rewritten better the were just the first example I found on stack overflow
        // a better implementation can probably be derived from the enable location method
        LocationManager service = (LocationManager)
                getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = service.getBestProvider(criteria, false);
        Location location = service.getLastKnownLocation(provider);

        // THIS IS WHERE THE GEOFIRE CODE STARTS
        // this creates a few fake drone entries in the data base using the location I just got from the device
        // I've supplied offsets to each of them
        // TODO: these should be in their own app simulating flight
        // TODO: WHEN I TESTED THIS FROM WRIGHT STATE THE LOCATION OF THE DRONES WAS STILL SET AROUND MY HOUSE
        // TODO: TROUBLE SHOOT WHAT I DID WRONG WITH SETTING THE LOCAIONS
        geoFire.setLocation("Rouge One", new GeoLocation(location.getLatitude()+.0007, location.getLongitude()+.0008));
        geoFire.setLocation("Red One", new GeoLocation(location.getLatitude()+.0002, location.getLongitude()+.0003));
        geoFire.setLocation("Echo Seven", new GeoLocation(location.getLatitude()-.0009, location.getLongitude()-.0003));
        geoFire.setLocation("Jedi One", new GeoLocation(location.getLatitude()-.0002, location.getLongitude()+.0004));
        geoFire.setLocation("Red Leader", new GeoLocation(location.getLatitude()+.0005, location.getLongitude()-.0006));
        geoFire.setLocation("Red three", new GeoLocation(location.getLatitude()+.0002, location.getLongitude()+.0003));
        geoFire.setLocation("Red six", new GeoLocation(location.getLatitude()-.001, location.getLongitude()-.0009));
        geoFire.setLocation("Red twelve", new GeoLocation(location.getLatitude()-.0007, location.getLongitude()+.0014));
        geoFire.setLocation("Red five", new GeoLocation(location.getLatitude()+.0015, location.getLongitude()-.0019));

        // this is a single event listener from geofire
//        geoFire.getLocation("Rouge One", new LocationCallback() {
//            @Override
//            public void onLocationResult(String key, GeoLocation location) {
//                if (location != null) {
//                    System.out.println(String.format("The location for key %s is [%f,%f]", key, location.latitude, location.longitude));
//                } else {
//                    System.out.println(String.format("There is no location for key %s in GeoFire", key));
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                System.err.println("There was an error getting the GeoFire location: " + databaseError);
//            }
//        });

        // TODO: WHEN I TESTED THIS FROM WRIGHT STATE THE LOCATION OF THE DRONES WAS STILL SET AROUND MY HOUSE
        // TODO: TROUBLE SHOOT WHAT I DID WRONG WITH SETTING OR FETCHING THE LOCATIONS
        // this is were I set up the location based geoquery based off of the users location I got earlier
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), 0.2);
        //  this is the event listener for the geoquery

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                MarkerViewOptions geofireMarker = new MarkerViewOptions().icon(drone).position(new LatLng(0, 0)).title("GeoFire Marker").snippet(String.format("%s", key));
                map.addMarker(geofireMarker).setPosition(new LatLng(location.latitude, location.longitude));
                System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onKeyExited(String key) {
                System.out.println(String.format("Key %s is no longer in the search area", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                MarkerViewOptions geofireMarker = new MarkerViewOptions().icon(drone).position(new LatLng(0, 0)).title("GeoFire Marker").snippet(String.format("%s", key));
                map.addMarker(geofireMarker).setPosition(new LatLng(location.latitude, location.longitude));
                System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                System.out.println("All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                System.err.println("There was an error with this query: " + error);
            }
        });



        // This is the event listener from the firebase database for a single test user
        mDatabaseRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Double lat = dataSnapshot.child("testuser").child("lat").getValue(Double.class);
                Double lng = dataSnapshot.child("testuser").child("lng").getValue(Double.class);
                map.addMarker(dog).setPosition(new LatLng(lat, lng));
                Log.d(TAG, "Value is: " + lat + " " + lng);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        // this is the button that toggles centering the app on the users location
        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    toggleGps(!map.isMyLocationEnabled());
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);

    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        mapView.onLowMemory();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

    }

    private void toggleGps(boolean enableGps) {
        if (enableGps) {
            // Check if user has granted location permission
            if (!locationServices.areLocationPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enableLocation(true);
            }
        } else {
            enableLocation(false);
        }
    }

    private void enableLocation(boolean enabled) {
        if (enabled) {
            // If we have the last location of the user, we can move the camera to that position.
            Location lastLocation = locationServices.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
            }

            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        locationServices.removeLocationListener(this);
                    }
                }
            });
            floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation(true);
            }
        }
    }




}
