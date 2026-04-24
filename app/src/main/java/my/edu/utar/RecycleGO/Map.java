package my.edu.utar.RecycleGO;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import my.edu.utar.RecycleGO.database.FirestoreManager;
import my.edu.utar.RecycleGO.database.RecycleCenter;
import my.edu.utar.RecycleGO.database.RecycleRequest;
import my.edu.utar.RecycleGO.database.UserRecord;

public class Map extends Fragment {

    private MapView mapView;
    private IMapController mapController;
    private FusedLocationProviderClient fusedLocationClient;
    private List<RecycleCenter> centerList = new ArrayList<>();
    private List<RecycleCenter> fullCenterList = new ArrayList<>(); 
    private MyLocationNewOverlay locationOverlay;
    private RecyclerView recyclerView;
    private AdapterRecycleCenter adapter;
    private Location currentUserLocation;
    private java.util.Map<RecycleCenter, Marker> centerToMarkerMap = new java.util.HashMap<>();
    private Polyline selectionLine;
    
    private boolean isMapMovingProgrammatically = false;
    private boolean isCarouselScrollingProgrammatically = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String flow = "NORMAL";
    private String userRole = "User"; 
    private RecycleRequest draftRequest;
    private FirestoreManager firestoreManager;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnAddCenter;
    private android.widget.ImageButton btnSearchAdd;
    private android.widget.EditText etSearch;

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

    private boolean isMapCenteredOn(GeoPoint target) {
        IGeoPoint current = mapView.getMapCenter();
        double threshold = 0.0001; 
        return Math.abs(current.getLatitude() - target.getLatitude()) < threshold &&
                Math.abs(current.getLongitude() - target.getLongitude()) < threshold;
    }

    public Map() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
        
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        Configuration.getInstance().load(ctx, prefs);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        firestoreManager = new FirestoreManager();

