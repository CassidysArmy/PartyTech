package com.example.emilynewman.partytech;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.plus.Plus;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.plus.*;


public class MainActivity2 extends ActionBarActivity {

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient; //TODO connect with bottom one


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);


        Button logOutBtn = (Button) findViewById(R.id.logOutBtn);

        //Listening to button event

        logOutBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                if (view.getId() == R.id.logOutBtn) {
                   if (mGoogleApiClient.isConnected()) { //TODO fix logout button - pass API from previous activity?
                        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                        mGoogleApiClient.disconnect();
                        mGoogleApiClient.connect();
                    }
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity2, menu);
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
