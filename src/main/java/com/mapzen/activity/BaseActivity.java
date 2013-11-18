package com.mapzen.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.mapzen.R;
import com.mapzen.entity.Place;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

import static com.mapzen.MapzenApplication.getLocation;
import static com.mapzen.MapzenApplication.getLocationPoint;

public class BaseActivity extends Activity {
    private MapView mapView;
    private MapController mapController;
    private SlidingMenu slidingMenu;
    private StarOverlay stars;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        setupMap();
        setupSlidingMenu();
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar ab = getActionBar();
        ab.setTitle(R.string.application_name);
        ab.setDisplayHomeAsUpEnabled(true);
    }

    private void setupSlidingMenu() {
        slidingMenu = new SlidingMenu(this);
        slidingMenu.setMode(SlidingMenu.LEFT);
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        slidingMenu.setShadowWidthRes(R.dimen.slidingmenu_shadow_width);
        slidingMenu.setShadowDrawable(R.drawable.slidingmenu_shadow);
        slidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        slidingMenu.setFadeDegree(0.35f);
        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        slidingMenu.setMenu(R.layout.slidingmenu);
    }

    @Override
    public void onBackPressed() {
        if ( slidingMenu.isMenuShowing() ) {
            slidingMenu.toggle();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( keyCode == KeyEvent.KEYCODE_MENU ) {
            this.slidingMenu.toggle();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.slidingMenu.toggle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            BoundingBoxE6 boundingBox = mapView.getBoundingBox();
            double[] box = {
                ((double) boundingBox.getLonWestE6()) / 1E6,
                ((double) boundingBox.getLatNorthE6()) / 1E6,
                ((double) boundingBox.getLatSouthE6()) / 1E6,
                ((double) boundingBox.getLonEastE6()) / 1E6
            };
            intent.putExtra("box", box);
        }
        super.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    private void setupMap() {
        Intent intent = getIntent();
        final Bundle bundle = intent.getExtras();
        mapView = (MapView) findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapController = mapView.getController();
        mapController.setZoom(6);
        mapView.setMultiTouchControls(true);

        GeoPoint location = getLocationPoint(this);

        if(bundle != null) {
            Place place = (Place) bundle.getParcelable("place");
            addStar(place.getPoint());
            mapController.setCenter(place.getPoint());
        } else if(location != null) {
            mapController.setCenter(location);
        }
    }

    private void addStar(GeoPoint point) {
        Drawable marker=getResources().getDrawable(android.R.drawable.star_big_on);
        int markerWidth = marker.getIntrinsicWidth();
        int markerHeight = marker.getIntrinsicHeight();
        marker.setBounds(0, markerHeight, markerWidth, 0);

        ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        stars = new StarOverlay(marker, resourceProxy);
        mapView.getOverlays().add(stars);
        stars.addItem(point, "myPoint1", "myPoint1");
    }

    private class StarOverlay extends ItemizedOverlay<OverlayItem> {
        private ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        public StarOverlay(Drawable pDefaultMarker,
                           ResourceProxy pResourceProxy) {
            super(pDefaultMarker, pResourceProxy);
        }

        public void addItem(GeoPoint p, String title, String snippet){
            OverlayItem newItem = new OverlayItem(title, snippet, p);
            items.add(newItem);
            populate();
        }

        @Override
        public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
            return false;
        }

        @Override
        protected OverlayItem createItem(int arg0) {
            return items.get(arg0);
        }

        @Override
        public int size() {
            return items.size();
        }
    }
}