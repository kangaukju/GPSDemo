package kr.kinow.gpsdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    final public String tag = "kinow";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;

    private GoogleMap mGoogleMap;
    private TextView addressText;
    private Button addressSearchButton;
    private TextView latText;
    private TextView lngText;
    private Button latlngSearchButton;
    private CheckBox satelliteCheckBox;
    private LinearLayout addressLayout;
    private LocationManager locationManager;
    protected Context mContext;
    private int DEFAULT_ZOOM_LEVEL = 13;
    private Address wakeupAddress;
    private GEOCoderManager geoCoderManager;
    private GPSManager gps;
    private RepeatViberator repeatViberator;
    private Dialog confirmDlg;
    private ToggleButton alaramButton;
    private LocationListener locationListener;
    private Location wakeupLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mContext = this;
        {
            confirmDlg = null;
            addressText = (TextView) findViewById(R.id.addressText);
            latText = (TextView) findViewById(R.id.latText);
            lngText = (TextView) findViewById(R.id.lngText);
            latlngSearchButton = (Button) findViewById(R.id.latlngSearchButton);
            satelliteCheckBox = (CheckBox) findViewById(R.id.satelliteCheckBox);
            addressSearchButton = (Button) findViewById(R.id.addressSearchButton);
            addressLayout = (LinearLayout) findViewById(R.id.addressLayout);
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            geoCoderManager = new GEOCoderManager(this);
            gps = new GPSManager(mContext);
            repeatViberator = null;
            alaramButton = (ToggleButton) findViewById(R.id.alaramButton);
            locationListener = null;
        }

        // 주소 검색 시
        addressSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!"".equals(addressText.getText().toString())) {
                    addressLayout.removeAllViews();

                    List<Address> addressList =
                            geoCoderManager.searchAddress(addressText.getText().toString(), 2);
                    if (addressList != null && addressList.size() > 0) {
                        for (Address a : addressList) {
                            AddressView av = new AddressView(mContext, a);
                            av.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View vv) {
                                    AddressView a = (AddressView)vv;
                                    geoCoderManager.navigateToLocation(mGoogleMap, a.getAddress());

                                    // 주소 저장
                                    wakeupAddress = a.getAddress();
                                    wakeupLocation = new Location("gps");
                                    wakeupLocation.setLatitude(wakeupAddress.getLatitude());
                                    wakeupLocation.setLongitude(wakeupAddress.getLongitude());
                                }
                            });
                            addressLayout.addView(av);
                        }
                    }
                }
            }
        });

        // 위성 사진 체크 시
        satelliteCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (satelliteCheckBox.isChecked()) {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });

        // 위도,경도 검색 시
        latlngSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setGPSCurrent(latText.getText().toString(), lngText.getText().toString());
            }
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap gmap) {
                mGoogleMap = gmap;
            }
        });

        alaramButton.setText("추적 중지");
        locationListener = enableLocationTrackingAlarm();
        alaramButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alaramButton.isChecked()) {
                    alaramButton.setText("추적 중지");
                    locationListener = enableLocationTrackingAlarm();
                } else {
                    alaramButton.setText("추적 시작");
                    disableLocationTrackingAlarm(locationListener);
                }
            }
        });
    }

    public final int GPS_UPDATE_TIME = 1;
    public final int GPS_UPDATE_DISTANCE = 10;
    private void disableLocationTrackingAlarm(LocationListener locationListener) {
        if (locationListener != null) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(locationListener);
        }
        if (repeatViberator != null) {
            repeatViberator.cancel(true);
            repeatViberator = null;
        }
    }
    // 1분마다 위치 추척하여 설정된 위치 근방이면 알림 발생
    private LocationListener enableLocationTrackingAlarm() {
        LocationListener locationListener = null;
        if (gps.isGetLocation()) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    // 위도, 경도 표시 갱신
                    double curLat = location.getLatitude();
                    double curLng = location.getLongitude();
                    latText.setText(String.valueOf(curLat));
                    lngText.setText(String.valueOf(curLng));

                    // 근접 위치 판단
                    if (wakeupAddress != null &&
                        wakeupLocation != null &&
                        location.distanceTo(wakeupLocation) < 50) {
                        double lat = wakeupAddress.getLatitude();
                        double lng = wakeupAddress.getLongitude();

                        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                        PendingIntent pendingIntent = PendingIntent.getActivity(
                                mContext,
                                0,
                                new Intent(mContext, mContext.getClass()),
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
                        builder.setSmallIcon(R.drawable.address_icon)
                                .setTicker("Approach Address")
                                .setWhen(System.currentTimeMillis())
                                .setContentTitle("Approach Address")
                                .setContentText(wakeupAddress.getAddressLine(0))
                                .setAutoCancel(true)
                                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                                .setContentIntent(pendingIntent);
                        nm.notify(111, builder.build());

                        Log.i(tag, "call change localtion : " + wakeupAddress.getAddressLine(0));

                        if (repeatViberator == null) {
                            repeatViberator = new RepeatViberator(mContext);
                            repeatViberator.execute();
                        }

                        if (confirmDlg == null || confirmDlg.isShowing() == false) {
                            confirmDlg = new AlertDialog.Builder(mContext)
                                    .setIcon(R.drawable.address_icon)
                                    .setTitle("Approach Address")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (repeatViberator != null) {
                                                repeatViberator.cancel(true);
                                                repeatViberator = null;
                                            }
                                        }
                                    })
                                    .setMessage(wakeupAddress.getAddressLine(0))
                                    .create();

                            if (mContext != null) {
                                confirmDlg.show();
                            }
                        }
                    }
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }
                @Override
                public void onProviderEnabled(String provider) {
                }
                @Override
                public void onProviderDisabled(String provider) {
                }
            };

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    GPS_UPDATE_TIME * 1000,
                    GPS_UPDATE_DISTANCE,
                    locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    GPS_UPDATE_TIME * 1000,
                    GPS_UPDATE_DISTANCE,
                    locationListener);

        }
        return locationListener;
    }

    // 위도, 경도에 맞는 위도 위치 이동
    private void setGPSCurrent(String lat, String lng) {
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lng);

        if (gps.isGetLocation()) {
            if ("".equals(lat) || "".equals(lng)) {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
            }
            // Creating a LatLng object for the current location
            LatLng latlng = new LatLng(latitude, longitude);

            // Showing the current location in Google Map
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));

            // Map 을 zoom 합니다.
            this.setZomLevel(DEFAULT_ZOOM_LEVEL);

            // 마커 설정.
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latlng); // 위도 , 경도
            markerOptions.title("Current Position"); // 제목 미리보기
            markerOptions.snippet("snippet");
//            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.fcb));
            mGoogleMap.addMarker(markerOptions).showInfoWindow();
        }
    }

    private void setZomLevel(int level) {
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(level));
        Toast.makeText(this, "Zoom Level : " + String.valueOf(level), Toast.LENGTH_LONG).show();
    }
}
