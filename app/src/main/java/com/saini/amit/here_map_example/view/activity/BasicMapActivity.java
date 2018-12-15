package com.saini.amit.here_map_example.view.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
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
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.mapping.PositionIndicator;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.search.Address;
import com.here.android.mpa.search.Category;
import com.here.android.mpa.search.CategoryFilter;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.ExploreRequest;
import com.here.android.mpa.search.GeocodeRequest2;
import com.here.android.mpa.search.GeocodeResult;
import com.here.android.mpa.search.HereRequest;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.ReverseGeocodeRequest;
import com.here.android.mpa.search.SearchRequest;
import com.saini.amit.here_map_example.R;
import com.saini.amit.here_map_example.view.Constant;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class BasicMapActivity extends AppCompatActivity {

    public static List<DiscoveryResult> s_ResultList;
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            /*Manifest.permission.WRITE_EXTERNAL_STORAGE,*/
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };


    // map embedded in the map fragment
    private Map map = null;
    private MapMarker mapMarker = null;
    private List<MapObject> m_mapObjectList = new ArrayList<>();

    private List<MapObject> routeMapObjects = new ArrayList<>();
    private Button m_placeDetailButton;

    private boolean isStarted = false;
    private GeoCoordinate currentGeoCoordinate;


    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initResultListButton();
        initSearchControlButtons();
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
                /*do {

                } while (!isStarted);*/

                while (!isStarted) {
                    startPositionManager();
                }

                createMapMarker();
                setMapThings();

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
            map.addTransformListener(onTransformListener);
            setCurrentLocation();
            addPositionListener();
