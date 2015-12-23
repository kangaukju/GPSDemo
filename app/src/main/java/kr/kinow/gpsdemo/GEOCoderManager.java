package kr.kinow.gpsdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

/**
 * Created by kinow on 2015-12-23.
 */
public class GEOCoderManager {

    private Context mContext;

    GEOCoderManager(Context context) {
        mContext = context;
    }

    private void showInvalidAddressMsg() {
        Dialog errorDlg = new AlertDialog.Builder(mContext)
                .setIcon(0)
                .setTitle("Error")
                .setPositiveButton("OK", null)
                .setMessage("Sorry, your address doesn't exist.")
                .create();
        errorDlg.show();
    }
    private void showListOfFoundAddresses(List<Address> foundAddresses) {
        String msg = "";
        for (Address a : foundAddresses) {
            msg = "lat:"+a.getLatitude()+", lng:"+a.getLongitude()+" - "+a.getAddressLine(0) + "\n";
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
        }
    }

    List<Address> searchAddress(String addres, int maxAddressCount) {
        Geocoder gc = new Geocoder(mContext);
        List<Address> addressList = null;

        try {
            addressList = gc.getFromLocationName(addres, maxAddressCount);

            if (addressList == null || addressList.size() == 0) {
                showInvalidAddressMsg();
            } else {
//                showListOfFoundAddresses(addressList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return addressList;
    }

    public void navigateToLocation(GoogleMap googleMap, Address address) {
        double latitude = address.getLatitude();
        double longitude = address.getLongitude();

        LatLng latlng = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));

        googleMap.animateCamera(CameraUpdateFactory.zoomTo(100));

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng); // 위도 , 경도
//        markerOptions.title("Current Position"); // 제목 미리보기
        markerOptions.snippet(address.getAddressLine(0));
        googleMap.addMarker(markerOptions).showInfoWindow();
    }
}
