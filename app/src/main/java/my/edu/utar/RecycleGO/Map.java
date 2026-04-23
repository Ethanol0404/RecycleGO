package my.edu.utar.RecycleGO;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleCenter;
import my.edu.utar.RecycleGO.database.RecycleRequest;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Map extends Fragment {

    private MapView mapView;
    private IMapController mapController;
    private FusedLocationProviderClient fusedLocationClient;
    private List<RecycleCenter> centerList = new ArrayList<>();
    private MyLocationNewOverlay locationOverlay;
    private RecyclerView recyclerView;
    private AdapterRecycleCenter adapter;
    private boolean isFirstFix = true;
    private Location currentUserLocation;
    private java.util.Map<RecycleCenter, Marker> centerToMarkerMap = new java.util.HashMap<>();
    private Polyline selectionLine;
    private boolean isProgrammaticUpdate = false;
    private boolean dataLoaded = false;
    
    private String flow = "NORMAL";
    private RecycleRequest draftRequest;
    private FirestoreManager firestoreManager;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || 
                    (coarseLocationGranted != null && coarseLocationGranted)) {
                    startLocationTracking();
                } else {
                    Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    public Map() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
        
        // Fix: Use non-deprecated SharedPreferences access
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        Configuration.getInstance().load(ctx, prefs);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        firestoreManager = new FirestoreManager();

        if (getArguments() != null) {
            flow = getArguments().getString("flow", "NORMAL");
            if (flow.equals("STATUS_TO_FORM")) {
                draftRequest = new RecycleRequest();
                draftRequest.setUserId(getArguments().getString("userId"));
                draftRequest.setCategory(getArguments().getString("category"));
                draftRequest.setDate(getArguments().getString("date"));
                draftRequest.setContact(getArguments().getString("contact"));
                draftRequest.setRemarks(getArguments().getString("remarks"));
                draftRequest.setStatus("Requesting");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (getActivity() instanceof FrameActivity) {
            ((FrameActivity) getActivity()).setHeaderVisible(false);
        }

        View view = inflater.inflate(R.layout.activity_map, container, false);
        mapView = view.findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);
        
        GeoPoint startPoint = new GeoPoint(3.1390, 101.6869);
        mapController.setCenter(startPoint);

        recyclerView = view.findViewById(R.id.recycler_horizontal_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        setupAdapter();
        
        // Sync Map when Carousel is swiped
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isProgrammaticUpdate) {
                    View centerView = snapHelper.findSnapView(layoutManager);
                    if (centerView != null) {
                        int pos = layoutManager.getPosition(centerView);
                        if (pos != RecyclerView.NO_POSITION && pos < centerList.size()) {
                            isProgrammaticUpdate = true;
                            RecycleCenter center = centerList.get(pos);
                            updateOverview(new GeoPoint(center.latitude, center.longitude));
                            
                            Marker marker = centerToMarkerMap.get(center);
                            if (marker != null) {
                                marker.showInfoWindow();
                            }
                            
                            // Maintain flag until map animation likely finishes to prevent feedback loop
                            recyclerView.postDelayed(() -> isProgrammaticUpdate = false, 1500);
                        }
                    }
                }
            }
        });

        // Sync Carousel when Map is moved/zoomed
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (!isProgrammaticUpdate) {
                    syncCarouselToMapCenter(layoutManager);
                }
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                if (!isProgrammaticUpdate) {
                    syncCarouselToMapCenter(layoutManager);
                }
                return false;
            }
        });

        checkPermissionsAndStart();
        loadAndPopulateData();

        if ("STATUS_TO_FORM".equals(flow)) {
            Toast.makeText(getContext(), "Select a center to complete your request", Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void syncCarouselToMapCenter(LinearLayoutManager layoutManager) {
        if (centerList.isEmpty() || recyclerView == null) return;

        IGeoPoint mapCenter = mapView.getMapCenter();
        double minDistanceSq = Double.MAX_VALUE;
        int closestIndex = -1;

        for (int i = 0; i < centerList.size(); i++) {
            RecycleCenter center = centerList.get(i);
            double dLat = center.latitude - mapCenter.getLatitude();
            double dLon = center.longitude - mapCenter.getLongitude();
            double distSq = dLat * dLat + dLon * dLon;
            
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closestIndex = i;
            }
        }

        if (closestIndex != -1) {
            final int index = closestIndex;
            recyclerView.post(() -> {
                int firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
                if (firstVisible != index) {
                    isProgrammaticUpdate = true;
                    recyclerView.smoothScrollToPosition(index);
                    
                    // Show info window for the closest marker
                    Marker marker = centerToMarkerMap.get(centerList.get(index));
                    if (marker != null) {
                        marker.showInfoWindow();
                    }
                    
                    // Reset flag after animation might have started
                    recyclerView.postDelayed(() -> isProgrammaticUpdate = false, 1000);
                }
            });
        }
    }

    private void setupAdapter() {
        adapter = new AdapterRecycleCenter(centerList, new AdapterRecycleCenter.OnItemClickListener() {
            @Override
            public void onItemClick(RecycleCenter center) {
                isProgrammaticUpdate = true;
                GeoPoint point = new GeoPoint(center.latitude, center.longitude);
                updateOverview(point);
                Marker marker = centerToMarkerMap.get(center);
                if (marker != null) marker.showInfoWindow();
                
                // Maintain flag until map animation likely finishes
                recyclerView.postDelayed(() -> isProgrammaticUpdate = false, 1500);
            }

            @Override
            public void onRecycleClick(RecycleCenter center) {
                handleSelection(center);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void handleSelection(RecycleCenter center) {
        if ("STATUS_TO_FORM".equals(flow) && draftRequest != null) {
            draftRequest.setCenterId(center.id);
            draftRequest.setCenterName(center.name);
            firestoreManager.submitRequest(draftRequest, new FirestoreManager.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Request Submitted!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new RecycleStatus())
                            .commit();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Path 2: Map -> Form -> Status
            Fragment nextFragment = new PickUpActivity();
            Bundle args = new Bundle();
            args.putString("flow", "MAP_TO_FORM");
            args.putString("centerId", center.id);
            args.putString("centerName", center.name);
            nextFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationTracking() {
        if (!isAdded()) return;

        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER);

        locationOverlay = new MyLocationNewOverlay(provider, mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        locationOverlay.runOnFirstFix(() -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    GeoPoint myLocation = locationOverlay.getMyLocation();
                    if (myLocation != null) {
                        Location loc = new Location("");
                        loc.setLatitude(myLocation.getLatitude());
                        loc.setLongitude(myLocation.getLongitude());


                        boolean isFirstLocation = (currentUserLocation == null);

                        if (isFirstLocation || loc.distanceTo(currentUserLocation) > 50) {
                            currentUserLocation = loc;

                            // 2. Sort the centers based on new distance
                            sortCenters(loc);

                            if (isFirstLocation) {
                                adapter.notifyDataSetChanged();
                                addMarkers();
                            } else {

                                adapter.notifyItemRangeChanged(0, centerList.size());

                                updateMarkerDistances();
                            }
                        }
                    }
                });
            }
        });

        mapView.getOverlays().add(locationOverlay);
    }

    private void loadAndPopulateData() {
        firestoreManager.getCentersCollection().get().addOnSuccessListener(queryDocumentSnapshots -> {
            centerList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                RecycleCenter center = doc.toObject(RecycleCenter.class);
                centerList.add(center);
            }
            if (currentUserLocation != null) {
                sortCenters(currentUserLocation);
            }
            adapter.notifyDataSetChanged();
            addMarkers();
            dataLoaded = true;
        });
    }

    private void addMarkers() {
        if (!isAdded()) return;
        
        List<Overlay> overlays = mapView.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            if (overlays.get(i) instanceof Marker) {
                overlays.remove(i);
            }
        }
        centerToMarkerMap.clear();

        for (RecycleCenter center : centerList) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(center.latitude, center.longitude));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(center.name);
            
            String snippet = center.supportedServices + "\nHours: " + center.operatingHours;
            if (center.distance > 0) {
                snippet += String.format("\nDistance: %.2f km", center.distance / 1000.0);
            }
            marker.setSnippet(snippet);
            
            marker.setOnMarkerClickListener((m, mv) -> {
                isProgrammaticUpdate = true;
                updateOverview(m.getPosition());
                m.showInfoWindow();
                
                int index = centerList.indexOf(center);
                if (index != -1) {
                    recyclerView.smoothScrollToPosition(index);
                }
                
                recyclerView.postDelayed(() -> isProgrammaticUpdate = false, 1500);
                return true;
            });
            
            mapView.getOverlays().add(marker);
            centerToMarkerMap.put(center, marker);
        }
        mapView.invalidate();
    }

    private void updateMarkerDistances() {
        // 1. Iterate through your centers
        for (RecycleCenter center : centerList) {
            // 2. Get the marker object we previously saved in our HashMap
            Marker marker = centerToMarkerMap.get(center);

            if (marker != null) {
                // 3. Re-build the string with the NEW distance value
                String snippet = center.supportedServices + "\nHours: " + center.operatingHours;
                if (center.distance > 0) {
                    snippet += String.format("\nDistance: %.2f km", center.distance / 1000.0);
                }

                // 4. Update the marker's internal data
                marker.setSnippet(snippet);

                // 5. If the user is currently looking at this marker,
                // refresh the popup so they see the numbers change live.
                if (marker.isInfoWindowShown()) {
                    marker.showInfoWindow();
                }
            }
        }
    }

    private void updateOverview(GeoPoint centerPoint) {
        if (!isAdded()) return;

        if (selectionLine != null) {
            mapView.getOverlays().remove(selectionLine);
            selectionLine = null;
        }

        if (currentUserLocation != null) {
            GeoPoint userPoint = new GeoPoint(currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
            
            selectionLine = new Polyline();
            selectionLine.addPoint(userPoint);
            selectionLine.addPoint(centerPoint);
            selectionLine.getOutlinePaint().setColor(Color.parseColor("#4CAF50"));
            selectionLine.getOutlinePaint().setStrokeWidth(8.0f);
            mapView.getOverlays().add(selectionLine);

            ArrayList<GeoPoint> points = new ArrayList<>();
            points.add(userPoint);
            points.add(centerPoint);
            BoundingBox box = BoundingBox.fromGeoPoints(points);
            
            mapView.zoomToBoundingBox(box, true, 150);
        } else {
            mapController.animateTo(centerPoint);
            mapController.setZoom(18.0);
        }
        
        mapView.invalidate();
    }

    private void sortCenters(Location userLocation) {
        if (centerList.isEmpty() || !isAdded()) return;

        for (RecycleCenter center : centerList) {
            float[] results = new float[1];
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                    center.latitude, center.longitude, results);
            center.distance = results[0];
        }

        Collections.sort(centerList, (c1, c2) -> Float.compare(c1.distance, c2.distance));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}