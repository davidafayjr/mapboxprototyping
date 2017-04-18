package com.fay.david.myfirstmapboxapp;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.support.design.widget.FloatingActionButton;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.location.Location;
import android.content.pm.PackageManager;
import android.Manifest;


import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.iid.FirebaseInstanceId;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@SuppressWarnings( {"MissingPermission"})

public class MainActivity extends AppCompatActivity implements PermissionsListener {

    private static final String TAG = "MainActivity";
    private MapView mapView;

    private MapboxMap map;
    private FloatingActionButton floatingActionButton;


    private LocationEngine locationEngine;
    private LocationEngineListener locationEngineListener;
    private PermissionsManager permissionsManager;
    private GeoQuery geoQuery;
    private Location lastLocation;
    private MarkerViewOptions dog;
    private MarkerViewOptions home;
    private GeoFire geoFire;
    private FirebaseDatabase mDatabase;
    private DatabaseReference firebaseDatabaseLocationReference;
    private DatabaseReference firebaseDatabaseNoFlyZoneReference;
    private DatabaseReference geofireDatabaseReference;
    private DatabaseReference firebaseDatbaseDescriptionReference;
    private DatabaseReference firebaseDatabasePurposeReference;
    private ChildEventListener firebaseDatabasePurposeChildEventListener;
    private ValueEventListener firebaseDatabaseLocationEventListener;
    private ChildEventListener firebaseDatabaseNoFlyZoneChildEventListener;
    private ChildEventListener firebaseDatbaseDescriptionChildEventListener;
    public MarkerViewOptions geoFireDrone;
    public Marker droneMarker;
    private Icon dogicon;
    private Icon drone;
    private Icon house;
    private double radius;
    private Map<String, Marker>drones;
    private Map<String, ArrayList>NoFlyZones;
    private Map<String, Polygon>NoFlyZonesPolygons;
    private Map<String, String>droneDescription;
    private Map<String, String>dronePurpose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Call this to cache the database locally
        // FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mDatabase = FirebaseDatabase.getInstance();

        //get a reference to the location entries in the database
        firebaseDatabaseLocationReference = mDatabase.getReference("dog");
        firebaseDatabaseLocationReference.keepSynced(true);
        firebaseDatabaseNoFlyZoneReference = mDatabase.getReference("NoFlyZones");
        firebaseDatabaseNoFlyZoneReference.keepSynced(true);
        firebaseDatbaseDescriptionReference = mDatabase.getReference("UserInfo/description");
        firebaseDatbaseDescriptionReference.keepSynced(true);
        firebaseDatabasePurposeReference = mDatabase.getReference("UserInfo/purpose");
        firebaseDatabasePurposeReference.keepSynced(true);

        radius =0.2;

        drones = new HashMap<String, Marker>();
        NoFlyZones =  new HashMap<String, ArrayList>();
        NoFlyZonesPolygons = new HashMap<String, Polygon>();
        droneDescription = new HashMap<String, String>();
        dronePurpose = new HashMap<String, String>();

        // GeoFire database rference
        geofireDatabaseReference = FirebaseDatabase.getInstance().getReference("GeoFire");
        geofireDatabaseReference.keepSynced(true);
        geoFire = new GeoFire(geofireDatabaseReference);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the account manager
        setContentView(R.layout.activity_main);

        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        // a much simpler way to create custom icons from png files
        dogicon = iconFactory.fromAsset("dog.png");
        drone = iconFactory.fromAsset("003-drone.png");
        house = iconFactory.fromAsset("house.png");

        //this is mapboxes location service
        locationEngine = LocationSource.getLocationEngine(this);
        locationEngine.activate();

