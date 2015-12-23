package kr.kinow.gpsdemo;

import android.content.Context;
import android.location.Address;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by kinow on 2015-12-23.
 */
public class AddressView extends Button {
    private Address mAddress;

    public AddressView(Context context, Address address) {
        super(context);
        mAddress = address;
        setText(address.getAddressLine(0));
    }

    public Address getAddress() {
        return mAddress;
    }
}
