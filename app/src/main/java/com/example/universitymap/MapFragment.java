package com.example.universitymap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private MapView mapView;
    private TextView infoTextView;

    private final Map<String, List<LatLng>> blockPolygons = new HashMap<String, List<LatLng>>() {{
        put("U1", createPolygon(
                new LatLng(13.1705, 77.5345),
                new LatLng(13.1710, 77.5345),
                new LatLng(13.1710, 77.5352),
                new LatLng(13.1705, 77.5352)
        ));
        put("U2", createPolygon(
                new LatLng(13.1700, 77.5353),
                new LatLng(13.1705, 77.5353),
                new LatLng(13.1705, 77.5360),
                new LatLng(13.1700, 77.5360)
        ));
        put("U3", createPolygon(
                new LatLng(13.1695, 77.5348),
                new LatLng(13.1700, 77.5348),
                new LatLng(13.1700, 77.5355),
                new LatLng(13.1695, 77.5355)
        ));
        put("U4", createPolygon(
                new LatLng(13.1690, 77.5355),
                new LatLng(13.1695, 77.5355),
                new LatLng(13.1695, 77.5362),
                new LatLng(13.1690, 77.5362)
        ));
        put("Library", createPolygon(
                new LatLng(13.1702, 77.5360),
                new LatLng(13.1707, 77.5360),
                new LatLng(13.1707, 77.5365),
                new LatLng(13.1702, 77.5365)
        ));
        put("Admin", createPolygon(
                new LatLng(13.1710, 77.5355),
                new LatLng(13.1715, 77.5355),
                new LatLng(13.1715, 77.5362),
                new LatLng(13.1710, 77.5362)
        ));
    }};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.map);
        infoTextView = view.findViewById(R.id.textInfo);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng defaultLocation = new LatLng(13.169857, 77.535301);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 16));

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);
        drawPolygons();
        addBlockMarkers();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

                        String currentBlock = getCurrentBlock(userLocation);

                        mMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title(currentBlock != null ? "You are in " + currentBlock : "Outside blocks")
                                .snippet("Tap for directions"));

                        infoTextView.setText(currentBlock != null ?
                                "Current Block: " + currentBlock : "Outside campus blocks");
                    }
                });
    }

    private void drawPolygons() {
        for (Map.Entry<String, List<LatLng>> entry : blockPolygons.entrySet()) {
            mMap.addPolygon(new PolygonOptions()
                    .addAll(entry.getValue())
                    .strokeColor(0xFF1A73E8)
                    .strokeWidth(3)
                    .fillColor(0x301A73E8));
        }
    }

    private void addBlockMarkers() {
        for (Map.Entry<String, List<LatLng>> entry : blockPolygons.entrySet()) {
            List<LatLng> points = entry.getValue();
            double latSum = 0, lngSum = 0;
            for (LatLng point : points) {
                latSum += point.latitude;
                lngSum += point.longitude;
            }
            LatLng center = new LatLng(latSum / points.size(), lngSum / points.size());
            mMap.addMarker(new MarkerOptions()
                    .position(center)
                    .title(entry.getKey())
                    .anchor(0.5f, 0.5f));
        }
    }

    private String getCurrentBlock(LatLng userLocation) {
        for (Map.Entry<String, List<LatLng>> entry : blockPolygons.entrySet()) {
            if (isPointInPolygon(userLocation, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isPointInPolygon(LatLng point, List<LatLng> polygon) {
        int intersectCount = 0;
        for (int j = 0; j < polygon.size() - 1; j++) {
            if (rayCastIntersect(point, polygon.get(j), polygon.get(j + 1))) {
                intersectCount++;
            }
        }
        return (intersectCount % 2) == 1;
    }

    private boolean rayCastIntersect(LatLng point, LatLng vertA, LatLng vertB) {
        double ax = vertA.longitude;
        double ay = vertA.latitude;
        double bx = vertB.longitude;
        double by = vertB.latitude;
        double px = point.longitude;
        double py = point.latitude;

        if ((ay > py && by > py) || (ay < py && by < py) || (ax < px && bx < px)) {
            return false;
        }

        double slope = (bx - ax) == 0 ? Double.MAX_VALUE : (by - ay) / (bx - ax);
        double intersectX = slope == Double.MAX_VALUE ? ax : ax + (py - ay) / slope;
        return intersectX >= px;
    }

    private List<LatLng> createPolygon(LatLng... points) {
        List<LatLng> polygon = new ArrayList<>();
        for (LatLng point : points) {
            polygon.add(point);
        }
        polygon.add(points[0]);
        return polygon;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
                drawPolygons();
                addBlockMarkers();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