        //create a mapview
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        // the creates the the MapboxMap display and places one marker on it
        // the style is also set here
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {

                map = mapboxMap;
                // customize map with markers, polylines etc
                home = new MarkerViewOptions().icon(house)
                        .position(new LatLng(39.700931, -83.743719)).title("The big red house").snippet("look out for the dog poop");
                dog = new MarkerViewOptions().icon(dogicon).position(new LatLng(0, 0)).title("moving marker").snippet("watch me go");

                map.setStyleUrl(Style.SATELLITE_STREETS);
                map.addMarker(home);

                // check permissions
                if (!PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {
                    permissionsManager.requestLocationPermissions(MainActivity.this);
                } else {
                    lastLocation = locationEngine.getLastLocation();
                    setInitialMapPosition();
                    enableLocation(true);

                }

                if (lastLocation == null) {
                    //force initial location
                    LocationManager service = (LocationManager)
                            getSystemService(LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    String provider = service.getBestProvider(criteria, false);
                    lastLocation = service.getLastKnownLocation(provider);
                }
                fetchTheDogFromFirebase();
                uploadTheDronesToGeofire();
                updateDroneDescriptions(droneDescription);
                updateDronePuroposes(dronePurpose);
                updateDrones(drones);

                updateNoFlyZones(NoFlyZonesPolygons, NoFlyZones);

                map.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition position) {
                        LatLngBounds bounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;
                        double northLat = bounds.getLatNorth();
                        double eastLng = bounds.getLonEast();
                        LatLng center = bounds.getCenter();
                        LatLng northEastCorner = new LatLng(northLat, eastLng);
                        radius = center.distanceTo(northEastCorner);
                        radius = radius * .001;
                        geoQuery.setRadius(radius);
                    }
                });
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
        lastLocation = locationEngine.getLastLocation();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        if (locationEngine != null && locationEngineListener != null) {
            locationEngine.activate();
            locationEngine.requestLocationUpdates();
            locationEngine.addLocationEngineListener(locationEngineListener);
            lastLocation = locationEngine.getLastLocation();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();


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

    //User functions go below here

    private void fetchTheDogFromFirebase(){

        // This is the event listener from the firebase database for a single test user
        firebaseDatabaseLocationEventListener = firebaseDatabaseLocationReference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Double lat = dataSnapshot.child("lat").getValue(Double.class);
                Double lng = dataSnapshot.child("lng").getValue(Double.class);
                map.removeMarker(dog.getMarker());
                dog = new MarkerViewOptions().icon(dogicon).position(new LatLng(lat, lng)).title("moving marker").snippet("watch me go");
                map.addMarker(dog);
                Log.d(TAG, "Value is: " + lat + " " + lng);

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }

    private void uploadTheDronesToGeofire(){
        /*
        description needed
         */

        if(lastLocation != null){
            // this creates a few fake drone entries in the data base using the location I just got from the device
            // I've supplied offsets to each of them
            // TODO: these should be in their own app simulating flight

            geoFire.setLocation("Rouge One", new GeoLocation(lastLocation.getLatitude() + .0007, lastLocation.getLongitude() + .0008));
            geoFire.setLocation("Red One", new GeoLocation(lastLocation.getLatitude() + .0002, lastLocation.getLongitude() + .0003));
            geoFire.setLocation("Echo Seven", new GeoLocation(lastLocation.getLatitude() - .0009, lastLocation.getLongitude() - .0003));
            geoFire.setLocation("Jedi One", new GeoLocation(lastLocation.getLatitude() - .0002, lastLocation.getLongitude() + .0004));
            geoFire.setLocation("Red Leader", new GeoLocation(lastLocation.getLatitude() + .0005, lastLocation.getLongitude() - .0006));
            geoFire.setLocation("Red three", new GeoLocation(lastLocation.getLatitude() + .0008, lastLocation.getLongitude() + .0003));
            geoFire.setLocation("Red six", new GeoLocation(lastLocation.getLatitude() - .001, lastLocation.getLongitude() - .0009));
            geoFire.setLocation("Red twelve", new GeoLocation(lastLocation.getLatitude() - .0007, lastLocation.getLongitude() + .0014));
            geoFire.setLocation("Red five", new GeoLocation(lastLocation.getLatitude() + .0015, lastLocation.getLongitude() - .0019));

        }

    }

    private void updateNoFlyZones(final Map<String, Polygon>NoFlyZonesPolygons, final Map<String, ArrayList>NoFlyZones){

        // This is the event listener from the firebase database for a single test user
        firebaseDatabaseNoFlyZoneChildEventListener = firebaseDatabaseNoFlyZoneReference.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                // get the string containing the noflyzone points
                System.out.println(String.format("Key %s" ,dataSnapshot.getKey() ));

                if (dataSnapshot.getKey().equals("numberOfZones")) {
                    return;
                }
                String noflyzonedata = (String) dataSnapshot.getValue();
                // split the sting of no fly zone points into an array
                ArrayList<String> noFlyZonePoints = new ArrayList<>(Arrays.asList(noflyzonedata.split("\\s+")));
                // Add no flyzone to map of no fly zones
                NoFlyZones.put(dataSnapshot.getKey(), noFlyZonePoints);
                NoFlyZonesPolygons.put(dataSnapshot.getKey(), drawPolygon(map, NoFlyZones.get(dataSnapshot.getKey())));

            }
            //TODO update what to do with no-fly zones in these methods
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                // get the string containing the noflyzone points
                System.out.println(String.format("Key %s" ,dataSnapshot.getKey() ));

                if (dataSnapshot.getKey().equals("numberOfZones")) {
                    return;
                }
                String noflyzonedata = (String) dataSnapshot.getValue();
                // split the sting of no fly zone points into an array
                ArrayList<String> noFlyZonePoints = new ArrayList<>(Arrays.asList(noflyzonedata.split("\\s+")));
                // Add no flyzone to map of no fly zones
                NoFlyZones.put(dataSnapshot.getKey(), noFlyZonePoints);
                NoFlyZonesPolygons.put(dataSnapshot.getKey(), drawPolygon(map, NoFlyZones.get(dataSnapshot.getKey())));
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // clean up after a noflyzone has been removed from the database
                NoFlyZones.remove(dataSnapshot.getKey());
                NoFlyZonesPolygons.get(dataSnapshot.getKey()).remove();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {
                // get the string containing the noflyzone points
                System.out.println(String.format("Key %s" ,dataSnapshot.getKey() ));

                if (dataSnapshot.getKey().equals("numberOfZones")) {
                    return;
                }
                String noflyzonedata = (String) dataSnapshot.getValue();
                // split the sting of no fly zone points into an array
                ArrayList<String> noFlyZonePoints = new ArrayList<>(Arrays.asList(noflyzonedata.split("\\s+")));
                // Add no flyzone to map of no fly zones
                NoFlyZones.put(dataSnapshot.getKey(), noFlyZonePoints);
                NoFlyZonesPolygons.put(dataSnapshot.getKey(), drawPolygon(map, NoFlyZones.get(dataSnapshot.getKey())));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

    }

    private void updateDroneDescriptions(final Map<String, String>droneDescription) {

        // This is the event listener from the firebase database for a single test user
        firebaseDatbaseDescriptionChildEventListener = firebaseDatbaseDescriptionReference.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                System.out.println(String.format("Key %s", dataSnapshot.getKey()));
                String droneDescriptionStr = (String) dataSnapshot.getValue();
                droneDescription.put(dataSnapshot.getKey(), droneDescriptionStr);


            }

            //TODO update what to do with no-fly zones in these methods
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

                System.out.println(String.format("Key %s", dataSnapshot.getKey()));
                String droneDescriptionStr = (String) dataSnapshot.getValue();
                droneDescription.put(dataSnapshot.getKey(), droneDescriptionStr);

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // clean up after a noflyzone has been removed from the database
                droneDescription.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {

                System.out.println(String.format("Key %s", dataSnapshot.getKey()));
                String droneDescriptionStr = (String) dataSnapshot.getValue();
                droneDescription.put(dataSnapshot.getKey(), droneDescriptionStr);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void updateDronePuroposes(final Map<String, String>dronePurpose){

        // This is the event listener from the firebase database for a single test user
        firebaseDatabasePurposeChildEventListener = firebaseDatabasePurposeReference.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                System.out.println(String.format("Key %s" ,dataSnapshot.getKey() ));
                String droneDescriptionStr = (String) dataSnapshot.getValue();
                dronePurpose.put(dataSnapshot.getKey(), droneDescriptionStr);


            }
            //TODO update what to do with no-fly zones in these methods
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

                System.out.println(String.format("Key %s" ,dataSnapshot.getKey() ));
                String droneDescriptionStr = (String) dataSnapshot.getValue();
                dronePurpose.put(dataSnapshot.getKey(), droneDescriptionStr);

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // clean up after a noflyzone has been removed from the database
                dronePurpose.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {

                System.out.println(String.format("Key %s" ,dataSnapshot.getKey() ));
                String droneDescriptionStr = (String) dataSnapshot.getValue();
                dronePurpose.put(dataSnapshot.getKey(), droneDescriptionStr);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

    }

    private void updateDrones(final Map<String, Marker>drones){

        if (lastLocation != null){

            // this is were I set up the location based geoquery based off of the users location I got earlier
            geoQuery = geoFire.queryAtLocation(new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), radius);

            //  this is the event listener for the geoquery
           geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if(drones.get(key)!=null){
                        droneMarker = drones.get(key);
                        drones.remove(key);
                        map.removeMarker(droneMarker);
                    }
                    //map.removeMarker(geoFireDrone.getMarker());
                    geoFireDrone = new MarkerViewOptions().icon(drone).position(new LatLng(location.latitude, location.longitude)).title(String.format("%s", key)).snippet(String.format("Description: %s\nPurpose: %s", droneDescription.get(key), dronePurpose.get(key)));
                    droneMarker = map.addMarker(geoFireDrone);
                    drones.put(key, droneMarker);

                    System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                }

                @Override
                public void onKeyExited(String key) {
                    droneMarker = drones.get(key);
                    drones.remove(key);
                    map.removeMarker(droneMarker);

                    System.out.println(String.format("Key %s is no longer in the search area", key));
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                    droneMarker = drones.get(key);
                    drones.remove(key);

                    ValueAnimator markerAnimator = ObjectAnimator.ofObject(droneMarker, "position",
                            new LatLngEvaluator(), droneMarker.getPosition(), new LatLng(location.latitude, location.longitude));
                    markerAnimator.setDuration(200);
                    markerAnimator.start();
                    drones.put(key, droneMarker);

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
        }
    }

    private Polygon drawPolygon(MapboxMap mapboxMap, ArrayList noFlyZone) {
        /*
        Draw one no-fly zone on the map
         */

        List<LatLng> polygon = new ArrayList<>();
        for(Iterator<String> i = noFlyZone.iterator(); i.hasNext(); ) {
            String latLngAlt = i.next();
            ArrayList<String> eachPoint = new ArrayList<String>(Arrays.asList(latLngAlt.split(",")));
            double lat = Double.parseDouble(eachPoint.get(1));
            double lng = Double.parseDouble(eachPoint.get(0));
            polygon.add(new LatLng(lat, lng));

        }

        Polygon crurrentNoflyzon = mapboxMap.addPolygon(new PolygonOptions()
                .addAll(polygon)
                .fillColor(Color.RED)
                .alpha((float)0.3));

       return crurrentNoflyzon;
    }

    private void toggleGps(boolean enableGps) {
        /*
        description needed
         */

        if (enableGps) {
            // Check if user has granted location permission
            permissionsManager = new PermissionsManager(this);
            if (!PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {
                permissionsManager.requestLocationPermissions(MainActivity.this);
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
            lastLocation = locationEngine.getLastLocation();
            if (lastLocation != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
            }

            locationEngineListener = new LocationEngineListener() {
                @Override
                public void onConnected() {
                    locationEngine.requestLocationUpdates();
                }
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                        locationEngine.removeLocationEngineListener(this);

                    }
                }
            };
            locationEngine.addLocationEngineListener(locationEngineListener);
            floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setInitialMapPosition() {
        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));

        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "This app needs location permissions in order to show its functionality.",
                Toast.LENGTH_LONG).show();

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocation(true);
        } else {
            Toast.makeText(this, "You didn't grant location permissions.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {
        // Method is used to interpolate the marker animation.

        private LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }

}


