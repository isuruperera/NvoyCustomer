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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.SearchView;
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
import java.util.Iterator;

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
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        nearbyCouriers = new ArrayList<>();
        recipients = new ArrayList<>();
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
                Intent profileActivity = new Intent(MapsActivity.this, ProfileActivity.class);
                startActivity(profileActivity);
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
                    final Spinner spinner = (Spinner) findViewById(R.id.spinner_recipients);
                    spinner.setSelection(0);
                }
            }
        });

        userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dataReference = database.getReference();
        dataReference.child("Customers").child(EncodeString(userEmail)).child("friends")
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
                                notificationManager.notify(0, notificationBuilder.build());
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

                        dataReference.child("Parcels").child(key).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Parcel p = dataSnapshot.getValue(Parcel.class);
                                if (p.getStatus() == Parcel.ACCEPTED) {
                                    showProgressWindow(false);
                                    currentCourierID = null;
                                    currentCourier = null;
                                    dataReference.child("Parcels").child(key).removeEventListener(this);
                                    new AlertDialog.Builder(MapsActivity.this)
                                            .setTitle("Courier has accepted your request!")
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
                                    showProgressWindow(false);
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


        final Spinner spinner = (Spinner) findViewById(R.id.spinner_recipients);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
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

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


    }

    private void showProgressWindow(boolean state) {
        if (state) {

            progressDialog = progressDialog.show(this, "Waiting for courier", "Be patient while the courier accepts your request", false, false);
        } else {
            progressDialog.dismiss();
        }

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
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(null, "Click",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    return v;
                }
            });
        }

        Toast.makeText(this, "Map Ready",
                Toast.LENGTH_SHORT).show();
        mClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mClient.connect();
    }

    private LocationRequest mLocationReq;

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
        //onRequestPermissionsResult(PackageManager.PERMISSION_GRANTED,new String[]   {
              //  Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
              //  new int[]{PackageManager.PERMISSION_GRANTED,PackageManager.PERMISSION_GRANTED}){




        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }*/
        mMap.setMyLocationEnabled(true);
        Toast.makeText(this, "Connected",
                Toast.LENGTH_SHORT).show();
        checkGPS();
        mLocationReq = LocationRequest.create();
        mLocationReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationReq.setInterval(30000);

        Toast.makeText(this, "Request Location",
                Toast.LENGTH_SHORT).show();

        try{
            //mMap.setMyLocationEnabled(true);
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient,mLocationReq,this);
            Location l = LocationServices.FusedLocationApi.getLastLocation(
                    mClient);
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
        Toast.makeText(this, "Connection Suspended",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed!",
                Toast.LENGTH_SHORT).show();
    }

    Marker marker;

    @Override
    public void onLocationChanged(Location location) {

        /*Toast.makeText(this, "Lcation Changed",
                Toast.LENGTH_SHORT).show();*/
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
                Toast.makeText(getBaseContext(), "Customers Updated!",
                        Toast.LENGTH_SHORT).show();


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


            Toast.makeText(this, "Courier Markers Updated",
                    Toast.LENGTH_SHORT).show();
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
            //((TextView)findViewById(R.id.text_selected_courier_contact)).setText(marker.getSnippet());

        }


        Toast.makeText(this, marker.getTitle(),
                Toast.LENGTH_SHORT).show();
        return true;
    }


}