//            calculateRoute();
//            triggerGeocodeRequest();
//            triggerRevGeocodeRequest();
        }
    }

    private void setCurrentLocation() {
        PositionIndicator positionIndicator = map.getPositionIndicator();

        if (null != positionIndicator) {

            positionIndicator.setVisible(true);
            positionIndicator.setAccuracyIndicatorVisible(true);
            // Set the map center to the Vancouver region (no animation)
//            map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0), Map.Animation.NONE);

            currentGeoCoordinate = getCurrentGeoCoordinate();
            setMapCurrentCoordinate(/*currentGeoCoordinate*/);

            triggerGeocodeRequest();
        }
    }

    private GeoCoordinate getCurrentGeoCoordinate() {
        GeoPosition geoPosition;
        if (getPositionManager().hasValidPosition()) {
            geoPosition = getPositionManager().getPosition();
        } else {
            geoPosition = getPositionManager().getLastKnownPosition();
        }

        if (null != geoPosition) {
            Log.e("geoPosition", geoPosition.toString());
            Log.e("coordinate", geoPosition.getCoordinate().toString());

            return geoPosition.getCoordinate();
        } else {
            return null;
        }
    }

    private void setMapCurrentCoordinate(/*GeoCoordinate currentGeoCoordinate*/) {
        if (null != map) {
            if (null != currentGeoCoordinate) {
                map.setCenter(currentGeoCoordinate, Map.Animation.NONE);
                setMapZooming();
            }
        }
    }

    private void setMapZooming() {
        if (null != map) {
            // Set the zoom level to the average between min and max
            double minimumZoomLevel = map.getMinZoomLevel();
            double maxZoomLevel = map.getMaxZoomLevel();
            double zoomLevel = (maxZoomLevel + minimumZoomLevel) / 2;
            map.setZoomLevel(zoomLevel);

        }
    }

    RoutePlan routePlan = null;

    private void calculateRoute(GeoCoordinate fromGeoCoordinate, GeoCoordinate ToGeoCoordinate) {
        // Declare the rm variable (the RouteManager)
        clearPreviousRoute();
        RouteManager rm = new RouteManager();


        // Create the RoutePlan and add two waypoints
        routePlan = new RoutePlan();
//        routePlan.addWaypoint(new GeoCoordinate(28.704060, 77.102493));   ////Delhi lat long
//        currentGeoCoordinate = getCurrentGeoCoordinate();
//        routePlan.addWaypoint(currentGeoCoordinate);  ///
//        routePlan.addWaypoint(new GeoCoordinate(28.459497, 77.026634));


        routePlan.addWaypoint(fromGeoCoordinate);
        routePlan.addWaypoint(ToGeoCoordinate);


        routePlan.setRouteOptions(getRouteOptions());


        // Calculate the route
//        rm.calculateRoute(routePlan, new RouteListener());
        if (!rm.isBusy())
            rm.calculateRoute(routePlan, routeManagerListener);
    }

    private RouteOptions getRouteOptions() {
        // Create the RouteOptions and set its transport mode & routing type
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routeOptions.setStartDirection(60);

        return routeOptions;
    }


    private void clearPreviousRoute() {
        if (null != routeMapObjects && routeMapObjects.size() > 0) {
            map.removeMapObjects(routeMapObjects);
            routeMapObjects.clear();
        }
    }

    private RouteManager.Listener routeManagerListener = new RouteManager.Listener() {
        // Method defined in Listener
        @Override
        public void onProgress(int i) {
            // Display a message indicating calculation progress
        }

        // Method defined in Listener
        @Override
        public void onCalculateRouteFinished(RouteManager.Error error, List<RouteResult> routeResult) {
            // If the route was calculated successfully
            if (error == RouteManager.Error.NONE) {
                // Render the route on the map
                List<MapObject> mapObjectList = new ArrayList<>();
                if (null != routeResult) {
                    for (RouteResult routeResultObj : routeResult) {
                        if (null != routeResultObj) {
                            MapRoute mapRoute = new MapRoute(routeResultObj.getRoute());
                            mapObjectList.add(mapRoute);
                            routeMapObjects.add(mapRoute);
                        }
                    }
                    map.addMapObjects(mapObjectList);
//                    MapRoute.setRenderType(MapRoute.RenderType.PRIMARY);


                    if (routeResult.size() > 0) {
                        zoomMap(routeResult.get(0).getRoute().getBoundingBox());
                    }
                }

//                setMapZooming();
//                MapRoute mapRoute = new MapRoute(routeResult.get(0).getRoute());
//                map.addMapObject(mapRoute);
            } else {
                // Display a message indicating route calculation failure
                Toast.makeText(BasicMapActivity.this,
                        "Error:route calculation error " + error,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    /*private class RouteListener implements RouteManager.Listener {

        // Method defined in Listener
        public void onProgress(int percentage) {
            // Display a message indicating calculation progress
        }

        // Method defined in Listener
        public void onCalculateRouteFinished(RouteManager.Error error, List<RouteResult> routeResult) {
            // If the route was calculated successfully
            if (error == RouteManager.Error.NONE) {
                // Render the route on the map
                List<MapObject> mapObjectList = new ArrayList<>();
                if (null != routeResult) {
                    for (RouteResult routeResultObj : routeResult) {
                        if (null != routeResultObj) {
                            MapRoute mapRoute = new MapRoute(routeResultObj.getRoute());
                            mapObjectList.add(mapRoute);
                            routeMapObjects.add(mapRoute);
                        }
                    }
                    map.addMapObjects(mapObjectList);


                    if (routeResult.size() > 0) {
                        zoomMap(routeResult.get(0).getRoute().getBoundingBox());
                    }
                }

//                setMapZooming();
//                MapRoute mapRoute = new MapRoute(routeResult.get(0).getRoute());
//                map.addMapObject(mapRoute);
            } else {
                // Display a message indicating route calculation failure
                Toast.makeText(BasicMapActivity.this,
                        "Error:route calculation error " + error,
                        Toast.LENGTH_LONG).show();
            }
        }
    }*/

    private void zoomMap(GeoBoundingBox geoBoundingBox) {
        map.zoomTo(geoBoundingBox, Map.Animation.LINEAR, Map.MOVE_PRESERVE_ORIENTATION);
    }

    /*private void showNavigation(List<RouteResult> routeResult){
        Route route = routeResult.get(0).getRoute();

        // move the map to the first waypoint which is starting point of
        // the route
        map.setCenter(routePlan.getWaypointAt(0),
                Map.Animation.NONE);

        // setting MapUpdateMode to RoadView will enable automatic map
        // movements and zoom level adjustments
        NavigationManager.getInstance().setMapUpdateMode
                (NavigationManager.MapUpdateMode.ROADVIEW);

        // adjust tilt to show 3D view
        m_map.setTilt(80);

        // adjust transform center for navigation experience in portrait
        // view
        m_mapTransformCenter = new PointF(m_map.getTransformCenter().x, (m_map
                .getTransformCenter().y * 85 / 50));
        m_map.setTransformCenter(m_mapTransformCenter);

        // create a map marker to show current position
        Image icon = new Image();
        m_positionIndicatorFixed = new MapMarker();
        try {
            icon.setImageResource(R.drawable.gps_position);
            m_positionIndicatorFixed.setIcon(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_positionIndicatorFixed.setVisible(true);
        m_positionIndicatorFixed.setCoordinate(m_map.getCenter());
        map.addMapObject(m_positionIndicatorFixed);

        m_mapFragment.getPositionIndicator().setVisible(false);

        NavigationManager.getInstance().setMap(map);

        // listen to real position updates. This is used when RoadView is
        // not active.
        PositioningManager.getInstance().addListener(
                new WeakReference<PositioningManager.OnPositionChangedListener>(
                        mapPositionHandler));

        // listen to updates from RoadView which tells you where the map
        // center should be situated. This is used when RoadView is active.
        NavigationManager.getInstance().getRoadView().addListener(new
                WeakReference<NavigationManager.RoadView.Listener>(roadViewListener));

        // start navigation simulation travelling at 13 meters per second
        NavigationManager.getInstance().simulate(route, 13);
    }*/

    private void triggerGeocodeRequest() {
//        m_resultTextView.setText("");
        /*
         * Create a GeocodeRequest object with the desired query string, then set the search area by
         * providing a GeoCoordinate and radius before executing the request.
         */
//        String query = "4350 Still Creek Dr,Burnaby";
        if (null != currentGeoCoordinate) {

            String query = "Bar";
            GeocodeRequest2 geocodeRequest = new GeocodeRequest2(query);
//        GeoCoordinate coordinate = new GeoCoordinate(49.266787, -123.056640);
//        GeoCoordinate coordinate = new GeoCoordinate(49.266787, -123.056640);
            geocodeRequest.setSearchArea(currentGeoCoordinate, 500);
            geocodeRequest.execute(new ResultListener<List<GeocodeResult>>() {
                @Override
                public void onCompleted(List<GeocodeResult> results, ErrorCode errorCode) {
                    if (errorCode == ErrorCode.NONE) {
                        /*
                         * From the result object, we retrieve the location and its coordinate and
                         * display to the screen. Please refer to HERE Android SDK doc for other
                         * supported APIs.
                         */
                        StringBuilder sb = new StringBuilder();
                        for (GeocodeResult result : results) {
                            sb.append(result.getLocation().getCoordinate().toString());
                            sb.append("\n");
                        }
                        Log.e("sb text", sb.toString());
//                    updateTextView(sb.toString());
                    } else {
//                    updateTextView("ERROR:Geocode Request returned error code:" + errorCode);
                    }
                }
            });
        }
    }

    private void triggerRevGeocodeRequest() {
//        m_resultTextView.setText("");
        /* Create a ReverseGeocodeRequest object with a GeoCoordinate. */
        GeoCoordinate coordinate = new GeoCoordinate(15.33757, 120.69949);
        ReverseGeocodeRequest revGecodeRequest = new ReverseGeocodeRequest(getCurrentGeoCoordinate());

        revGecodeRequest.execute(new ResultListener<Address>() {
            @Override
            public void onCompleted(Address address, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    /*
                     * From the location object, we retrieve the address and display to the screen.
                     * Please refer to HERE Android SDK doc for other supported APIs.
                     */
//                    updateTextView(location.getAddress().toString());

                    Log.e("sb text", address.toString());
                } else {
//                    updateTextView("ERROR:RevGeocode Request returned error code:" + errorCode);
                }
            }
        });
    }


    private ResultListener<DiscoveryResultPage> discoveryResultPageListener = new ResultListener<DiscoveryResultPage>() {
        @Override
        public void onCompleted(DiscoveryResultPage discoveryResultPage, ErrorCode errorCode) {
            if (errorCode == ErrorCode.NONE) {
                /* No error returned,let's handle the results */
                m_placeDetailButton.setVisibility(View.VISIBLE);

                /*
                 * The result is a DiscoveryResultPage object which represents a paginated
                 * collection of items.The items can be either a PlaceLink or DiscoveryLink.The
                 * PlaceLink can be used to retrieve place details by firing another
                 * PlaceRequest,while the DiscoveryLink is designed to be used to fire another
                 * DiscoveryRequest to obtain more refined results.
                 */
                s_ResultList = discoveryResultPage.getItems();
                for (DiscoveryResult item : s_ResultList) {
                    /*
                     * Add a marker for each result of PlaceLink type.For best usability, map can be
                     * also adjusted to display all markers.This can be done by merging the bounding
                     * box of each result and then zoom the map to the merged one.
                     */
                    if (item.getResultType() == DiscoveryResult.ResultType.PLACE ||
                            item.getResultType() == DiscoveryResult.ResultType.DISCOVERY ||
                            item.getResultType() == DiscoveryResult.ResultType.UNKNOWN) {
                        PlaceLink placeLink = (PlaceLink) item;
                        addMarkerAtPlace(placeLink);
                    }
                }
            } else {
                Toast.makeText(BasicMapActivity.this,
                        "ERROR:Discovery search request returned return error code+ " + errorCode,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void addMarkerAtPlace(PlaceLink placeLink) {
        Image img = new Image();
        try {
            img.setImageResource(R.drawable.marker);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MapMarker mapMarker = new MapMarker();
        mapMarker.setIcon(img);
        mapMarker.setCoordinate(new GeoCoordinate(placeLink.getPosition()));
        mapMarker.setTitle(placeLink.getTitle());
//        mapMarker.setVisible(true);
        mapMarker.setDescription(placeLink.getDetailsRequest().toString());

        map.addMapObject(mapMarker);
        map.setInfoBubbleAdapter(infoBubbleAdapter);
        m_mapObjectList.add(mapMarker);
    }

    private Map.InfoBubbleAdapter infoBubbleAdapter = new Map.InfoBubbleAdapter() {
        @Override
        public View getInfoBubbleContents(MapMarker mapMarker) {

            return null;
        }

        @Override
        public View getInfoBubble(MapMarker mapMarker) {
            View view = LayoutInflater.from(BasicMapActivity.this).inflate(R.layout.view_info_bubble, null, false);
            TextView tvTitle = view.findViewById(R.id.tv_title);
            TextView tvDistance = view.findViewById(R.id.tv_distance);
            tvTitle.setText(mapMarker.getTitle());
            tvDistance.setText(mapMarker.getDescription());
            return view;
        }
    };


    private void initSearchControlButtons() {
        Button aroundRequestButton = (Button) findViewById(R.id.aroundRequestBtn);
        aroundRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Trigger an AroundRequest based on the current map center and the filter for
                 * Eat&Drink category.Please refer to HERE Android SDK API doc for other supported
                 * location parameters and categories.
                 */
                cleanMap();
                /*AroundRequest aroundRequest = new AroundRequest();
                aroundRequest.setSearchCenter(m_map.getCenter());
                CategoryFilter filter = new CategoryFilter();
                filter.add(Category.Global.EAT_DRINK);
                aroundRequest.setCategoryFilter(filter);
                aroundRequest.execute(discoveryResultPageListener);*/
            }
        });

        Button exploreRequestButton = (Button) findViewById(R.id.exploreRequestBtn);
        exploreRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Trigger an ExploreRequest based on the bounding box of the current map and the
                 * filter for Shopping category.Please refer to HERE Android SDK API doc for other
                 * supported location parameters and categories.
                 */
//                cleanMap();
                ExploreRequest exploreRequest = new ExploreRequest();
                exploreRequest.setSearchArea(map.getBoundingBox());
                CategoryFilter filter = new CategoryFilter();
                filter.add(Category.Global.SHOPPING);
                filter.add(Category.Global.BUSINESS_SERVICES);
                filter.add(Category.Global.EAT_DRINK);
                filter.add(Category.Global.TRANSPORT);
                filter.add(Category.Global.SIGHTS_MUSEUMS);
                filter.add(Category.Global.FACILITIES);
                filter.add(Category.Global.LEISURE_OUTDOOR);
                filter.add(Category.Global.GOING_OUT);
                filter.add(Category.Global.NATURAL_GEOGRAPHICAL);
                filter.add(Category.Global.ADMINISTRATIVE_AREAS_BUILDINGS);
                filter.add(Category.Global.ACCOMMODATION);
                exploreRequest.setCategoryFilter(filter);
                exploreRequest.execute(discoveryResultPageListener);
            }
        });

        Button hereRequestButton = (Button) findViewById(R.id.hereRequestBtn);
        hereRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Trigger a HereRequest based on the current map center.Please refer to HERE
                 * Android SDK API doc for other supported location parameters and categories.
                 */
                cleanMap();
                HereRequest hereRequest = new HereRequest();
                hereRequest.setSearchCenter(map.getCenter());
                hereRequest.execute(discoveryResultPageListener);
            }
        });

        final EditText edtSearchBar = findViewById(R.id.edt_search_bar);
        Button searchRequestButton = (Button) findViewById(R.id.searchRequestBtn);
        searchRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Trigger a SearchRequest based on the current map center and search query
                 * "Hotel".Please refer to HERE Android SDK API doc for other supported location
                 * parameters and categories.
                 */
                cleanMap();
                String searchingStr = edtSearchBar.getText().toString().trim();

                if (TextUtils.isEmpty(searchingStr)) {
                    searchingStr = "Bar";
                    Toast.makeText(BasicMapActivity.this, "By Default Searching For " + searchingStr, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BasicMapActivity.this, "Searching For " + searchingStr, Toast.LENGTH_SHORT).show();
                }
                SearchRequest searchRequest = new SearchRequest(searchingStr);
                searchRequest.setSearchCenter(map.getCenter());
                searchRequest.execute(discoveryResultPageListener);
            }
        });

    }

    private void initResultListButton() {
        m_placeDetailButton = (Button) findViewById(R.id.resultListBtn);
        m_placeDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Open the ResultListActivity */
                if (null != s_ResultList && s_ResultList.size() > 0) {
                    Intent intent = new Intent(BasicMapActivity.this, ResultListActivity.class);
                    startActivityForResult(intent, Constant.requestCode.BAR_SELECTED);
                } else {
                    Toast.makeText(BasicMapActivity.this, "their is no current location of device," +
                                    "please ON your device location.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void cleanMap() {
        if (!m_mapObjectList.isEmpty()) {
            map.removeMapObjects(m_mapObjectList);
            m_mapObjectList.clear();
        }
        m_placeDetailButton.setVisibility(View.GONE);
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
            Log.e("startPositionManager ", isStarted + "  ");
            isStarted = getPositionManager().start(PositioningManager.LocationMethod.GPS_NETWORK);
        }
    }

    private void stopPositionManager() {
        if (null != getPositionManager()) {
            getPositionManager().stop();
            isStarted = true;
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
            for (ViewObject object : list) {
                if (object.getBaseType() == ViewObject.Type.USER_OBJECT && ((MapObject) object).getType() == MapObject.Type.MARKER) {
                    MapMarker mapMarker = (MapMarker) object;
                    System.out.println("Title is................." + mapMarker.getTitle());
                    System.out.println("Desc is................." + mapMarker.getDescription());


                    if (!mapMarker.isInfoBubbleVisible()) {
//                        mapMarker.getInfoBubbleContents();
                        mapMarker.getInfoBubbleHashCode();
                    } else {
                        mapMarker.hideInfoBubble();
                    }
                    return false;
                }
            }
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
            if (isStarted) {
                currentGeoCoordinate = geoPosition.getCoordinate();
                setMapCurrentCoordinate();
            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.requestCode.BAR_SELECTED:
                if (resultCode == Activity.RESULT_OK) {
//                    String geoCoordinate = data.getStringExtra("DISCOVERY_RESULT");

                    double latitude = data.getDoubleExtra("LAT", 0);
                    double longitude = data.getDoubleExtra("LON", 0);
                    double altitude = data.getDoubleExtra("ALT", 0);

                    GeoCoordinate newGeoCoordinate = new GeoCoordinate(latitude, longitude, altitude);

                    if (null != currentGeoCoordinate && null != newGeoCoordinate)
                        calculateRoute(currentGeoCoordinate, newGeoCoordinate);

                }
                break;
        }
    }
}