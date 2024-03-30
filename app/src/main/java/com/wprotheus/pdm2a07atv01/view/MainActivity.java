package com.wprotheus.pdm2a07atv01.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.wprotheus.pdm2a07atv01.R;
import com.wprotheus.pdm2a07atv01.util.PermissionUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 107;
    private GeoPoint marcadorMapa = new GeoPoint(-10.183114920492931, -48.333664189415735);
    private MapView map;
    private Marker pointer;
    private MyLocationNewOverlay myLocationOverlay = null;
    private CompassOverlay mCompassOverlay = null;
    private boolean permissionDenied = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton ibLoc = findViewById(R.id.ibLoc);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        configuraViewMapa();
        checaPermissao();

        ibLoc.setOnTouchListener((v, event) -> {
            apagaMarcadorVelho();
            setMarcadorLocal();
            return true;
        });

        pointer.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) { }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                GeoPoint geoPoint = marker.getPosition();
                geoPoint.setCoords(geoPoint.getLatitude(), geoPoint.getLongitude());
                marker.setTitle("Coordenadas Geográficas: ");
                marker.setSnippet("Latitude: " + geoPoint.getLatitude() +
                        " Longitude: " + geoPoint.getLongitude());
                map.getOverlays().add(marker);
                map.invalidate();
            }

            @Override
            public void onMarkerDragStart(Marker marker) { }
        });
    }

    private void configuraViewMapa() {
        map = findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(15.2);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.setMultiTouchControls(true);
        mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);
        RotationGesture(map);
        marcadorInicial();
    }

    private void checaPermissao() {
        if (checkLocationPermission())
            enableMyLocation();
        else
            requestLocationPermission();
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true);
    }

    private void enableMyLocation() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);
    }

    private void marcadorInicial() {
        Drawable icon = ResourcesCompat.getDrawable(getResources(), R.drawable.pin, null);
        pointer = new Marker(map);
        pointer.setPosition(marcadorMapa);
        pointer.setIcon(icon);
        pointer.setDraggable(true);
        pointer.setTitle("Praça dos Girassóis - Monumento à Bíblia - Centro Geodésico do Brasil.");
        pointer.setSnippet("Latitude: " + marcadorMapa.getLatitude() +
                " Longitude: " + marcadorMapa.getLongitude());
        pointer.showInfoWindow();
        pointer.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(pointer);
        map.getController().setCenter(marcadorMapa);
        map.invalidate();
    }

    private void setMarcadorLocal() {
        GeoPoint myLocation = myLocationOverlay.getMyLocation();
        pointer.setPosition(myLocation);
        pointer.setTitle("Coordenadas Geográficas: ");
        pointer.setSnippet("Latitude: " + myLocation.getLatitude() +
                " Longitude: " + myLocation.getLongitude());
        map.getOverlays().add(pointer);
        map.getController().setCenter(myLocation);
        map.invalidate();
    }

    private void apagaMarcadorVelho() {
        List<Overlay> overlays = map.getOverlays();
        for (Overlay overlay : overlays)
            if (overlay instanceof Marker)
                overlays.remove(overlay);
    }

    private void RotationGesture(MapView map) {
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(map);
        mRotationGestureOverlay.setEnabled(true);
        map.setMultiTouchControls(true);
        map.getOverlays().add(mRotationGestureOverlay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION))
                enableMyLocation();
            else
                permissionDenied = true;
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        map.onResume();
        if (permissionDenied) {
            showMissingError();
            permissionDenied = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    private void showMissingError() {
        PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
}