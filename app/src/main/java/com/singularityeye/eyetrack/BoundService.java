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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.singularityeye.eyetrack.model.Satellite;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.Vector;

/**
 * BoundService model is a Service that includes a single background task/thread to execute soap requests and send local broadcasts (actions) to client.
 * @author Manuel Martin-Gonzalez
 * @version 1.0.0-alpha
 */
public class BoundService extends Service {

    // soap web services communication
    private static final String NAMESPACE = "urn:satwsdl"; // soap web services namespace
    private static final String METHOD_NAME = "GetPositionsByIp"; // soap web service method name
    private static final String SOAP_ACTION = NAMESPACE + "/" + METHOD_NAME; // soap web services action
    private static final String URL = "http://www.n2yo.com/sat/satws.php"; // soap web services url (?wsdl)
    private String param_id, param_ip, param_seconds, param_key; // properties/params for soap request method

    // thread: executes soap requests and sends local broadcasts (actions) to client
    private SOAPWSTask task;

    // task model;
    // send soap web services requests;
    // send local broadcasts to client
    private class SOAPWSTask extends Thread {

        private boolean flag; // task control tag

        @Override
        public void run() {
            flag = true;
            while (flag){
                try {
                    if(sendRequest()) sendBroadCast(); // execute soap request & send broadcast (action)
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("ERROR", "ThreadInterruptedException: "+e.getMessage());
                }
            }
            Log.d("TASK", "TASK/THREAD HAS BEEN STOPPED ! (STOP SENDING REQUESTS)"); // debug
        }

        public void stopTask(){
            flag = false;
        }

    }

    private final IBinder binder = new LocalBinder(); // binder: the programming interface that clients use to interact with the service

    /**
     * Local binder/proxy model with a method to obtain the service reference.
     * With this reference, clients can make calls to service methods.
     * Local service implies that service is in the same process as client.
     * Note: Take into account that the most important part is defining the service's IBinder interface (Binder implements IBinder).
     */
    // note: take into account that service is not a thread;
    // note: a remote service is a service that is in a different process
    public class LocalBinder extends Binder{
        BoundService getService(){
            return BoundService.this;
        }
    }

    // last eye position
    private Satellite last_eye;

    /**
     * Given action definition for intent in sendBroadCast method (it has the same name as soap web services method).
     */
    public static final String ACTION_GETPOSITIONSBYIP = "com.singularityeye.eyetrack.action.GETPOSITIONSBYIP";
    /**
     * Given object definition for intent extra data in sendBroadCast method.
     */
    public static final String OBJECT_GETPOSITIONSBYIP = "LAST_EYE";

    /**
     * Provides binding for the service.
     * @param intent binding intent
     * @return local binder/proxy
     */
    // note: service is running and client is bound for first time
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("SERVICE", "ON_BIND !"); // debug
        return binder;
    }

    /**
     * Provides unbinding for the service (stops sending soap requests).
     * @param intent unbinding intent
     * @return false (boolean), it means that onRebind method later is not called when clients bind to service
     */
    // called when all clients have disconnected from a particular interface;
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("SERVICE", "ON_UNBIND !"); // debug
        stopTask();  // stop sending soap requests
        return false; // onRebind method later is not called when clients bind to service
    }

    /**
     * Bound service is destroyed.
     * When the last client unbinds from the service the system destroys the service.
     */
    @Override
    public void onDestroy() {
        Log.d("SERVICE", "ON_DESTROY !"); // debug
        super.onDestroy();
    }

    /**
     * Create new task/thread.
     * It starts sending soap requests, and sending local broadcasts (actions) to client.
     * @param id string that identifies the platform
     * @param ip android device ip
     * @param seconds the last seconds for what we get the positions
     * @param key string that holds the soap web service license key
     */
    public void initTask(String id, String ip, String seconds, String key){
        Log.d("TASK", "INIT TASK: CREATE AND START NEW TASK !"); // debug

        // set params for request
        this.param_id = id;
        this.param_ip = ip;
        this.param_seconds = seconds;
        this.param_key = key; // (.lic)

        // create and start new thread/task
        this.task = new SOAPWSTask();
        this.task.start();
    }

    /**
     * Stop the main task/thread.
     * It stops sending soap requests, and sending local broadcasts (actions) to client.
     */
    public void stopTask(){
        this.task.stopTask();
    }

    // send soap request and set last eye position
    // if no response return false
    private boolean sendRequest(){
        Log.d("SERVICE", "START_REQUEST !"); // debug

        // create soap request
        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
        // set properties
        request.addProperty("id", this.param_id);
        request.addProperty("ip", this.param_ip);
        request.addProperty("seconds", this.param_seconds);
        request.addProperty("license", this.param_key);

        // create envelope
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet = true;
        // set soap object/request
        envelope.setOutputSoapObject(request);

        // send envelope (http)
        HttpTransportSE httpTransport = new HttpTransportSE(URL);
        try {
            httpTransport.call(SOAP_ACTION, envelope);
        } catch (Exception e) {
            Log.e("ERROR", "HTTPTransportException: "+e.getMessage());
            return false;
        }

        // get response
        Vector<SoapObject> response_array = null;
        try {
            response_array = (Vector<SoapObject>) envelope.getResponse(); // list of satellite positions
        } catch (SoapFault soapFault) {
            Log.e("ERROR", "SoapFault: "+soapFault.faultstring);
            return false;
        }
        // get last eye position from response
        if(null == response_array) return false;
        SoapObject last_eye_position = response_array.get(response_array.size()-1); // get last eye position
        last_eye = new Satellite();
        last_eye.setNORAD_ID(last_eye_position.getPrimitivePropertyAsString("id"));
        last_eye.setShortname(last_eye_position.getPrimitivePropertyAsString("shortname"));
        last_eye.setLatitude(Double.parseDouble(last_eye_position.getPrimitivePropertyAsString("satlatitude")));
        last_eye.setLongitude(Double.parseDouble(last_eye_position.getPrimitivePropertyAsString("satlongitude")));
        last_eye.setAltitude(Double.parseDouble(last_eye_position.getPrimitivePropertyAsString("sataltitude")));
        // show last position
        Log.d("LAST_SATELLITE_POSITION", "ID = " + last_eye.getNORAD_ID()); // debug
        Log.d("LAST_SATELLITE_POSITION", "SHORT_NAME = "+last_eye.getShortname()); // debug
        Log.d("LAST_SATELLITE_POSITION", "SAT_LATITUDE = "+last_eye.getLatitude()); // debug
        Log.d("LAST_SATELLITE_POSITION", "SAT_LONGITUDE = "+last_eye.getLongitude()); // debug
        Log.d("LAST_SATELLITE_POSITION", "SAT_ALTITUDE = "+last_eye.getAltitude()); // debug
        return true;
    }

    // send object by local broadcast to client
    private void sendBroadCast(){
        Log.d("SERVICE", "SEND_BROADCAST !"); // debug
        if(null != last_eye){
            Intent intent = new Intent(ACTION_GETPOSITIONSBYIP);
            intent.putExtra(OBJECT_GETPOSITIONSBYIP, last_eye);
            // note: send broadcasts of intents to local objects within activity process
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent); // dispatch intent via LocalBroadcastManager
        }else{
            Log.e("ERROR", "Broadcast not sent (data not found)");
        }
    }

}
