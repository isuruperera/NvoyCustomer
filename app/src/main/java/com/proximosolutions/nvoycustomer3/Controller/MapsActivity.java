package com.proximosolutions.nvoycustomer3.Controller;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.proximosolutions.nvoycustomer3.MainLogic.ConfigInfo;
import com.proximosolutions.nvoycustomer3.MainLogic.Courier;
import com.proximosolutions.nvoycustomer3.MainLogic.Customer;
import com.proximosolutions.nvoycustomer3.MainLogic.Parcel;
import com.proximosolutions.nvoycustomer3.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private GoogleApiClient mClient;
    private String userEmail;
    private TextView searchField;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private FloatingActionButton recipientsBtn;
    private FloatingActionButton parcelsBtn;
    private FloatingActionButton profileBtn;
    private ArrayList<Courier> nearbyCouriers;
    private volatile ArrayList<Customer> recipients;
    private ArrayList<String> recipient_emails;
    private Parcel currentParcel;
    private String currentCourierID;
    private Courier currentCourier;
    private Customer currentUser;
    private Customer currentRecipient;
    private ConfigInfo nvoyConfigInfo;
    private Parcel trackingParcel;
    private ProgressDialog progressDialog;
    private Spinner spinner;
    private String currentParcelKey;
    private boolean acceptedOnce=false;
    private LocationRequest mLocationReq;
    private ArrayList<String> parcelsIDList;



    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        updateConfigInfo();
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        nearbyCouriers = new ArrayList<>();
        recipients = new ArrayList<>();
        parcelsIDList = new ArrayList<>();
        //currentParcel = new Parcel();

        recipientsBtn = (FloatingActionButton) findViewById(R.id.button_recipients);
        recipientsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapsActivity = new Intent(MapsActivity.this, RecipientsActivity.class);
                startActivity(mapsActivity);
                //System.out.println("FAB WORKS!");
            }
        });

        parcelsBtn = (FloatingActionButton) findViewById(R.id.button_parcels);
        parcelsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parcelsActivity = new Intent(MapsActivity.this, ParcelsActivity.class);
                startActivity(parcelsActivity);
            }
        });

        profileBtn = (FloatingActionButton) findViewById(R.id.button_profile);
        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent userProfile = new Intent(MapsActivity.this,UserProfile.class);


                userProfile.putExtra("customer",currentUser);
                userProfile.putExtra("customerState","Unknown");
                userProfile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //databaseReference.child("Couriers").child(EncodeString(((TextView)childText.findViewById(R.id.child_text)).getText().toString().trim())).removeEventListener(this);
                startActivity(userProfile);
            }
        });
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference dataReference = database.getReference();


        dataReference.child("Customers").child(EncodeString(FirebaseAuth.getInstance().getCurrentUser().getEmail())).child("parcels")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Iterable<DataSnapshot> parcels = dataSnapshot.getChildren();
                        recipient_emails = new ArrayList<String>();

                        for (DataSnapshot parcel : parcels) {
                            parcelsIDList.add(parcel.getKey().toString());

                        }

                        generateNewParcelNotification();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });




        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setAnchorPoint((float) 0.14);
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (currentCourierID == null) {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    Toast.makeText(getBaseContext(), "Select a Courier first!",
                            Toast.LENGTH_SHORT).show();
                } else if (currentCourier == null) {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                    ((Button) findViewById(R.id.btn_add_delivery_accept)).setError("Confirm Selection!");
                } else {
                    ((Button) findViewById(R.id.btn_add_delivery_accept)).setError(null);
                    calculateFair();
                }
            }
        });

        userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();


        dataReference.child("Customers").child(EncodeString(FirebaseAuth.getInstance().getCurrentUser().getEmail())).child("friends")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Iterable<DataSnapshot> friendList = dataSnapshot.getChildren();
                        recipient_emails = new ArrayList<String>();

                        for (DataSnapshot friend : friendList) {
                            if ((friend.getValue().toString()).equals("New")) {
                                Intent intent = new Intent(MapsActivity.this, RecipientsActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                PendingIntent pendingIntent = PendingIntent.getActivity(MapsActivity.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MapsActivity.this);
                                notificationBuilder.setContentTitle("New Recipient Request");
                                notificationBuilder.setContentText("From: " + DecodeString(friend.getKey().toString()));
                                notificationBuilder.setAutoCancel(true);
                                notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
                                notificationBuilder.setContentIntent(pendingIntent);
                                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                int m = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
                                notificationManager.notify(m, notificationBuilder.build());
                            }

                            if ((friend.getValue().toString()).equals("Accepted")) {
                                recipient_emails.add(friend.getKey().toString());
                            }
                        }

                        updateCustomers();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        ((Button) findViewById(R.id.btn_add_delivery_accept)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentCourierID != null) {
                    for (Courier c : nearbyCouriers) {
                        if (c.getUserID().equals(currentCourierID)) {
                            currentCourier = c;
                        }
                    }
                    if (currentCourier.isExpressCourier()) {
                        ((TextView) findViewById(R.id.text_delivery_method)).setText("EXPRESS DELIVERY");
                        ((TextView) findViewById(R.id.text_delivery_method)).setTextColor(Color.RED);
                    } else {
                        ((TextView) findViewById(R.id.text_delivery_method)).setText("REGULAR DELIVERY");
                        ((TextView) findViewById(R.id.text_delivery_method)).setTextColor(Color.GREEN);
                    }
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                } else {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    Toast t = new Toast(getBaseContext());
                    t.makeText(getBaseContext(), "Select a courier to begin!",
                            Toast.LENGTH_LONG).show();
                }


            }
        });

        ((Button) findViewById(R.id.btn_add_delivery_request)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView packageDetails = (TextView) findViewById(R.id.parcel_description);
                if (currentParcel != null) {
                    if (packageDetails.getText().toString().equals("")) {
                        packageDetails.setError("This field cannot be empty!");
                    } else {
                        currentParcel.setItemDescription(packageDetails.getText().toString());
                        ((Button) findViewById(R.id.btn_add_delivery_request)).setError(null);
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        final DatabaseReference dataReference = database.getReference();
                        final String key = dataReference.child("Parcels").push().getKey();
                        currentParcel.setParcelID(key);
                        currentParcelKey = key;
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String currentDateTime = dateFormat.format(new Date());
                        currentParcel.setLastUpdated(currentDateTime);
                        dataReference.child("Parcels").child(key).setValue(currentParcel);
                        dataReference.child("Customers")
                                .child(currentParcel.getSenderID()).child("parcels")
                                .child(key).setValue(currentDateTime);
                        dataReference.child("Customers")
                                .child(currentParcel.getReceiverID()).child("parcels")
                                .child(key).setValue(currentDateTime);
                        dataReference.child("Couriers")
                                .child(currentParcel.getCarrierID()).child("parcels")
                                .child(key).setValue(currentDateTime);
                        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        showProgressWindow(true);

                        dataReference.child("Parcels").child(currentParcelKey).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Parcel p = dataSnapshot.getValue(Parcel.class);
                                currentParcel = p;
                                if (p.getStatus() == Parcel.ACCEPTED) {
                                    showProgressWindow(false);
                                    currentCourierID = null;
                                    acceptedOnce = true;
                                    new AlertDialog.Builder(MapsActivity.this)
                                            .setTitle("Parcel was marked as accepted!")
                                            .setMessage("You can contact courier now or you can access the details about " +
                                                    "your parcel from parcels menu. The courier will contact " +
                                                    "you before collecting the parcel.")
                                            .setPositiveButton(R.string.contact_courier, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String number = currentCourier.getContactNumber();
                                                    Uri call = Uri.parse("tel:" + number);
                                                    Intent surf = new Intent(Intent.ACTION_DIAL, call);
                                                    startActivity(surf);

                                                }
                                            })
                                            .setNegativeButton(R.string.cancel_contact_courier, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();

                                } else if (p.getStatus() == Parcel.CANCELLED) {
                                    dataReference.child("Parcels").child(currentParcelKey).removeEventListener(this);
                                    showProgressWindow(false);
                                    new AlertDialog.Builder(MapsActivity.this)
                                            .setTitle("Courier has rejected your request!")
                                            .setMessage("Unfortunately, the courier has rejected your request, ID:"+currentParcelKey+". You can always try another courier.")

                                            .setNegativeButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }else if (p.getStatus() == Parcel.PICKUP) {
                                    showProgressWindow(false);
                                    new AlertDialog.Builder(MapsActivity.this)
                                            .setTitle("Parcel has marked as picked up")
                                            .setMessage("Did courier pick up the parcel from you?")
                                            .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    changeParcelStatus(Parcel.IN_TRANSIT);
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    changeParcelStatus(Parcel.CUST_MARKED_NOT_COLLECTED);
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();

                                }else if (p.getStatus() == Parcel.MARKED_DELIVERED && p.getReceiverID().equals(EncodeString(FirebaseAuth.getInstance().getCurrentUser().getEmail()))) {
                                    showProgressWindow(false);
                                    new AlertDialog.Builder(MapsActivity.this)
                                            .setTitle("Parcel has marked as delivered")
                                            .setMessage("Did courier deliver the parcel?")
                                            .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    changeParcelStatus(Parcel.DELIVERED);
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    changeParcelStatus(Parcel.CUST_MARKED_NOT_DELIVERED);
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        //progressDialog.show(getBaseContext(), "Getting Route", "Waiting for courier", false, false);


                    }

                } else {
                    //slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    ((Button) findViewById(R.id.btn_add_delivery_request)).setError("Complete Parcel Details!");
                    packageDetails.setError("Select a recipient!");
                }


            }
        });

        ((Button) findViewById(R.id.btn_add_delivery_cancel_1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                currentCourierID = null;
                currentCourier = null;
                currentParcel = null;
                ((TextView) findViewById(R.id.text_delivery_fair)).setText("");
                ((TextView) findViewById(R.id.text_delivery_method)).setText("");
                ((TextView) findViewById(R.id.text_selected_courier)).setText("");
            }
        });

        ((Button) findViewById(R.id.btn_add_delivery_cancel_2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                ((TextView) findViewById(R.id.text_selected_courier)).setText("");
                ((TextView) findViewById(R.id.text_delivery_fair)).setText("");
                ((TextView) findViewById(R.id.text_delivery_method)).setText("");
                currentCourierID = null;
                currentCourier = null;
                currentParcel = null;
            }
        });


        spinner = (Spinner) findViewById(R.id.spinner_recipients);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                calculateFair();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void generateNewParcelNotification(){
        if(parcelsIDList!=null){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dataReference = database.getReference();
            dataReference.child("Parcels").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Iterable<DataSnapshot> parcels = dataSnapshot.getChildren();
                    ArrayList<Parcel> parcelsList = new ArrayList<Parcel>();
                    for (DataSnapshot parcel : parcels) {
                        Parcel tempParcel = parcel.getValue(Parcel.class);
                        if(parcelsIDList.contains(tempParcel.getParcelID() )){
                            parcelsList.add(tempParcel);
                            if(tempParcel.getStatus() == Parcel.MARKED_DELIVERED){
                                Intent parcelProfile = new Intent(MapsActivity.this, ParcelProfile.class);
                                parcelProfile.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                parcelProfile.putExtra("parcel",tempParcel);
                                parcelProfile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                PendingIntent pendingIntent = PendingIntent.getActivity(MapsActivity.this, 0, parcelProfile, PendingIntent.FLAG_ONE_SHOT);
                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MapsActivity.this);
                                notificationBuilder.setContentTitle("Parcel marked as delivered!");
                                notificationBuilder.setContentText("From: " + DecodeString(tempParcel.getCarrierID()));
                                notificationBuilder.setAutoCancel(true);
                                notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
                                notificationBuilder.setContentIntent(pendingIntent);
                                notificationBuilder.setOngoing(true);
                                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                int m = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
                                notificationManager.notify(m, notificationBuilder.build());
                            }
                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void calculateFair(){
        if (recipients.size() != 0 && currentCourier != null) {
            currentRecipient = recipients.get(spinner.getSelectedItemPosition());
            LatLng senderLoc = new LatLng(Double.parseDouble(currentUser.getLocation().getLatitude()), Double.parseDouble(currentUser.getLocation().getLongitude().toString()));
            LatLng receiverLoc = new LatLng(Double.parseDouble(currentRecipient.getLocation().getLatitude()), Double.parseDouble(currentRecipient.getLocation().getLongitude().toString()));
            Double distance = SphericalUtil.computeDistanceBetween(senderLoc, receiverLoc);
            int deliveryFair = (int) (((distance - 2) / 1000) * nvoyConfigInfo.getNormalDeliveryChargePerKM()
                    + nvoyConfigInfo.getNormalDeliveryFixedCharge());
            ((TextView) findViewById(R.id.text_delivery_fair)).setText("Delivery Fair : " + deliveryFair + ".0 LKR");
            currentParcel = new Parcel();
            currentParcel.setSenderID(currentUser.getUserID());
            currentParcel.setReceiverID(currentRecipient.getUserID());
            currentParcel.setCarrierID(currentCourier.getUserID());
            currentParcel.setCurrentLocation(currentUser.getLocation());
            currentParcel.setDeliveryFair(deliveryFair);
            currentParcel.setStatus(Parcel.NEW);


        }
    }

    private void showProgressWindow(boolean state) {
        if (state) {

            progressDialog = progressDialog.show(this, "Waiting for courier", "Be patient while the courier accepts your request", false, false);
            Runnable progressRunnable = new Runnable() {

                @Override
                public void run() {
                    if(currentParcel.getStatus()==Parcel.NEW){
                        progressDialog.dismiss();
                        new AlertDialog.Builder(MapsActivity.this)
                                .setTitle("Courier is not responding!")
                                .setMessage("The courier is not responding to your request. You can always try another courier.")

                                .setNegativeButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        changeParcelStatus(Parcel.TIME_OUT);

                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }


                }
            };

            Handler pdCanceller = new Handler();
            pdCanceller.postDelayed(progressRunnable, 120000);
        } else {
            progressDialog.dismiss();
        }

    }

    private void changeParcelStatus(int status){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dataReference = database.getReference();
        dataReference.child("Parcels").child(currentParcelKey).child("status").setValue(status);

    }

    public static String EncodeString(String string) {
        return string.replace(".", ",");
    }

    public static String DecodeString(String string) {
        return string.replace(",", ".");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        if (mMap != null) {
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {

                    if (marker.getTitle().equals("Me")) {
                        return null;
                    }
                    View v = getLayoutInflater().inflate(R.layout.info_layout, null);

                    TextView name = (TextView) v.findViewById(R.id.courier_name);
                    TextView contact = (TextView) v.findViewById(R.id.courier_contact);
                    Button button = (Button) v.findViewById(R.id.courier_button_contact);

                    name.setText(marker.getTitle());
                    contact.setText(marker.getSnippet());

                    return v;
                }
            });
        }


        mClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mClient.connect();
    }



    @Override
    protected void onResume() {
        super.onResume();

        if (mClient != null) {
            mClient.connect();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mClient != null) {
            mClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(this,new String[]   {
                        Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},PackageManager.PERMISSION_GRANTED);



                return;
            }
        }

        mMap.setMyLocationEnabled(true);

        checkGPS();
        mLocationReq = LocationRequest.create();
        mLocationReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationReq.setInterval(30000);



        try{
            //mMap.setMyLocationEnabled(true);
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient,mLocationReq,this);
            Location l = LocationServices.FusedLocationApi.getLastLocation(mClient);
            LatLng ll = new LatLng(l.getLatitude(),l.getLongitude());
            CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(ll,15);
            mMap.animateCamera(camUpdate);
            updateMyLocation(ll);
        }catch (Exception e){

        }

        updateCouriers();

        //LocationServices.FusedLocationApi.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this)
    }

    private void updateConfigInfo(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dataReference = database.getReference();
        dataReference.child("ConfigInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                nvoyConfigInfo = dataSnapshot.getValue(ConfigInfo.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void updateMyLocation(LatLng location){
        if(currentUser!=null){
            currentUser.setLocation(new com.proximosolutions.nvoycustomer3.MainLogic.Location());
            currentUser.getLocation().setLatitude(String.valueOf(location.latitude));
            currentUser.getLocation().setLongitude(String.valueOf(location.longitude));
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dataReference = database.getReference();
            dataReference.child("Customers")
                    .child(currentUser.getUserID())
                    .child("location")
                    .child("latitude").setValue(currentUser.getLocation().getLatitude());
            dataReference.child("Customers")
                    .child(currentUser.getUserID())
                    .child("location")
                    .child("longitude").setValue(currentUser.getLocation().getLongitude());

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    Marker marker;

    @Override
    public void onLocationChanged(Location location) {


        if(location == null){
            Toast.makeText(this, "Cannot get current location",
                    Toast.LENGTH_SHORT).show();
        }else{
            LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
            //mMap.addMarker(new MarkerOptions().position(ll).title("My Location"));
            /*CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(ll,15);
            mMap.animateCamera(camUpdate);*/
            if(marker != null){
                marker.remove();
            }

            MarkerOptions markerOptions = new MarkerOptions()
                    .title("Me")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.myself))
                    .position(ll);
            marker = mMap.addMarker(markerOptions);
            updateMyLocation(ll);


        }


    }



    private void checkGPS(){
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable GPS")
                    .setMessage("Turn on your GPS in high accuracy mode from settings")
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(settings);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }

    }

    private void updateCustomers(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dataReference = database.getReference();
        dataReference.child("Customers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> customerList = dataSnapshot.getChildren();
                ArrayList<Customer> customers = new ArrayList<Customer>();
                ArrayList<String> custName = new ArrayList<String>();
                for (DataSnapshot customer : customerList) {
                    Customer temp = customer.getValue(Customer.class);
                    if(recipient_emails.contains(temp.getUserID())){
                        customers.add(temp);
                        custName.add(temp.getFirstName() +" "+temp.getLastName());
                    }
                    if(temp.getUserID().equals(EncodeString(FirebaseAuth.getInstance().getCurrentUser().getEmail().toString())) ){
                        currentUser = temp;
                    }
                }
                recipients = customers;
                Spinner sp = (Spinner)findViewById(R.id.spinner_recipients);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),R.layout.nvoy_spinner_item,custName);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp.setAdapter(adapter);



            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateCouriers(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dataReference = database.getReference();

        dataReference.child("Couriers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> courierList = dataSnapshot.getChildren();
                ArrayList<Courier> couriers = new ArrayList<Courier>();
                for (DataSnapshot courier : courierList) {
                    Courier tempCourier = new Courier();
                    tempCourier.setUserID(courier.child("userID").getValue().toString());
                    tempCourier.setFirstName(courier.child("firstName").getValue().toString());
                    tempCourier.setLastName(courier.child("lastName").getValue().toString());
                    tempCourier.setContactNumber(courier.child("contactNumber").getValue().toString());
                    tempCourier.setActive((boolean)courier.child("active").getValue());
                    tempCourier.setExpressCourier((boolean)courier.child("expressCourier").getValue());
                    com.proximosolutions.nvoycustomer3.MainLogic.Location courierLocation = new com.proximosolutions.nvoycustomer3.MainLogic.Location();
                    if(courier.child("location").child("latitude").getValue().toString().equals("")){
                        continue;
                    }
                    courierLocation.setLatitude(courier.child("location").child("latitude").getValue().toString());
                    courierLocation.setLongitude(courier.child("location").child("longitude").getValue().toString());
                    tempCourier.setCurrentLocation(courierLocation);
                    if(tempCourier.isActive()){
                        couriers.add(tempCourier);
                    }


                }
                nearbyCouriers = couriers;
                addCourierMarkers(couriers);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    ArrayList<Marker> markers;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void addCourierMarkers(ArrayList<Courier> couriers){
        if(markers!=null){
            for(Marker marker:markers){
                if(marker != null){
                    marker.remove();
                }
            }

        }
        markers = new ArrayList<>();
        for(Courier courier: couriers){
            if(courier.isExpressCourier()){
                LatLng ll = new LatLng(Double.parseDouble(courier.getCurrentLocation().getLatitude()) ,Double.parseDouble(courier.getCurrentLocation().getLongitude()));
                MarkerOptions markerOptions = new MarkerOptions()
                        .title(courier.getFirstName()+" "+courier.getLastName())
                        .snippet(courier.getUserID())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.express_courier))
                        .position(ll);
                Marker tempMarker = mMap.addMarker(markerOptions);

                markers.add(tempMarker);
            }else{
                LatLng ll = new LatLng(Double.parseDouble(courier.getCurrentLocation().getLatitude()) ,Double.parseDouble(courier.getCurrentLocation().getLongitude()));
                MarkerOptions markerOptions = new MarkerOptions()
                        .title(courier.getFirstName()+" "+courier.getLastName())
                        .snippet(courier.getUserID())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.normal_courier))
                        .position(ll);
                Marker tempMarker = mMap.addMarker(markerOptions);

                markers.add(tempMarker);

            }



        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {

        for(Marker m: markers){
            if(m.getTitle().equals("Me")){
                m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.myself));
            }
        }
        if(!marker.getTitle().equals("Me")){
            //marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.courier_waiting));
            currentCourier = null;
            currentCourierID = null;
            currentCourierID = null;
            ((TextView)findViewById(R.id.text_delivery_method)).setText("");
            ((TextView)findViewById(R.id.text_selected_courier)).setText("");
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
            ((TextView)findViewById(R.id.text_selected_courier)).setText("Selected: "+marker.getTitle());
            currentCourierID = marker.getSnippet();

        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}
