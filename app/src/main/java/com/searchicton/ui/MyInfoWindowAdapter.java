package com.searchicton.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.searchicton.R;
import com.searchicton.database.Landmark;

/** InfoWindowAdapter wrapper class
 * Used for putting a custom InfoWindow when markers are tapped
 */
class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View myContentsView;
    private final Context context;

    MyInfoWindowAdapter(Context context){
        this.context = context;
        myContentsView = LayoutInflater.from(context).inflate(R.layout.info_window, null);
    }

    /** Sets the content of the InfoWindow */
    @Override
    public View getInfoContents(Marker marker) {
        Landmark landmark = (Landmark) marker.getTag();
        TextView tvTitle = ((TextView)myContentsView.findViewById(R.id.title));
        tvTitle.setText(landmark.getTitle());
        TextView tvDesc = ((TextView)myContentsView.findViewById(R.id.description));
        tvDesc.setText(landmark.getDescription());

        TextView tvPoints = ((TextView)myContentsView.findViewById(R.id.points));
        tvPoints.setText(landmark.getPoints() + "");

        Button tvButton = ((Button)myContentsView.findViewById(R.id.discoverButton));
        tvButton.setOnClickListener(view -> {
            Toast.makeText(context, "discoverButton clicked", Toast.LENGTH_LONG).show();
        });

        return myContentsView;
    }

    @Override
    public View getInfoWindow(Marker marker) {
//            View v = inflater.inflate(R.layout.balloon, null);
//            if (marker != null) {
//                textViewTitle = (TextView) v.findViewById(R.id.textViewTitle);
//                textViewTitle.setText(marker.getTitle());
//            }
//            return (v);
        return null;
    }



}