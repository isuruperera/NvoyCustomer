package com.proximosolutions.nvoycustomer3.Controller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.proximosolutions.nvoycustomer3.MainLogic.Customer;
import com.proximosolutions.nvoycustomer3.R;


public class UserProfile extends AppCompatActivity {

    private Button suspendBtn;
    private Button viewTransactionsBtn;
    private String currentUserType;
    private String customerState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //lockIntent = true;
            //this.setActionBar(new ActionBar());
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
           // getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_user_profile);
        //String userName = (String)savedInstanceState.get("userName");
        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            if(extras != null){
                ((android.support.design.widget.CollapsingToolbarLayout)findViewById(R.id.user_profile_toolbar)).setTitle(((Customer)extras.get("customer")).getFirstName()+" "+((Customer)extras.get("customer")).getLastName());
                //((android.support.design.widget.CollapsingToolbarLayout)findViewById(R.id.user_profile_toolbar)).set
                //String s = extras.get("email").toString();
                ((TextView)findViewById(R.id.user_email)).setText(DecodeString(((Customer)extras.get("customer")).getUserID()));
                ((TextView)findViewById(R.id.user_contact_no)).setText(((Customer)extras.get("customer")).getContactNumber());
                ((TextView)findViewById(R.id.user_nic)).setText(((Customer)extras.get("customer")).getNic());

                customerState = (String)extras.get("customerState");
                setCustomerState(customerState);



            }
        }else{
            ((android.support.design.widget.CollapsingToolbarLayout)findViewById(R.id.user_profile_toolbar))
                    .setTitle(((Customer)savedInstanceState
                            .getSerializable("customer"))
                            .getFirstName()
                            +" "+((Customer)savedInstanceState
                            .getSerializable("customer"))
                            .getLastName());

            ((TextView)findViewById(R.id.user_email)).setText(DecodeString(((Customer)savedInstanceState.getSerializable("customer")).getUserID()));
            ((TextView)findViewById(R.id.user_contact_no)).setText(((Customer)savedInstanceState.getSerializable("customer")).getContactNumber());
            ((TextView)findViewById(R.id.user_nic)).setText(((Customer)savedInstanceState.getSerializable("customer")).getNic());
            customerState = (String)savedInstanceState.getSerializable("customerState");
            setCustomerState(customerState);


        }

        ((TextView)findViewById(R.id.user_contact_no)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = ((TextView)findViewById(R.id.user_contact_no)).getText().toString();
                Uri call = Uri.parse("tel:" + number);
                Intent surf = new Intent(Intent.ACTION_DIAL, call);
                startActivity(surf);
            }
        });

        ((TextView)findViewById(R.id.btn_view_parcels)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parcelsActivity = new Intent(UserProfile.this, ParcelsActivity.class);
                startActivity(parcelsActivity);
            }
        });



        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = firebaseDatabase.getReference();
        suspendBtn = (Button) findViewById(R.id.btn_suspend_courier);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        suspendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final TextView email = (TextView)findViewById(R.id.user_email);
                if(customerState.equals("Pending Request")){
                    alertDialogBuilder.setTitle("Remove the pending request?");
                    alertDialogBuilder
                            .setMessage("Click yes to remove!")
                            .setCancelable(false)
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            databaseReference.child("Customers")
                                                    .child(EncodeString(FirebaseAuth
                                                            .getInstance()
                                                            .getCurrentUser().getEmail()))
                                                    .child("friends")
                                                    .child(EncodeString(email.getText().toString()))
                                                    .setValue("Removed");

                                            databaseReference.child("Customers")
                                                    .child(EncodeString(email.getText().toString()))
                                                    .child("friends")
                                                    .child(EncodeString(FirebaseAuth
                                                            .getInstance()
                                                            .getCurrentUser().getEmail()))
                                                    .setValue("Removed");
                                            customerState = "Removed";
                                            setCustomerState(customerState);
                                            onBackPressed();
                                            Log.d("Recipient","Pending Recipient Removed");
                                        }
                                    })

                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }

                if(customerState.equals("New Request!")){
                    alertDialogBuilder.setTitle("Accept the pending request?");
                    alertDialogBuilder
                            .setMessage("Click yes to accept!")
                            .setCancelable(false)
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            ((Button)findViewById(R.id.btn_suspend_courier)).setText("Remove");
                                            ((Button)findViewById(R.id.btn_suspend_courier)).setActivated(false);
                                            databaseReference.child("Customers")
                                                    .child(EncodeString(FirebaseAuth
                                                            .getInstance()
                                                            .getCurrentUser().getEmail()))
                                                    .child("friends")
                                                    .child(EncodeString(email.getText().toString()))
                                                    .setValue("Accepted");

                                            databaseReference.child("Customers")
                                                    .child(EncodeString(email.getText().toString()))
                                                    .child("friends")
                                                    .child(EncodeString(FirebaseAuth
                                                            .getInstance()
                                                            .getCurrentUser().getEmail()))
                                                    .setValue("Accepted");
                                            customerState = "Valid Recipient";
                                            setCustomerState(customerState);

                                            Log.d("Recipient","New Recipient Accepted");
                                        }
                                    })

                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }

                if(customerState.equals("Valid Recipient")){
                    alertDialogBuilder.setTitle("Remove the recipient?");
                    alertDialogBuilder
                            .setMessage("Click yes to remove!")
                            .setCancelable(false)
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            databaseReference.child("Customers")
                                                    .child(EncodeString(FirebaseAuth
                                                            .getInstance()
                                                            .getCurrentUser().getEmail()))
                                                    .child("friends")
                                                    .child(EncodeString(email.getText().toString()))
                                                    .setValue("Removed");

                                            databaseReference.child("Customers")
                                                    .child(EncodeString(email.getText().toString()))
                                                    .child("friends")
                                                    .child(EncodeString(FirebaseAuth
                                                            .getInstance()
                                                            .getCurrentUser().getEmail()))
                                                    .setValue("Removed");
                                            customerState = "Removed";
                                            setCustomerState(customerState);

                                            Log.d("Recipient","Exsisting Recipient Removed");
                                        }
                                    })

                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        });

    }

    private void setCustomerState(String customerState){
        if(customerState.equals("Pending Request")){
            ((TextView)findViewById(R.id.user_is_active)).setText("Pending Request");
            ((Button)findViewById(R.id.btn_suspend_courier)).setText("Remove");
        }

        if(customerState.equals("New Request!")){
            ((TextView)findViewById(R.id.user_is_active)).setText("New Request");
            ((Button)findViewById(R.id.btn_suspend_courier)).setText("Accept");
        }

        if(customerState.equals("Valid Recipient")){
            ((TextView)findViewById(R.id.user_is_active)).setText("Valid Recipient");
            ((Button)findViewById(R.id.btn_suspend_courier)).setText("Remove");
        }

        if(customerState.equals("Removed")){
            ((TextView)findViewById(R.id.user_is_active)).setText("Removed user");
            ((Button)findViewById(R.id.btn_suspend_courier)).setText("");
            ((Button)findViewById(R.id.btn_suspend_courier)).setEnabled(false);
            ((TextView)findViewById(R.id.user_contact_no)).setText("Unavailable");
            ((TextView)findViewById(R.id.user_nic)).setText("Unavailable");

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
