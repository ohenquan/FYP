package com.example.enquan.bikesharing;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.Manifest;
import android.os.Bundle;
import android.os.Build;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Mainpage extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    ArrayList<BluetoothDevice> searchedList = new ArrayList<BluetoothDevice>();
    ArrayList<String> checkCurrentHash = new ArrayList<String>();
    String lockDeviceAddress = "";
    String currentParkingId = "";
    String pw_num = "";
    String thePassword = "";
    String theHash = "";
    String passwordList = "";
    String parkingAddress = "";
    TextView trip_view;
    Button i1, unlock_bike;
    String name = null;
    String url_post = "http://172.20.10.3:8000";
    String id_bike = "";
    String statusTrip = "locked";
    int duration = Toast.LENGTH_SHORT;
    int seconds, minutes = 0;
    boolean lockIsNear = false;
    boolean parkingIsNear = false;
    boolean stop1 = false;
    double[] lati ={1.444334,1.443172,1.443074,1.441882,1.441433,1.443812,1.444390,1.445655,1.446040};
    double[] longti = {103.783609,103.784533,103.785035,103.785092,103.786004,103.786091,103.786582,103.785477,103.784448};

    // UUIDs for UAT service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    // BTLE state
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;

    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // Discover services.
                if (!gatt.discoverServices()) {
                    Log.d("Mainpage", "Fail Discovering");
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d("Mainpage", "Disconnected!");
            } else {
                Log.d("Mainpage", "State changed");
            }


        }

        // Called when services have been discovered on the remote device.
        // It seems to be necessary to wait for this discovery to occur before
        // manipulating any services or characteristics.
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Mainpage", "Service discovery completed");
            } else {
                Log.d("Mainpage", "Service discovery failed with status: " + status);
            }
            // Save reference to each characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                Log.d("Mainpage", "Couldn't set notifications for RX characteristic!");
            }
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                if (!gatt.writeDescriptor(desc)) {
                    Log.d("Mainpage", "Couldn't write RX client descriptor value!");
                }
            } else {
                Log.d("Mainpage", "Couldn't get RX client descriptor!");
            }
            if (statusTrip.equals("locked")) {
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                getBikeId();
                            }
                        },
                        2000
                );
            }
            if (statusTrip.equals("unlocked")) {
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                statusTrip = "hashing";
                                getParkingHash();
                            }
                        },
                        2000
                );
            }
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d("Mainpage", "Received: " + characteristic.getStringValue(0));
            if (statusTrip.equals("locked") &&  characteristic.getStringValue(0).contains("bike")) {
                id_bike += characteristic.getStringValue(0);
                Log.d("Mainpage", "setting idbike to:" + id_bike);
                statusTrip = "unlocking";
            }
            else if (statusTrip.equals("locked") &&  !characteristic.getStringValue(0).contains("bike")){
                gatt.disconnect();
                adapter.startLeScan(scanCallback);
            }
            else if (statusTrip.equals("unlocking")) {
                thePassword = characteristic.getStringValue(0);
                verifyBikePw(thePassword);
            } else if (statusTrip.equals("hashing")) {
                if(!checkCurrentHash.contains(characteristic.getStringValue(0) )){
                    theHash += characteristic.getStringValue(0);
                }
                checkCurrentHash.add(characteristic.getStringValue(0));
                if (theHash.length() > 63) {
                    statusTrip="unlocked";
                    getHashDB(theHash);
                }
            }


        }
    };

    // BTLE device scanning callback.
    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        // Called when a device is found.
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

            if (statusTrip.equals("locked")) {
                if (!searchedList.contains(bluetoothDevice)) {
                    searchedList.add(bluetoothDevice);
                    // Check if the device has the UART service.
                    if (parseUUIDs(bytes).contains(UART_UUID)) {
                        // Found a device, stop the scan.
                        adapter.stopLeScan(scanCallback);
                        lockDeviceAddress = bluetoothDevice.getAddress();
                        // Connect to the device.
                        // Control flow will now go to the callback functions when BTLE events occur.
                        gatt = bluetoothDevice.connectGatt(getApplicationContext(), true, callback);
                    }
                }
            }
            if (statusTrip.equals("unlocked")) {
                if (!searchedList.contains(bluetoothDevice)) {
                    searchedList.add(bluetoothDevice);
                    if (bluetoothDevice.getAddress().equals(lockDeviceAddress)) {

                        Log.d("Mainpage", "address of this lock is same as the one unlocked");
                        lockIsNear = true;
                    } else {
                        Log.d("Mainpage", "Checking is this the parking lot: " + bluetoothDevice.getAddress());
                        if (parseUUIDs(bytes).contains(UART_UUID)) {
                            adapter.stopLeScan(scanCallback);
                            parkingAddress = bluetoothDevice.getAddress();
                            getParkingDB(bluetoothDevice.getAddress(), bluetoothDevice);
                            // Found a device
                            Log.d("Mainpage", "Found UART service!");
                            Log.d("Mainpage", "device address:" + bluetoothDevice.getAddress());
                        }
                    }
                    if (lockIsNear && parkingIsNear) {
                        adapter.stopLeScan(scanCallback);
                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Mainpage", "Scanning for devices...");
    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        super.onStop();
        completeTrip();
        if (gatt != null) {
            // For better reliability be careful to disconnect and close the connection.
//            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
        }
    }


    public void getBikeId() {
        // send z to get the id, plus the password_num from server
        String message = "z";
        if (tx == null || message == null || message.isEmpty()) {
            // Do nothing if there is no device or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            Log.d("Mainpage", "Sent: " + message);
        } else {
            Log.d("Mainpage", "Couldn't write TX characteristic!");
        }
    }

    public void getBikePassword(String password_num) {
        // send the password_num from server
        Log.d("Mainpage", "passwordnum: " + password_num);
        String message = password_num;
        pw_num = password_num;
        if (tx == null || message == null || message.isEmpty()) {
            // Do nothing if there is no device or message to send.
            Log.d("Mainpage", "Nothing to send");
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        Log.d("Mainpage", "going to write tx");
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            Log.d("Mainpage", "Sent: " + message);
        } else {
            Log.d("Mainpage", "Couldn't write TX characteristic!");
        }
    }

    public void getParkingHash() {
        String message = "h";
        if (tx == null || message == null || message.isEmpty()) {
            // Do nothing if there is no device or message to send.
            Log.d("Mainpage", "Nothing to send");
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        Log.d("Mainpage", "going to write tx");
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            Log.d("Mainpage", "Sent: " + message);
        } else {
            Log.d("Mainpage", "Couldn't write TX characteristic!");
        }
    }

    public void verifyBikePw(String bikePass) {
        Log.d("Mainpage", "verifying password...");
        String[] parts = passwordList.split(":");
        String verify = parts[(Integer.parseInt(pw_num) + 1)];
        Log.d("Mainpage", "verify value:" + verify);
        Log.d("Mainpage", "Bikepass value:" + bikePass);
        if (verify.equals(bikePass)) {
            Log.d("Mainpage", "unlocked the bike!");
            statusTrip = "unlocked";
            gatt.disconnect();
            stop1 = false;
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Unlocked", duration).show();
                    trip_view.setVisibility(View.VISIBLE);
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    unlock_bike.setText("Lock bike");
                    searchedList = new ArrayList<BluetoothDevice>();
                }
            });

            Timer t = new Timer();
            //Set the schedule function and rate
            t.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            trip_view.setText("Trip time:"+String.valueOf(minutes)+":"+String.valueOf(seconds));
                            seconds += 1;

                            if(seconds == 60)
                            {
                                seconds=0;
                                minutes += 1;
                                trip_view.setText("Trip time:"+String.valueOf(minutes)+":"+String.valueOf(seconds));

                            }
                            if(stop1){
                                t.cancel();
                            }



                        }

                    });
                }

            }, 0, 1000);


        }
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }

    public void parkingValidity(String valid, BluetoothDevice bluetoothDevice) {
        Log.d("Mainpage", "The validity:" + valid);
        if (valid.equals("Valid")) {
            gatt = bluetoothDevice.connectGatt(getApplicationContext(), true, callback);
            parkingIsNear = true;
        } else {
            adapter.startLeScan(scanCallback);
        }
    }


    private void completeTrip() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        parkingIsNear = false;
        stop1 = true;
        searchedList = new ArrayList<BluetoothDevice>();
        checkCurrentHash = new ArrayList<String>();
        lockDeviceAddress = "";
        pw_num = "";
        theHash = "";
        thePassword = "";
        passwordList = "";
        id_bike = "";
        statusTrip = "locked";
        unlock_bike.setText("Unlock bike");
        seconds = 0;
        minutes = 0;
        trip_view.setVisibility(View.INVISIBLE);
        Toast.makeText(getApplicationContext(), "Locked", duration).show();
        gatt.disconnect();

        Thread someThread = new Thread() {

            @Override
            public void run() {
                Log.d("Mainpage", "Before thread");
                while (id_bike.equals("")) {
                    //wait for condition
                    try {

                        Log.d("Mainpage", "thread wait 5seconds");
                        Thread.sleep(3000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d("Mainpage", "thread sleep error");
                    }
                }

                // connect to database to get bike password
                while(id_bike.length() < 10){

                }
                getPasswordDB(id_bike);
            }

        };
        someThread.start();

    }

    private void getPasswordDB(String idBike) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url_post, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d("apple", "response:" + response);
                passwordList = response;
                getBikePassword(String.valueOf(response.charAt(0)));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("apple", "error:" + error);
            }

        }) {
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("getPassword", idBike);
                Log.d("apple", "reached here");
                return params;
            }
        };
        stringRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void getHashDB(String hash) {
        Log.d("Mainpage", "The hash value is:" + hash);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url_post, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d("Mainpage", "response for hash:" + response);
                if (response.equals("Correct")) {
                    adapter.stopLeScan(scanCallback);
                    completeTrip();
//                    if(lockIsNear != true){
//                        completeTrip();
//                    }
                }
                else{
                    //bomb
                    adapter.stopLeScan(scanCallback);
                    statusTrip = "unlocked";
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("apple", "error:" + error);
            }

        }) {
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("setHash", hash);
                params.put("", parkingAddress.replaceAll(":", "_"));

                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void getParkingDB(String idParking, BluetoothDevice bluetoothDevice) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url_post, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                parkingValidity(response, bluetoothDevice);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("err", "error:" + error);
            }

        }) {
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("checkParking", idParking.replaceAll(":", "_"));
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        unlock_bike = findViewById(R.id.button1);
        trip_view = (TextView) findViewById(R.id.status_trip);
        adapter = BluetoothAdapter.getDefaultAdapter();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        unlock_bike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (unlock_bike.getText().toString().equals("Unlock bike")) {
                    findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                    if(gatt != null)
                        gatt.disconnect();
                    adapter.stopLeScan(scanCallback);
                    adapter.startLeScan(scanCallback);
                } else if (unlock_bike.getText().toString().equals("Lock bike")) {
                    findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                    adapter.stopLeScan(scanCallback);
                    adapter.startLeScan(scanCallback);

                }
            }
        });

        Thread someThread = new Thread() {

            @Override
            public void run() {
                while (id_bike.equals("")) {
                    //wait for condition
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while(id_bike.length() < 10){

                }
                // connect to database to get bike password
                getPasswordDB(id_bike);

            }

        };
        someThread.start();


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
        for (int i = 0; i<lati.length; i++){
            MarkerOptions pl = new MarkerOptions();
            pl.position(new LatLng(lati[i],longti[i] ));
            pl.title("parking lot");
            pl.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("p_icon",90,90)));
            mMap.addMarker(pl);
        }


        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {

                CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);

                mMap.moveCamera(center);
                mMap.animateCamera(zoom);

            }
        });
    }

    public Bitmap resizeMapIcons(String iconName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * <p>
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
}