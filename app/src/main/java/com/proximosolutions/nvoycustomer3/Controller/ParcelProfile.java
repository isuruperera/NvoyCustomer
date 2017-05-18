package com.proximosolutions.nvoycustomer3.Controller;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.proximosolutions.nvoycustomer3.MainLogic.Courier;
import com.proximosolutions.nvoycustomer3.MainLogic.Customer;
import com.proximosolutions.nvoycustomer3.MainLogic.Parcel;
import com.proximosolutions.nvoycustomer3.R;


public class ParcelProfile extends AppCompatActivity {

    private Button removeParcelBtn;
    private Button trackParcelBtn;
    private Button viewLocationBtn;
    private Button viewTransactionsBtn;
    private String currentUserType;
    private String customerState;
    private Parcel currentParcel;
    private Customer sender;
    private Customer receiver;
    private Courier courier;
    private boolean isSenderFetched = false;
    private boolean isReceiverFetched = false;
    private boolean isCourierFetched = false;
    private String currentParcelKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);;
        setContentView(R.layout.activity_parcel_profile);
        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            if(extras != null){
                currentParcel = (Parcel)extras.get("parcel");
            }
        }else{
            currentParcel = (Parcel)savedInstanceState.getSerializable("parcel");
        }
        currentParcelKey = currentParcel.getParcelID();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = firebaseDatabase.getReference();

        if(currentParcel.getStatus() == Parcel.MARKED_DELIVERED && currentParcel.getReceiverID().equals(EncodeString(FirebaseAuth.getInstance().getCurrentUser().getEmail())) ){
            new AlertDialog.Builder(ParcelProfile.this)
                    .setTitle("Parcel has marked as delivered")
                    .setMessage("Did you receive the parcel?" +
                            " ID:"+currentParcelKey+"")
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            changeParcelStatus(Parcel.DELIVERED);
                            currentParcel.setStatus(Parcel.DELIVERED);
                            if(isCourierFetched && isReceiverFetched && isSenderFetched){
                                updateView();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            changeParcelStatus(Parcel.IN_TRANSIT);
                            currentParcel.setStatus(Parcel.IN_TRANSIT);
                            if(isCourierFetched && isReceiverFetched && isSenderFetched){
                                updateView();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }



        databaseReference.child("Customers").child(currentParcel.getReceiverID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                receiver = (Customer)dataSnapshot.getValue(Customer.class);
                isReceiverFetched = true;
                if(isSenderFetched && isCourierFetched){
                    updateView();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        databaseReference.child("Customers").child(currentParcel.getSenderID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                sender = (Customer)dataSnapshot.getValue(Customer.class);
                isSenderFetched = true;
                if(isCourierFetched && isReceiverFetched){
                    updateView();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        databaseReference.child("Couriers").child(currentParcel.getCarrierID()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                courier = dataSnapshot.getValue(Courier.class);
                isCourierFetched = true;
                if(isSenderFetched && isReceiverFetched){
                    updateView();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        removeParcelBtn = (Button) findViewById(R.id.btn_remove_parcel);
        removeParcelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentParcel.getStatus()==Parcel.NEW || currentParcel.getStatus()==Parcel.CANCELLED ){
                    databaseReference.child("Parcels").child(currentParcel.getParcelID()).child("status").setValue(Parcel.CANCELLED);
                }else{
                    Toast.makeText(ParcelProfile.this, "Please Contact Courier to cancel the parcel",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        trackParcelBtn = (Button) findViewById(R.id.btn_track_parcel);
        trackParcelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri;
                if(currentParcel.getStatus()==Parcel.IN_TRANSIT){

                    StringBuilder uriString = new StringBuilder("google.navigation:q=");
                    uriString.append(currentParcel.getCurrentLocation().getLatitude());
                    uriString.append(",");
                    uriString.append(currentParcel.getCurrentLocation().getLongitude());
                    uriString.append("&avoid=tf");

                    gmmIntentUri = Uri.parse(uriString.toString());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }else{
                    Toast t = new Toast(getBaseContext());
                    t.makeText(getBaseContext(), "Only available in parcels in transit!",
                            Toast.LENGTH_SHORT).show();
                }



            }
        });

        viewLocationBtn = (Button) findViewById(R.id.btn_view_location);
        viewLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentParcel.getStatus()==Parcel.IN_TRANSIT) {
                    StringBuilder uriString = new StringBuilder("google.streetview:cbll=");
                    uriString.append(currentParcel.getCurrentLocation().getLatitude());
                    uriString.append(",");
                    uriString.append(currentParcel.getCurrentLocation().getLongitude());

                    Uri gmmIntentUri = Uri.parse(uriString.toString());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }else{
                    Toast t = new Toast(getBaseContext());
                    t.makeText(getBaseContext(), "Only available in parcels in transit!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void changeParcelStatus(int status){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dataReference = database.getReference();

        dataReference.child("Parcels").child(currentParcelKey).child("status").setValue(status);

    }

    private void updateView(){
        Button removeBtn = (Button)findViewById(R.id.btn_remove_parcel);
        Button trackingBtn = (Button)findViewById(R.id.btn_track_parcel);
        Button streetViewBtn = (Button)findViewById(R.id.btn_view_location);
        ((android.support.design.widget.CollapsingToolbarLayout)findViewById(R.id.user_profile_toolbar)).setTitle(currentParcel.getParcelID());
        ((TextView)findViewById(R.id.text_parcel_contact_courier)).setText(courier.getContactNumber());
        ((TextView)findViewById(R.id.text_parcel_contact_courier)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = courier.getContactNumber();
                Uri call = Uri.parse("tel:" + number);
                Intent surf = new Intent(Intent.ACTION_DIAL, call);
                startActivity(surf);
            }
        });
        ((TextView)findViewById(R.id.text_parcel_contact_receiver)).setText(receiver.getContactNumber());
        ((TextView)findViewById(R.id.text_parcel_contact_receiver)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = receiver.getContactNumber();
                Uri call = Uri.parse("tel:" + number);
                Intent surf = new Intent(Intent.ACTION_DIAL, call);
                startActivity(surf);
            }
        });
        ((TextView)findViewById(R.id.text_parcel_receiver)).setText(receiver.getFirstName()+" "+receiver.getLastName());
        ((TextView)findViewById(R.id.text_parcel_courier)).setText(courier.getFirstName()+" "+courier.getLastName());
        ((TextView)findViewById(R.id.text_parcel_delivery_fair)).setText(String.valueOf(currentParcel.getDeliveryFair())+" LKR");
        switch(currentParcel.getStatus()){
            case Parcel.NEW:
                ((TextView)findViewById(R.id.text_parcel_status)).setText("New Parcel");
                break;
            case Parcel.ACCEPTED:
                ((TextView)findViewById(R.id.text_parcel_status)).setText("Waiting for courier");
;
                break;
            case Parcel.CANCELLED:
                ((TextView)findViewById(R.id.text_parcel_status)).setText("Cancelled Parcel");
                removeBtn.setEnabled(false);
                removeBtn.setText("");
                break;
            case Parcel.DELIVERED:
                ((TextView)findViewById(R.id.text_parcel_status)).setText("Successfully Delivered");
                removeBtn.setEnabled(false);
                removeBtn.setText("");
                break;
            case Parcel.IN_TRANSIT:
                ((TextView)findViewById(R.id.text_parcel_status)).setText("Parcel In Transit");
                removeBtn.setEnabled(false);
                removeBtn.setText("");
                break;
            case Parcel.MARKED_DELIVERED:
                ((TextView)findViewById(R.id.text_parcel_status)).setText("Marked Delivered");
                removeBtn.setEnabled(false);
                removeBtn.setText("");
                break;
        }

    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {

        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                //lockIntent = false;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        //System.out.println(getBaseContext().find);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //lockIntent = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        //lockIntent = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        //lockIntent = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //lockIntent = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //lockIntent = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //lockIntent = false;
    }

    public static String EncodeString(String string) {
        return string.replace(".", ",");
    }

    public static String DecodeString(String string) {
        return string.replace(",", ".");
    }
}