        SharedPreferences prefs_role = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userRole = prefs_role.getString("loggedInRole", "User");

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
            ((FrameActivity) getActivity()).setHeaderVisible(true);
        }

        View view = inflater.inflate(R.layout.activity_map, container, false);
        mapView = view.findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);
        mapController.setCenter(new GeoPoint(3.1390, 101.6869));

        etSearch = view.findViewById(R.id.et_map_search);
        btnSearchAdd = view.findViewById(R.id.map_btnSearchAdd);
        btnAddCenter = view.findViewById(R.id.map_btnAddCenter);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCenters(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Allow adding centers if Admin OR if in Sign Up flow
        boolean allowAdd = userRole.equalsIgnoreCase("Admin") || "SIGNUP_TO_MAP".equals(flow);

        if (allowAdd) {
            btnAddCenter.setVisibility(View.VISIBLE);
            
            View.OnClickListener addListener = v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AddCenterFragment())
                        .addToBackStack("ADD_CENTER")
                        .commit();
            };
            btnAddCenter.setOnClickListener(addListener);
            btnSearchAdd.setOnClickListener(addListener);
        }

        recyclerView = view.findViewById(R.id.recycler_horizontal_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        
        final PagerSnapHelper snapHelper = new PagerSnapHelper() {
            @Override
            public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
                View centerView = findSnapView(layoutManager);
                if (centerView == null) return RecyclerView.NO_POSITION;
                int position = layoutManager.getPosition(centerView);
                int targetPosition = super.findTargetSnapPosition(layoutManager, velocityX, velocityY);
                if (targetPosition > position) return position + 1;
                else if (targetPosition < position) return position - 1;
                return position;
            }
        };
        snapHelper.attachToRecyclerView(recyclerView);

        setupAdapter();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isCarouselScrollingProgrammatically) {
                    View centerView = snapHelper.findSnapView(layoutManager);
                    if (centerView != null) {
                        int pos = layoutManager.getPosition(centerView);
                        if (pos != RecyclerView.NO_POSITION && pos < centerList.size()) {
                            RecycleCenter center = centerList.get(pos);
                            GeoPoint target = new GeoPoint(center.latitude, center.longitude);
                            if (!isMapCenteredOn(target)) {
                                isMapMovingProgrammatically = true;
                                updateOverview(target);
                                Marker m = centerToMarkerMap.get(center);
                                if (m != null) m.showInfoWindow();
                                mainHandler.postDelayed(() -> isMapMovingProgrammatically = false, 2500);
                            }
                        }
                    }
                }
            }
        });

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (!isMapMovingProgrammatically && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                    syncCarouselToMapCenter(layoutManager, snapHelper);
                }
                return false;
            }
            @Override
            public boolean onZoom(ZoomEvent event) {
                if (!isMapMovingProgrammatically && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                    syncCarouselToMapCenter(layoutManager, snapHelper);
                }
                return false;
            }
        });

        checkPermissionsAndStart();
        loadAndPopulateData();
        return view;
    }

    private void filterCenters(String query) {
        centerList.clear();
        if (query.isEmpty()) {
            centerList.addAll(fullCenterList);
            btnSearchAdd.setVisibility(View.GONE);
        } else {
            for (RecycleCenter center : fullCenterList) {
                if (center.name.toLowerCase().contains(query.toLowerCase()) || 
                    (center.supportedServices != null && center.supportedServices.toLowerCase().contains(query.toLowerCase()))) {
                    centerList.add(center);
                }
            }
            
            boolean allowAdd = userRole.equalsIgnoreCase("Admin") || "SIGNUP_TO_MAP".equals(flow);
            if (centerList.isEmpty() && allowAdd) {
                btnSearchAdd.setVisibility(View.VISIBLE);
            } else {
                btnSearchAdd.setVisibility(View.GONE);
            }
        }
        adapter.notifyDataSetChanged();
        addMarkers();
    }

    private void syncCarouselToMapCenter(LinearLayoutManager layoutManager, PagerSnapHelper snapHelper) {
        if (centerList.isEmpty() || recyclerView == null || 
            recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) return;

        IGeoPoint mapCenter = mapView.getMapCenter();
        double minDistanceSq = Double.MAX_VALUE;
        int closestIndex = -1;

        for (int i = 0; i < centerList.size(); i++) {
            RecycleCenter center = centerList.get(i);
            double distSq = Math.pow(center.latitude - mapCenter.getLatitude(), 2) + 
                            Math.pow(center.longitude - mapCenter.getLongitude(), 2);
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closestIndex = i;
            }
        }

        if (closestIndex != -1 && !isMapMovingProgrammatically) {
            View snapView = snapHelper.findSnapView(layoutManager);
            int currentPos = (snapView != null) ? layoutManager.getPosition(snapView) : -1;
            if (currentPos != closestIndex) {
                isCarouselScrollingProgrammatically = true;
                recyclerView.smoothScrollToPosition(closestIndex);
                mainHandler.postDelayed(() -> isCarouselScrollingProgrammatically = false, 800);
            }
        }
    }

    private void setupAdapter() {
        adapter = new AdapterRecycleCenter(centerList, new AdapterRecycleCenter.OnItemClickListener() {
            @Override
            public void onItemClick(RecycleCenter center) {
                isMapMovingProgrammatically = true;
                updateOverview(new GeoPoint(center.latitude, center.longitude));
                isCarouselScrollingProgrammatically = true;
                recyclerView.smoothScrollToPosition(centerList.indexOf(center));
                mainHandler.postDelayed(() -> {
                    isMapMovingProgrammatically = false;
                    isCarouselScrollingProgrammatically = false;
                }, 2500);
            }
            @Override
            public void onRecycleClick(RecycleCenter center) {
                if (userRole.equalsIgnoreCase("Admin")) handleJoin(center);
                else handleSelection(center);
            }
        });
        adapter.setIsAdmin(userRole.equalsIgnoreCase("Admin") || "SIGNUP_TO_MAP".equals(flow));
        recyclerView.setAdapter(adapter);
    }

    private void handleJoin(RecycleCenter center) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("loggedInUid", "");
        if (uid.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreManager.joinRecycleCenter(uid, center.id, new FirestoreManager.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Joined " + center.name, Toast.LENGTH_SHORT).show();
                // Refresh data to update "Join" to "Joined"
                loadAndPopulateData();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Join failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSelection(RecycleCenter center) {
        if ("STATUS_TO_FORM".equals(flow) && draftRequest != null) {
            draftRequest.setCenterId(center.id);
            draftRequest.setCenterName(center.name);
            firestoreManager.submitRequest(draftRequest, new FirestoreManager.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Request Submitted!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new RecycleStatus()).commit();
                }
                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else if ("SIGNUP_TO_MAP".equals(flow)) {
            Intent intent = new Intent();
            intent.putExtra("selectedCenter", center.name);
            intent.putExtra("selectedCenterId", center.id);
            requireActivity().setResult(android.app.Activity.RESULT_OK, intent);
            requireActivity().finish();
        } else {
            Fragment f = new PickUpActivity();
            Bundle args = new Bundle();
            args.putString("flow", "MAP_TO_FORM");
            args.putString("centerId", center.id);
            args.putString("centerName", center.name);
            f.setArguments(args);
            getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, f).addToBackStack(null).commit();
        }
    }

    private void checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking();
        } else {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void startLocationTracking() {
        if (!isAdded()) return;
        GpsMyLocationProvider p = new GpsMyLocationProvider(requireContext());
        locationOverlay = new MyLocationNewOverlay(p, mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.runOnFirstFix(() -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    GeoPoint myLocation = locationOverlay.getMyLocation();
                    if (myLocation != null) {
                        Location loc = new Location("");
                        loc.setLatitude(myLocation.getLatitude());
                        loc.setLongitude(myLocation.getLongitude());
                        if (currentUserLocation == null) {
                            currentUserLocation = loc;
                            sortCenters(loc);
                            mapController.animateTo(myLocation);
                        }
                    }
                });
            }
        });
        mapView.getOverlays().add(locationOverlay);
    }

    private void loadAndPopulateData() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("loggedInUid", "");
        
        if (!uid.isEmpty()) {
            firestoreManager.getUser(uid, new FirestoreManager.OnUserFetchListener() {
                @Override
                public void onUserFetched(UserRecord user) {
                    if (user != null && adapter != null) {
                        adapter.setJoinedCenters(user.getJoinedCenters());
                    }
                }
                @Override
                public void onFailure(String error) {}
            });
        }

        firestoreManager.getCentersCollection().get().addOnSuccessListener(queryDocumentSnapshots -> {
            fullCenterList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                RecycleCenter c = doc.toObject(RecycleCenter.class);
                if (c.id == null) c.id = doc.getId(); // Ensure ID is present
                fullCenterList.add(c);
            }
            centerList.clear();
            centerList.addAll(fullCenterList);
            if (currentUserLocation != null) sortCenters(currentUserLocation);
            adapter.notifyDataSetChanged();
            addMarkers();
        });
    }

    private void addMarkers() {
        if (!isAdded()) return;
        List<Overlay> overlays = mapView.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            if (overlays.get(i) instanceof Marker) overlays.remove(i);
        }
        centerToMarkerMap.clear();

        for (RecycleCenter center : centerList) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(center.latitude, center.longitude));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(center.name);
            marker.setInfoWindow(new CustomInfoWindow(R.layout.layout_map_bubble, mapView, center));
            marker.setOnMarkerClickListener((m, mv) -> {
                isMapMovingProgrammatically = true;
                updateOverview(m.getPosition());
                m.showInfoWindow();
                isCarouselScrollingProgrammatically = true;
                recyclerView.smoothScrollToPosition(centerList.indexOf(center));
                mainHandler.postDelayed(() -> {
                    isMapMovingProgrammatically = false;
                    isCarouselScrollingProgrammatically = false;
                }, 2500);
                return true;
            });
            mapView.getOverlays().add(marker);
            centerToMarkerMap.put(center, marker);
        }
        mapView.invalidate();
    }

    private class CustomInfoWindow extends InfoWindow {
        private RecycleCenter center;
        public CustomInfoWindow(int layoutResId, MapView mapView, RecycleCenter center) {
            super(layoutResId, mapView);
            this.center = center;
        }
        @Override
        public void onOpen(Object item) {
            View v = getView();
            ImageView img = v.findViewById(R.id.bubble_image);
            TextView title = v.findViewById(R.id.bubble_title);
            TextView desc = v.findViewById(R.id.bubble_description);
            TextView subDesc = v.findViewById(R.id.bubble_subdescription);
            title.setText(center.name);
            desc.setText(center.supportedServices);
            String snippet = "Hours: " + center.operatingHours;
            if (center.distance > 0) snippet += String.format("\nDistance: %.2f km", center.distance / 1000.0);
            subDesc.setText(snippet);
            if (center.pictureUrl != null && !center.pictureUrl.isEmpty()) {
                img.setVisibility(View.VISIBLE);
                Glide.with(img.getContext()).load(center.pictureUrl).placeholder(R.drawable.community).into(img);
            } else img.setVisibility(View.GONE);
            v.setOnClickListener(view -> close());
        }
        @Override
        public void onClose() {}
    }

    private void updateOverview(GeoPoint centerPoint) {
        if (!isAdded()) return;
        if (selectionLine != null) {
            mapView.getOverlays().remove(selectionLine);
            selectionLine = null;
        }
        if (currentUserLocation != null) {
            selectionLine = new Polyline();
            selectionLine.addPoint(new GeoPoint(currentUserLocation.getLatitude(), currentUserLocation.getLongitude()));
            selectionLine.addPoint(centerPoint);
            selectionLine.getOutlinePaint().setColor(Color.parseColor("#4CAF50"));
            selectionLine.getOutlinePaint().setStrokeWidth(8.0f);
            mapView.getOverlays().add(selectionLine);
        }
        mapController.animateTo(centerPoint);
        mapController.setZoom(16.0); 
        mapView.invalidate();
    }

    private void sortCenters(Location userLocation) {
        if (fullCenterList.isEmpty() || !isAdded()) return;
        for (RecycleCenter c : fullCenterList) {
            float[] res = new float[1];
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), c.latitude, c.longitude, res);
            c.distance = res[0];
        }
        Collections.sort(fullCenterList, (c1, c2) -> Float.compare(c1.distance, c2.distance));
        filterCenters(etSearch.getText().toString());
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
        mainHandler.removeCallbacksAndMessages(null);
    }
}
