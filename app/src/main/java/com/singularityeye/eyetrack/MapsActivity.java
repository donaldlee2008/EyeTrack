/*
    "Eye Track" is an Android Satellite Tracking App for Live Real Time Satellite Tracking and Predictions.
    Copyright (C) 2016  Manuel Martín-González

    This file is part of "Eye Track".

    "Eye Track" is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    "Eye Track" is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with "Eye Track".  If not, see <http://www.gnu.org/licenses/>.
*/

package com.singularityeye.eyetrack;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.singularityeye.eyetrack.license.License;
import com.singularityeye.eyetrack.license.Serializer;
import com.singularityeye.eyetrack.model.Satellite;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * MapsActivity model is a fragment activity which can be considered as the main component of the application.
 * It includes a google map object where tracking is displayed.
 * @author Manuel Martin-Gonzalez
 * @version 1.0.0-alpha
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // map object
    private GoogleMap map; // map

    // visual map properties
    private LatLng defaultPosition; // default marker position
    private Marker eye_marker; // satellite marker
    private CameraPosition cameraPosition; // camera position
    private TextView textViewPosition; // textView position
    private String position; // position

    // updated marker properties
    private Satellite last_eye; // last tracked eye from soap request
    private LatLng current_position; // current eye position

    // IPC
    private BroadcastReceiver receiver; // broadcast receiver
    private LocalBroadcastManager localBroadcastManager; // helper to register local broadcast receiver
    private ServiceConnection connection; // service connection (binding)
    private BoundService service; // bound service

    // testing request params
    private final String NORAD_ID = "27424";
    private String hostAddress; // device ip
    private final String SECONDS = "1";
    private final String KEY = ((License) Serializer.readObject("manuel.lic")).SERIAL_NUMBER;

    // Broadcast Receiver: receive intents sent by sendBroadcast method
    private class BroadCastReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case BoundService.ACTION_GETPOSITIONSBYIP:

                    Log.d("BROADCASTRECEIVER", "REQUEST_RECEIVED ! :-)"); // debug

                    // get last tracked eye from soap request
                    last_eye = (Satellite) intent.getSerializableExtra(BoundService.OBJECT_GETPOSITIONSBYIP);

                    // update position
                    current_position = new LatLng(last_eye.getLatitude(), last_eye.getLongitude());
                    position = "Short Name: "+last_eye.getShortname()+"\n"
                            +"Latitude: "+current_position.latitude+" º\n"
                            +"Longitude: "+current_position.longitude+" º\n"
                            +"Altitude: "+last_eye.getAltitude()+" km\n";
                    textViewPosition.setText(position);

                    // update marker and camera
                    eye_marker.setPosition(current_position);
                    eye_marker.setTitle(last_eye.getShortname());
                    eye_marker.setSnippet("NORAD ID: " + last_eye.getNORAD_ID());

                    map.animateCamera(CameraUpdateFactory.newLatLng(current_position));

                    break;
                default:
                    break;
            }
        }
    }

    // Service Connection (binding/not_unbinding)
    private class ServiceConnection implements android.content.ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("SERVICECONECTION", "ON_SERVICE_CONNECTED !"); // debug
            MapsActivity.this.service = ((BoundService.LocalBinder) service).getService();
            MapsActivity.this.service.initTask(NORAD_ID, hostAddress, SECONDS, KEY);
        }

        // the connection to the service is unexpectedly lost, such as when the service has crashed or has been killed
        // note: this method will not be called when you unbind to the service
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("ERROR", "The service has been unexpectedly disconnected !");
            // avoid zombie soap requests
            if(null != MapsActivity.this.service) MapsActivity.this.service.stopTask(); // stop sending soap requests
        }
    }

    /**
     * Instance map fragment and create main inter-process communication components.
     * Get notified when the map is ready to be used.
     * Create BroadCast Receiver to receive intents sent by bound service.
     * Create Local Broadcast Manager to register/unregister a receiver for any local broadcasts that match the given intent filter.
     * Create Service Connection (which monitors the connection with the service; binding).
     * @param savedInstanceState saved data that describes the state of the activity when it was destroyed.
     */
    // in RAM
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ONCREATE", "ON_CREATE !"); // debug

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // note: it calls onMapReady method

        hostAddress = getHostAddress();
        Log.d("IP_ADDRESS", "HOST_ADDRESS: " + hostAddress); // debug

        // IPC
        receiver = new BroadCastReceiver(); // create BroadCast Receiver
        IntentFilter filter = new IntentFilter(BoundService.ACTION_GETPOSITIONSBYIP);
        localBroadcastManager = LocalBroadcastManager.getInstance(this); // create localBroadcastManger
        localBroadcastManager.registerReceiver(receiver, filter); // register a receiver for any local broadcasts that match the given intentFilter
        connection = new ServiceConnection(); // create Service Connection (monitors the connection with the service; binding)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install it inside the SupportMapFragment.
     * This method will only be triggered once the user has installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("ONMAPREADY", "ON_MAP_READY !"); // debug

        this.map = googleMap;
        this.map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        this.map.getUiSettings().setMapToolbarEnabled(false);

        // visual components
        defaultPosition = new LatLng(36.723888, -2.177156); // by default

        this.eye_marker = this.map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.eye_icon))
                .rotation(0)
                .position(defaultPosition)
                .flat(true));

        this.textViewPosition = (TextView) findViewById(R.id.textViewPosition);
    }

    /**
     * The activity is being re-displayed to the user.
     * It is called after the activity has been stopped, prior to it being started again.
     */
    @Override
    protected void onRestart() {
        Log.d("ONRESTART", "ON_RESTART !"); // debug
        super.onRestart();
    }

    /**
     * The activity is visible and interacts with the service.
     * Bind to local service. Automatically create the service as long as the binding exists.
     */
    // in cache, visible;
    @Override
    protected void onStart() {
        Log.d("ONSTART", "ON_START !"); // debug
        // bind to local service; it calls onServiceConnected on the ServiceConnection,
        // to deliver the IBinder that the client can use to communicate with the service;
        // note: multiple clients can connect to service at once,
        // however the system use the same IBinder (which first client binds) to
        // any additional clients that bind without calling onBind() again
        // FLAG note: automatically create the service as long as the binding exists
        bindService(new Intent(this, BoundService.class), connection, Context.BIND_AUTO_CREATE); // start sending soap requests
        super.onStart();
    }

    /**
     * The activity is visible and in action.
     * We should keep the processing that occurs at this transition to a minimum.
     */
    // visible, in action;
    @Override
    protected void onResume() {
        Log.d("ONRESUME", "ON_RESUME !"); // debug
        super.onResume();
    }

    /**
     * The activity is partially visible.
     * We should keep the processing that occurs at this transition to a minimum.
     */
    // in cache, partially visible;
    @Override
    protected void onPause() {
        Log.d("ONPAUSE", "ON_PAUSE !"); // debug
        super.onPause();
    }

    /**
     * The activity is hidden.
     * The activity does not interact with the service while the activity is hidden.
     * Unbind service (automatically destroy bound service) and stop sending soap requests.
     */
    // in RAM, hidden;
    @Override
    protected void onStop() {
        Log.d("ONSTOP", "ON_STOP !"); // debug
        // note: automatically destroy bound service
        unbindService(connection); // unbind service and stop sending soap requests
        super.onStop();
    }

    /**
     * The activity is destroyed.
     * Unregister (a previously registered) broadcast receiver.
     */
    // out of RAM
    @Override
    protected void onDestroy() {
        Log.d("ONDESTROY","ON_DESTROY !"); // debug
        localBroadcastManager.unregisterReceiver(receiver); // unregister a previously registered broadcastReceiver
        super.onDestroy();
    }

    // get host address (needed for soap requests)
    private String getHostAddress() {
        try {
            // iterate over interfaces
            for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements();) {
                NetworkInterface nextInterface = networkInterfaces.nextElement();
                // iterate over ip addresses (note: one interface could have more than one ip address)
                for (Enumeration<InetAddress> ipAddresses = nextInterface.getInetAddresses(); ipAddresses.hasMoreElements();) {
                    InetAddress nextIpAddress = ipAddresses.nextElement();
                    // get an IPv4 address
                    if (!nextIpAddress.isLoopbackAddress() && nextIpAddress instanceof Inet4Address) {
                        return nextIpAddress.getHostAddress(); // get first ip address
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("ERROR", "GetHostAddressException: "+e.getMessage());
        } return null;
    }

}
