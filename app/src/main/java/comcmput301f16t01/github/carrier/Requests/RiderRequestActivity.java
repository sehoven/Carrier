package comcmput301f16t01.github.carrier.Requests;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.content.DialogInterface;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import comcmput301f16t01.github.carrier.FareCalculator;
import comcmput301f16t01.github.carrier.R;
import comcmput301f16t01.github.carrier.RiderViewOfferingDriversActivity;
import comcmput301f16t01.github.carrier.Users.UsernameTextView;

import static comcmput301f16t01.github.carrier.Requests.Request.Status.CANCELLED;
import static comcmput301f16t01.github.carrier.Requests.Request.Status.COMPLETE;
import static comcmput301f16t01.github.carrier.Requests.Request.Status.CONFIRMED;
import static comcmput301f16t01.github.carrier.Requests.Request.Status.PAID;

/**
 * <p>RiderRequestActivity displays request information from the rider's perspective. It gives the rider
 * the ability to see their request, cancel it, or accept a driver from it.</p>
 * </br>
 * <p>See code attribution in Wiki: <a href="https://github.com/CMPUT301F16T01/Carrier/wiki/Code-Re-Use#driverviewrequestactivity">DriverViewRequestActivity</a></p>
 * </br>
 * <p>Based on: <a href="https://github.com/MKergall/osmbonuspack/wiki/Tutorial_0">Tutorial_0</a></p>
 * <p>Author: MKergall</p>
 * <p>Retrieved on: November 10th, 2016</p>
 * </br>
 * <p>Updated with: <a href="http://stackoverflow.com/questions/38539637/osmbonuspack-roadmanager-networkonmainthreadexception">OSMBonuspack RoadManager NetworkOnMainThreadException</a></p>
 * <p>Author: <a href="http://stackoverflow.com/users/4670837/yubaraj-poudel">yubaraj poudel</a></p>
 * <p>Posted: August 6th, 2016</p>
 * <p>Retrieved on: November 10th, 2016</p>
 * </br>
 * <p>Based on: <a href="http://stackoverflow.com/questions/20608590/osmdroid-zooming-to-show-the-whole-pathoverlay">OSMDroid: zooming to show the whole PathOverlay</a></p>
 * <p>Author: <a href="http://stackoverflow.com/users/6769091/thebugger">theBugger</a></p>
 * <p>Posted on: September 30th, 2016</p>
 * <p>Retrieved on: November 24th, 2016</p>
 */
