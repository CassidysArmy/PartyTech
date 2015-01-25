package com.example.emilynewman.partytech;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.*;
import com.google.android.gms.plus.model.people.Person;
import com.couchbase.lite.*;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.util.Log;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

//TODO after screen 3 go to screen 2
//TODO fix "Log in with Google" button (make it actually appear)

public class MainActivity3 extends ActionBarActivity
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {


    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;


    /* Track whether the sign-in button has been clicked so that we know to resolve
    * all issues preventing sign-in without waiting.
    */
    private boolean mSignInClicked;

    /* Store the connection result from onConnectionFailed callbacks so that we can
     * resolve them when the user clicks sign-in.
     */
    private ConnectionResult mConnectionResult;


    //Couchbase
    private Database database;
    private String docID;

    //User info
    private Person currentPerson;
    private String personName;
    private Person.Image personPhoto;
    private String personGooglePlusProfile;
    private Person.AgeRange personAgeRange;


    final String TAG = "GoogleProcesses";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Client Initiated");
        //Create a Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        Log.d(TAG, "Client Built");



        // create a manager
        Manager manager;
        try {
            manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            Log.d (TAG, "Manager created");
        } catch (IOException e) {
            Log.e(TAG, "Cannot create manager object");
            return;
        }

        // create a name for the database and make sure the name is legal
        String dbname = "users";
        if (!Manager.isValidDatabaseName(dbname)) {
            Log.e(TAG, "Bad database name");
            return;
        }
        // create a new database
        try {
            database = manager.getDatabase(dbname);
            Log.d (TAG, "Database created");
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot get database");
            return;
        }

        // get the current date and time
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        // create an object that contains data for a document
        Map<String, Object> docContent = new HashMap<String, Object>();
        docContent.put("message", "TetherDex Users");
        docContent.put("creationDate", currentTimeString);
        // display the data for the new document
        Log.d(TAG, "docContent=" + String.valueOf(docContent));
        // create an empty document
        Document document = database.createDocument();
        // add content to document and write the document to the database
        try {
            document.putProperties(docContent);
            Log.d (TAG, "Document written to database named " + dbname + " with ID = " + document.getId());
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot write document to database", e);
        }
        // save the ID of the new document
        String docID = document.getId();
        Log.d(TAG, "Document saved");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        //Connected to Google Play services!
        mSignInClicked = false;
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();


        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            personName = currentPerson.getDisplayName();
            personPhoto = currentPerson.getImage();
            personGooglePlusProfile = currentPerson.getUrl();
            personAgeRange = currentPerson.getAgeRange();
        }

        // retrieve the document from the database
        Document retrievedDocument = database.getDocument(docID);
        // display the retrieved document
        Log.d(TAG, "retrievedDocument=" + String.valueOf(retrievedDocument.getProperties()));

        // update the document
        Map<String, Object> updatedProperties = new HashMap<String, Object>();
        updatedProperties.putAll(retrievedDocument.getProperties());
        updatedProperties.put("currentUser", currentPerson);
        updatedProperties.put ("userName", personName);
        updatedProperties.put ("userPhoto", personPhoto);
        updatedProperties.put ("userGPP", personGooglePlusProfile);
        updatedProperties.put ("userAgeRange", personAgeRange);
        try {
            retrievedDocument.putProperties(updatedProperties);
            Log.d(TAG, "updated retrievedDocument=" + String.valueOf(retrievedDocument.getProperties()));
        } catch (CouchbaseLiteException e) {
            Log.e (TAG, "Cannot update document", e);
        }

    }

    public void signInButton(View view) {
        // Do something in response to button click

        if (view.getId() == R.id.sign_in_button
                && !mGoogleApiClient.isConnecting() ) {
            mSignInClicked = true;
            resolveSignInError();
        }
    }




    public void onDisconnected() {
        Toast.makeText(this, "Goodbye", Toast.LENGTH_LONG).show();
    }

    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                startIntentSenderForResult(result.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
            Intent nextScreen = new Intent(getApplicationContext(), MainActivity2.class);
            startActivity(nextScreen);
        }
    }

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                startIntentSenderForResult(mConnectionResult.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }




    /*-====================NON GOOGLE=============================*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity3, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





}