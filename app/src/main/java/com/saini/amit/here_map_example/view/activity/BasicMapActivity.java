package com.saini.amit.here_map_example.view.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.internal.restrouting.Route;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.routing.RoutePlan;
import com.saini.amit.here_map_example.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class BasicMapActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            /*Manifest.permission.WRITE_EXTERNAL_STORAGE,*/
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };


    // map embedded in the map fragment
    private Map map = null;
    private MapMarker mapMarker = null;

    private boolean isPaused = false;
    private GeoCoordinate currentGeoCoordinate;

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionAndStartApp();
    }

    @Override
    protected void onPause() {
//        stopPositionManager();
        super.onPause();
    }

    private void checkPermissionAndStartApp() {
        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            setupMapFragmentView();
        } else {
            ActivityCompat
                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    private void setupMapFragmentView() {
        initialize();
    }

    private void initialize() {
        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        startPositionManager();
        mapFragment.init(onEngineInitListener);
    }

    private OnEngineInitListener onEngineInitListener = new OnEngineInitListener() {
        @Override
        public void onEngineInitializationCompleted(Error error) {
            if (error == OnEngineInitListener.Error.NONE) {
                if (null == mapFragment) {
                    return;
                }
                mapFragment.getMapGesture().addOnGestureListener(onGestureListener);

                createMapMarker();
                setMapThings();

//                getPositionManager().start(PositioningManager.LocationMethod.GPS_NETWORK);
//                final RoutePlan routePlan = new RoutePlan();
//
//                 these two waypoints cover suburban roads
//                routePlan.addWaypoint(new GeoCoordinate(48.98382, 2.50292));
//                routePlan.addWaypoint(new GeoCoordinate(48.95602, 2.45939));

                try {
                    // calculate a route for navigation
//                    CoreRouter coreRouter = new CoreRouter();
//                    Route coreRoute = new Route();
//                    coreRoute.a();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(BasicMapActivity.this,
                        "ERROR: Cannot initialize Map with error " + error,
                        Toast.LENGTH_LONG).show();
                System.out.println("ERROR: Cannot initialize Map Fragment");
            }
        }
    };

    private void setMapThings() {
        // retrieve a reference of the map from the map fragment
        map = mapFragment.getMap();
        if (null != map) {
            startPositionManager();
            map.addTransformListener(onTransformListener);
            map.getPositionIndicator().setVisible(true);
            map.getPositionIndicator().setAccuracyIndicatorVisible(true);
            // Set the map center to the Vancouver region (no animation)
            map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0), Map.Animation.NONE);
            GeoPosition geoPosition;
            if (getPositionManager().hasValidPosition()) {
                geoPosition = getPositionManager().getPosition();
            } else {
                geoPosition = getPositionManager().getLastKnownPosition();
            }

            if (null != geoPosition)
                currentGeoCoordinate = geoPosition.getCoordinate();

            setMapCurrentCoordinate(/*currentGeoCoordinate*/);
        }
    }

    private void setMapCurrentCoordinate(/*GeoCoordinate currentGeoCoordinate*/) {
        if (null != map) {
            // Set the zoom level to the average between min and max
            double minimumZoomLevel = map.getMinZoomLevel();
            double maxZoomLevel = map.getMaxZoomLevel();
            double zoomLevel = (maxZoomLevel + minimumZoomLevel) / 2;


            if (null != currentGeoCoordinate) {
                map.setCenter(currentGeoCoordinate, Map.Animation.NONE);
                map.setZoomLevel(zoomLevel);
            }
        }
    }

    private void createMapMarker() {
        // create a map marker to show current position
        Image icon = new Image();
        mapMarker = new MapMarker();
        try {
            icon.setImageResource(R.drawable.gps_position);
            mapMarker.setIcon(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PositioningManager getPositionManager() {
        return PositioningManager.getInstance();
    }

    private void addPositionListener() {
        getPositionManager().addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(onPositionChangedListener));
    }

    private void removePositionListener() {
        getPositionManager().removeListener(onPositionChangedListener);
    }

    private void startPositionManager() {
        if (null != getPositionManager()) {
            isPaused = false;
            getPositionManager().start(PositioningManager.LocationMethod.GPS_NETWORK);
        }
    }

    private void stopPositionManager() {
        if (null != getPositionManager()) {
            getPositionManager().stop();
            isPaused = true;
        }
    }

    private MapGesture.OnGestureListener onGestureListener = new MapGesture.OnGestureListener() {
        @Override
        public void onPanStart() {

        }

        @Override
        public void onPanEnd() {

        }

        @Override
        public void onMultiFingerManipulationStart() {

        }

        @Override
        public void onMultiFingerManipulationEnd() {

        }

        @Override
        public boolean onMapObjectsSelected(List<ViewObject> list) {
            return false;
        }

        @Override
        public boolean onTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public void onPinchLocked() {

        }

        @Override
        public boolean onPinchZoomEvent(float v, PointF pointF) {
            return false;
        }

        @Override
        public void onRotateLocked() {

        }

        @Override
        public boolean onRotateEvent(float v) {
            return false;
        }

        @Override
        public boolean onTiltEvent(float v) {
            return false;
        }

        @Override
        public boolean onLongPressEvent(PointF pointF) {
            return false;
        }

        @Override
        public void onLongPressRelease() {

        }

        @Override
        public boolean onTwoFingerTapEvent(PointF pointF) {
            return false;
        }
    };

    private Map.OnTransformListener onTransformListener = new Map.OnTransformListener() {
        @Override
        public void onMapTransformStart() {
        }

        @Override
        public void onMapTransformEnd(MapState mapsState) {
            // do not start RoadView and its listener until moving map to current position has
            // completed
            /*if (m_returningToRoadViewMode) {
                NavigationManager.getInstance().setMapUpdateMode(NavigationManager.MapUpdateMode
                        .ROADVIEW);
                NavigationManager.getInstance().getRoadView().addListener(new
                        WeakReference<NavigationManager.RoadView.Listener>(roadViewListener));
                m_returningToRoadViewMode = false;
            }*/
        }

    };

    private PositioningManager.OnPositionChangedListener onPositionChangedListener = new PositioningManager.OnPositionChangedListener() {
        @Override
        public void onPositionUpdated(PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition, boolean b) {

        }

        @Override
        public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {

        }
    };

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                int permissionCount = 0;

                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat
                                .shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                            + " not granted. "
                                            + "Please go to settings and turn on for this app",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show();
                        }
                    } else if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                        permissionCount++;
                    }
                }

                if (permissionCount == permissions.length) {
                    setupMapFragmentView();
                }
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onDestroy() {
        removePositionListener();
        super.onDestroy();
    }
}