public class RiderRequestActivity extends AppCompatActivity {
    private Activity activity = RiderRequestActivity.this;
    private GeoPoint startPoint = null;
    private GeoPoint endPoint = null;
    private Road[] roadList = null;
    private MapView map;
    private IMapController mapController;
    private Request request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_view_request);

        // unpacking the bundle to get the position of request
        Bundle bundle = getIntent().getExtras();

        int position = bundle.getInt("position");
        request = RequestController.getRiderInstance().get(position);

        setTitle("Request");

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        startPoint = new GeoPoint(request.getStart());
        endPoint = new GeoPoint(request.getEnd());

        mapController = map.getController();
        mapController.setCenter(getCenter());
        zoomToBounds(getBoundingBox(startPoint, endPoint));

        ArrayList<OverlayItem> overlayItems = new ArrayList<>();
        overlayItems.add(new OverlayItem("Starting Point", "This is the starting point", startPoint));
        overlayItems.add(new OverlayItem("Destination", "This is the destination point", endPoint));

        setViews();
        setMarkers();
        getRoadAsync();
    }

    @Override
    public void onResume() {
        super.onResume();
        setViews();
    }
    /**
     * This function finds a BoundingBox that fits both the start and end location points.
     *
     * @param start GeoPoint for start location
     * @param end GeoPoint for end location
     * @return BoundingBox that holds both location points
     */
    private BoundingBox getBoundingBox(GeoPoint start, GeoPoint end) {
        double north;
        double south;
        double east;
        double west;
        if(start.getLatitude() > end.getLatitude()) {
            north = start.getLatitude();
            south = end.getLatitude();
        } else {
            north = end.getLatitude();
            south = start.getLatitude();
        }
        if(start.getLongitude() > end.getLongitude()) {
            east = start.getLongitude();
            west = end.getLongitude();
        } else {
            east = end.getLongitude();
            west = start.getLongitude();
        }
        return new BoundingBox(north, east, south, west);
    }

    /**
     * This function allows the MapView to zoom to show the whole path between
     * the start and end points.
     *
     * @param box BoundingBox for start and end points
     */
    // see code attribution
    private void zoomToBounds(final BoundingBox box) {
        if (map.getHeight() > 0) {
            map.zoomToBoundingBox(box, false);
            map.zoomToBoundingBox(box, false);
        } else {
            ViewTreeObserver vto = map.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    map.zoomToBoundingBox(box, false);
                    map.zoomToBoundingBox(box, false);
                    ViewTreeObserver vto2 = map.getViewTreeObserver();
                    vto2.removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    /**
     * Given the request passed in by the user, set the map according to the start and end locations
     */
    private void setMarkers() {
        Marker startMarker = new Marker(map);
        startMarker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_start_marker, null));
        Marker endMarker = new Marker(map);
        endMarker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_end_marker, null));

        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("START:\n" + request.getStart().getAddress());
        startMarker.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, map));
        endMarker.setPosition(endPoint);
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setTitle("END:\n" + request.getEnd().getAddress());
        endMarker.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, map));

        map.getOverlays().add(startMarker);
        map.getOverlays().add(endMarker);
        map.invalidate();
    }

    /**
     * Asynchronous task to get the route between the two points
     */
    // see code attribution
    private void getRoadAsync() {
        roadList = null;

        GeoPoint roadStartPoint = startPoint;
        GeoPoint roadEndPoint = endPoint;
        if (startPoint != null) {
            roadStartPoint = startPoint;
        }
        if (endPoint != null) {
            roadEndPoint = endPoint;
        }
        ArrayList<GeoPoint> waypoints = new ArrayList<>(2);
        waypoints.add(roadStartPoint);
        waypoints.add(roadEndPoint);
        new UpdateRoadTask().execute(waypoints);
    }

    /**
     * Class to update the road on the map, Async to prevent locking up UI thread.
     */
    // see code attribution
    private class UpdateRoadTask extends AsyncTask<Object, Void, Road[]> {

        @Override
        protected Road[] doInBackground(Object... params) {
            @SuppressWarnings("unchecked")
            ArrayList<GeoPoint> waypoints = (ArrayList<GeoPoint>) params[0];
            RoadManager roadManager = new OSRMRoadManager(activity);
            return roadManager.getRoads(waypoints);
        }

        @Override
        protected void onPostExecute(Road[] roads) {
            double minLength = 0;
            Road bestRoad = null;
            roadList = roads;
            if (roads == null)
                return;
            if (roads[0].mStatus == Road.STATUS_TECHNICAL_ISSUE) {
                Toast.makeText(activity, "Technical issue when getting the route", Toast.LENGTH_SHORT).show();
            } else if (roads[0].mStatus > Road.STATUS_TECHNICAL_ISSUE) { //functional issues
                Toast.makeText(activity, "No possible route here", Toast.LENGTH_SHORT).show();
            }
            List<Overlay> mapOverlays = map.getOverlays();
            for (Road road : roads) {
                if (road.mLength < minLength || minLength == 0) {
                    minLength = road.mLength;
                    bestRoad = road;
                }
            }

            String routeDesc = bestRoad.getLengthDurationText(activity, -1);
            Polyline roadPolyline = RoadManager.buildRoadOverlay(bestRoad);
            roadPolyline.setTitle(getString(R.string.app_name) + " - " + routeDesc);
            roadPolyline.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, map));
            mapOverlays.add(0, roadPolyline);
            map.invalidate();
        }
    }

    /** Centers the map on the start of the route */
    public void centerStart(View view) {
        mapController.setCenter(startPoint);
    }

    /** Centers the map on the end of the map */
    public void centerEnd(View view) {
        mapController.setCenter(endPoint);
    }

    /**
     * Get the center point of the route to center the screen on
     *
     * @return GeoPoint
     */
    private GeoPoint getCenter() {
        double startLat = startPoint.getLatitude();
        double startLong = startPoint.getLongitude();
        double endLat = endPoint.getLatitude();
        double endLong = endPoint.getLongitude();

        Location retLoc = new Location("");

        if (startLat > endLat) {
            retLoc.setLatitude(endLat + ((startLat - endLat) / 2));
        } else {
            retLoc.setLatitude(startLat + ((endLat - startLat) / 2));
        }

        if (startLong > endLong) {
            retLoc.setLongitude(endLong + ((startLong - endLong) / 2));
        } else {
            retLoc.setLongitude(startLong + ((endLong - startLong) / 2));
        }

        return new GeoPoint(retLoc);
    }

    /**
     * Will confirm the completion of a request.
     * @param view The view for the button that was clicked.
     */
    public void completeRequest(View view) {
        if (request.getStatus() == CONFIRMED) {
            RequestController.completeRequest(request);
            ImageView statusImageView = (ImageView) findViewById(R.id.imageView_requestStatus);
            statusImageView.setImageResource(R.drawable.complete);
            Button cancelButton = (Button) findViewById(R.id.button_delete);
            cancelButton.setAlpha(0.5f);
            cancelButton.setEnabled(false);
            view.setEnabled(false);
            view.setAlpha(0.5f);
            RequestController.getOffersInstance().notifyListeners();
            RequestController.getRiderInstance().notifyListeners();
        }
    }

    /**
     * Given the request passed in by the user, set the views in the layout.
     */
    private void setViews() {
        // Set up the fare
        Currency localCurrency = Currency.getInstance( Locale.getDefault() );
        String price = localCurrency.getSymbol() + FareCalculator.toString(request.getFare());
        TextView fareTextView = (TextView) findViewById(R.id.textView_$fareAmount);
        fareTextView.setText(price);

        // TODO setText could be inside the setUser method?
        // Set up the UsernameTextView of the rider
        UsernameTextView riderUsernameTextView = (UsernameTextView) findViewById(R.id.UsernameTextView_rider);
        riderUsernameTextView.setText(request.getRider().getUsername());
        riderUsernameTextView.setUser(request.getRider());

        // Set up the UsernameTextView of the driver
        UsernameTextView driverUsernameTextView = (UsernameTextView) findViewById(R.id.UsernameTextView_driver);
        // If no driver has been selected we need to display the list of drivers who have made an offer.
        TextView driverTextView = (TextView) findViewById(R.id.textView_driver);
        driverTextView.setText(R.string.DriverHere);
        // Remove the listener and only set it if there are offering drivers
        driverTextView.setOnClickListener(null);
        if (request.getChosenDriver() != null) {
            driverUsernameTextView.setText(request.getChosenDriver().getUsername());
            driverUsernameTextView.setUser(request.getChosenDriver());
        } else if (request.getOfferedDrivers().size() > 0 && (request.getStatus() != CONFIRMED || request.getStatus() != PAID)) {
            // If there is an offered driver
            driverTextView = (TextView) findViewById(R.id.textView_driver);
            driverTextView.setText(R.string.View_Offering_Drivers);
            driverTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = getIntent().getExtras();
                    Intent intent = new Intent(RiderRequestActivity.this, RiderViewOfferingDriversActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });

        }

        TextView startAddressTextView = (TextView) findViewById(R.id.textView_start);
        startAddressTextView.setText(request.getStart().toString());

        TextView endAddressTextView = (TextView) findViewById(R.id.textView_end);
        endAddressTextView.setText(request.getEnd().toString());

        TextView descriptionTextView = (TextView) findViewById(R.id.textView_description);
        descriptionTextView.setText(request.getDescription());

        /**
         * This switch statement changes the status image
         */
        ImageView statusImageView = (ImageView) findViewById(R.id.imageView_requestStatus);
        if (statusImageView != null) {
            switch (request.getStatus()) {
                case OPEN:
                    statusImageView.setImageResource(R.drawable.open);
                    break;
                case OFFERED:
                    statusImageView.setImageResource(R.drawable.offered);
                    break;
                case CONFIRMED:
                    statusImageView.setImageResource(R.drawable.confirmed);
                    break;
                case COMPLETE:
                    statusImageView.setImageResource(R.drawable.complete);
                    break;
                case PAID:
                    statusImageView.setImageResource(R.drawable.paid);
                    break;
                case CANCELLED:
                    statusImageView.setImageResource(R.drawable.cancel);
                    break;

            }
        }
        // Make the button grey if it is completed or paid.
        if (request.getStatus() != CONFIRMED) {
            Button completeButton = (Button) findViewById(R.id.button_confirm_completion);
            completeButton.setAlpha(0.5f);
            completeButton.setEnabled(false);
        }
        else {
            Button completeButton = (Button) findViewById(R.id.button_confirm_completion);
            completeButton.setAlpha(1f);
            completeButton.setEnabled(true);
        }
        if (request.getStatus() == COMPLETE || request.getStatus() == PAID || request.getStatus() == CANCELLED){
            Button cancelButton = (Button) findViewById(R.id.button_delete);
            cancelButton.setEnabled(false);
            cancelButton.setAlpha(0.5f);
        }
    }

    /**
     * Will initialize the view ids for all the views in the activity.
     * @param v the Cancel Request button.
     */
    public void cancelRequest(final View v) {
        AlertDialog.Builder adb = new AlertDialog.Builder(RiderRequestActivity.this);
        if ((request.getStatus() != Request.Status.CANCELLED)
                && (request.getStatus() != COMPLETE)
                && (request.getStatus() != PAID)) {
            adb.setMessage("Cancel request?");
            adb.setCancelable(true);
            adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RequestController.cancelRequest(request);
                    v.setAlpha(0.5f);
                    v.setEnabled(false);
                    RequestController.getOffersInstance().notifyListeners();
                    RequestController.getRiderInstance().notifyListeners();
                    finish();
                }
            });

            adb.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
        } else {
            adb.setTitle("Error: ");
            adb.setMessage("Request cannot be cancelled.");
            adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
        }
        adb.show();
    }
}
