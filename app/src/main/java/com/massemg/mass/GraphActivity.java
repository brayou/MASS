package com.massemg.mass;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.snatik.storage.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

import me.aflak.bluetooth.Bluetooth;


public class GraphActivity extends AppCompatActivity {
    Bluetooth bluetooth = new Bluetooth(this);
    final String DEVICE_MAC = "00:06:66:F2:30:7D"; //ON DEVICE ITSELF --------------------------------------------------
    boolean graphMode = true;
    boolean connected = false, fetched = false, paired = false;
    BluetoothDevice device = null;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    int numPaired = 0;
    int delay = 2000; //2 seconds
    public BluetoothSocket socket;
    public InputStream inStream;
    public byte[] buffer;
    int count = 0;
    boolean btFlags = false;
    int position = 0;
    int bufPos = 0;
    //public ThreadPoolExecutor Executor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateGraph(savedInstanceState, graphMode);
    }

    public void onCreateGraph(Bundle savedInstanceState, boolean graphMode) {
        if (!graphMode) return;
        setContentView(R.layout.activity_graph);
        initGraph();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            // Device doesn't support Bluetooth
//        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 2105;
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //AcceptThread(mBluetoothAdapter);
        int BLUETOOTH_REQUEST = 0;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    BLUETOOTH_REQUEST);
        } else {
            if(btFlags) Log.d("myTag", "Bluetooth permission already granted.");
            super.onStart();
            bluetooth.onStart();
            bluetooth.enable();
            Log.d("myTag", "Connecting to Bluetooth...");
        }
//        paired = false;
//        fetched = false;
//        connected = false;
        //pairedDevices = mBluetoothAdapter.getBondedDevices();
        numPaired = pairedDevices.size();
        if(btFlags) Log.d("myTag", "Bluetooth: " + Integer.toString(numPaired) + " devices already paired.");
        boolean btTest = true;
        int[][] testArr = {{0, 0}, {1, 1}, {2, 2}};
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_YEAR);
        final String dayStr = String.valueOf(day);
        buffer = new byte[1024];
        for (int i = 0; i < 1024; i++) buffer[i] = (byte) ('+');
        if (!btTest) return;
        device = null;
        //Handler handler = new Handler();
        new btSetup().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dayStr);
//        bluetooth.setDeviceCallback(new DeviceCallback() {
//            @Override
//            public void onDeviceConnected(BluetoothDevice device) {
//                connected = true;
//                Log.d("myTag", "Bluetooth: SMiRF connected!!");
//            }
//
//            @Override
//            public void onDeviceDisconnected(BluetoothDevice device, String message) {
//            }
//
//            @Override
//            public void onMessage(String message) {
//                updateGraph(toData(message), false, dayStr, true);
//            }
//
//            @Override
//            public void onError(String message) {
//            }
//
//            @Override
//            public void onConnectError(BluetoothDevice device, String message) {
//            }
//        });
        //updateGraph(testArr,true, dayStr, true);
//        if(btTest) Log.d("myTag", "Bluetooth: btTest enabled.");
//        else Log.d("myTag", "Bluetooth: btTest disabled.");
    }

//   @Override
//    protected void onStart() {
//        super.onStart();
//    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetooth.onStop();
    }

    public void initGraph() {
        count = 0;
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScrollableY(true); // enables vertical scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getViewport().setScalableY(true);
        graph.getViewport().setYAxisBoundsManual(true);
        //graph.getViewport().setMinX(300);
        graph.getViewport().setMaxX(100);
        graph.getViewport().setMaxY(1024);

        graph.getGridLabelRenderer().setNumHorizontalLabels(1000);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Muscle Activation (mV)");
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        //graph.getViewport().setMinX(0);
        //graph.getViewport().setMaxX(40);
        graph.removeAllSeries();
        position = 0;
    }

    public void updateGraph(int[][] data, boolean fresh, String dayStr, boolean save) {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series;
        series = new LineGraphSeries<>();
        int offset = 0, x = 0;
        if (!fresh) {
            if (graph.getSeries().size() > 0) series = (LineGraphSeries) graph.getSeries().get(0);
            //Log.d("myTag", "Updating graph in Append mode.");

        } else {
            Log.d("myTag", "Updating graph in Fresh mode.");
            position = 0;
        }
        for (int i = 0; i < data.length; i++) {
            //Log.d("myTag", "Considering drawing point "+data[i][0] + ", " + data[i][1]);
//            if (data[i][0] < position) {
//                Log.d("myTag", "SKIPPED DATA POINT.");
//                continue;
//            }
            //offset = data[i][0] - position;
            //if data is behind graph
            //if(offset < 0)  x = data[i][0] - offset;
            //if data is ahead graph
            //x = data[i][0] - offset;
            //position = x + 1;
            //data[i][0] = x + 1;

            position++;
            data[i][0] = position;
            Log.d("myTag", "Graphing data point ["+data[i][0] + ", " + data[i][1]+"]");
            series.appendData(new DataPoint(data[i][0], data[i][1]), true, 10000);
        }
        graph.removeAllSeries();
        graph.addSeries(series);
        if (save) writeFile(data, dayStr);
    }

    public void writeFile(int[][] data, String dayStr) {
        Storage storage = new Storage(getApplicationContext());
        String path = storage.getInternalFilesDirectory();
        //String newDir = path + File.separator + "MASS";
        //storage.createDirectory(newDir, false);
        path = path + "/" + dayStr;

        //path = newDir + "/" + dayStr;
        if (!storage.isFileExist(path)) {
            storage.createFile(path, "");
        }
        storage.appendFile(path, toStr(data));
        //Log.d("myTag", "File: Appended following to " + path + ": " + toStr(data));
    }

    public String toStr(int[][] data) {
        String x = "";
        for (int i = 0; i < data.length; i++) {
            if (i != 0) x += ", ";
            x += Arrays.toString(data[i]);
        }
        return x;
    }

    public int[][] readFile(String str) {
        Storage storage = new Storage(getApplicationContext());
        String path = storage.getInternalFilesDirectory();
        //String newDir = path + File.separator + "MASS";
        //storage.createDirectory(newDir, false);
        path = path + "/" + str;
        //path = newDir + "/" + str;
        if (!storage.isFileExist(path)) {
            Log.d("myTag", "File " + path + " not found.");
            return new int[][]{{-1, -1}};
        }
        String dataStr = storage.readTextFile(path);
        Log.d("myTag", "File: Read following from " + path + ": " + dataStr);
        return toData(dataStr);
    }

    public int[][] toData(String str) {
        int len = 0;
        for (int j = 0; j < str.length(); j++) {
            char c = str.charAt(j);
            if (c == ']') len++;
        }
        //Log.d("myTag", "toData: Number of points is: " + Integer.toString(len));
        int[][] data = new int[len*2][2];

        int index = 0;
        boolean x = true, haveInt = false;
        int num = 0, place = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[' || c == ',' || c == ' ' || c == ']') {
                if (haveInt) {
                    //Log.d("myTag", "toData: number found at index "+index+": "+num+", x is "+x);
                    //Log.d("myTag", "writing to data[" + index + "][" + (x ? 0 : 1) + "]");
                    data[index][x ? 0 : 1] = num;
                    x = !x;
                    if (x == true) index++;
                    //Log.d("myTag", "toData: successful arr access");
                    haveInt = false;
                }
                num = 0;
                place = 0;
                continue;
            }
            num += Character.getNumericValue(c) * java.lang.Math.pow(10, place);
            place++;
            haveInt = true;
        }
        return data;
    }

    public class btSetup extends AsyncTask<String, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(GraphActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
            pdLoading.setMessage("\tConnecting...");
            pdLoading.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
            if(btFlags) Log.d("myTag", "Bluetooth: About to enter connecting thread.");
            //MY CODE-----
//          while (!connected) {
//                try {
//                    bluetooth.connectToName("MASS");
//                } catch (IllegalArgumentException e) {
//                    //Log.d("myTag", "Bluetooth: SMiRF connecting crashed.");
//                }
//            }
            //MY CODE ENDS----

            // I know I should be using an intent here...
            while (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
            pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice d : pairedDevices) {
                    if (d.getName().equals("MASS")) {
                        //Log.d("myTag", "Bluetooth: MAC is " + d.getAddress());
                        device = mBluetoothAdapter.getRemoteDevice(d.getAddress());
                    }
                }
            }
//            if (device == null) {
//                // BroadcastReceiver registered with IntentFilter(BluetoothDevice.ACTION_FOUND)
//                registerReceiver(mReceiver, find);
//                if (!bluetoothAdapter.isDiscovering()) {
//                    bluetoothAdapter.startDiscovery();
//                }
//            }
            while (device == null) {
            } // Wait for BroadcastReceiver to find the device and connect
            if(btFlags) Log.d("myTag", "Bluetooth: Broadcast Receiver found device");
            if (device != null) {
                // Create socket connection in a new thread
                BluetoothSocket tmp = null;
                String uuid = device.getUuids()[0].toString();
                try {
                    // Get a BluetoothSocket to connect with the given BluetoothDevice.
                    // MY_UUID is the app's UUID string, also used in the server code.
                    tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
                } catch (IOException e) {
                    Log.e("myTag", "Bluetooth: Socket's create() method failed", e);
                }
                socket = tmp;
//                Connect connection = new Connect();
//                new Thread(connection).start();
            }
            while (socket == null) {
            }// Wait for successfull socket connection
            if(btFlags) Log.d("myTag", "Bluetooth: Socket found.");
            if (socket != null) {
                mBluetoothAdapter.cancelDiscovery();
                // Get input/ouputstream
//                Communication communicate = new Communication();
//                new Thread(communicate).start();
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect();
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and return.
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        Log.e("myTag", "Bluetooth: Could not close the client socket", closeException);
                    }
                    return null;
                }
                if(btFlags) Log.d("myTag", "Bluetooth: Socket connected.");
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(mmSocket);
            }

            Log.d("myTag", "Bluetooth: SMiRF connected.");
            pdLoading.dismiss();
            InputStream tmpIn = null;
            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("myTag", "Bluetooth: Error occurred when creating input stream", e);
            }
            inStream = tmpIn;
            new readData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            new procData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //this method will be running on UI thread
        }
    }

    public class readData extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("myTag", "Thread1: Read thread started.");
            while (true) {
                String s = "";
                String newS = "";
                bufPos = 0;
                int inc = 0, start = 0;
                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    //Log.d("myTag","hey");
                    if (bufPos >= 1014) {
                        buffer = new byte[1024];
                        for (int i = 0; i < 1024; i++) buffer[i] = (byte) ('+');
                        Log.d("myTag", "Thread1: Refreshed buffer");
                        bufPos = 0;
                    }
                    try {
                        //Log.d("myTag","hey2");
                        // Read from the InputStream.
                        inc = inStream.read(buffer, bufPos, 9);
                        //Log.d("myTag","hey3");
                        if(bufPos == 0) start = 1;
                        else start = 0;
                        newS = new String(
                                Arrays.copyOfRange(buffer, bufPos-1+start, bufPos-1+inc)
                        );
                        bufPos += inc;
                        Log.d("myTag", "Thread1: Received " + newS + " from SMirF");
                        //s = newS.substring(0,8);
                        //Log.d("myTag", "BUFFER is of length "+len+" and is: "+ s);
                    } catch (IOException e) {
                        Log.d("myTag", "Thread1: Input stream was disconnected", e);
                        break;
                    }
                }
                break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //this method will be running on UI thread
        }
    }

    public class procData extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
        }

        @Override
        protected Void doInBackground(String... params) {
            Log.d("myTag", "Thread2: Processing thread started.");
            char c;
            String s = "";
            count = 0;
            int data[][] = {{10000, -1}};
            boolean x = true, haveInt = false;
            int num = 0, place = 0;
            String numb = "";
            while (true) {
                //Log.d("myTag", "Processing");
                for (int i = 0; i < buffer.length; i++) {

                    while(bufPos <= i) {} //Wait for the slower Read thread

                    c = (char) (buffer[i]);
                    if (c == '[' || c == ',' || c == ' ' || c == ']') {
                        //Log.d("myTag", "Found recognizable symbol");
                        if (haveInt) {
                            //Log.d("myTag", "toData: number found at index "+index+": "+num+", x is "+x);
                            //Log.d("myTag", "writing to data["+index+"]["+(x?0:1)+"]");
                            data[0][x ? 0 : 1] = Integer.parseInt(numb);
                            x = !x;
                            haveInt = false;
                            count++;
                            if (count == 2) {
                                Log.d("myTag", "Graphing [" + data[0][0] + "][" + data[0][1] + "]");
                                updateGraph(data, false, params[0], true);
                                count = 0;
                            }
                        }
//                        num = 0;
//                        place = 0;
                        numb = "";
                        continue;
                    } else if (c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9' || c == '0') {
//                        num += Character.getNumericValue(c) * java.lang.Math.pow(10, place);
                        numb += c;
                        //place++;
                        haveInt = true;
                    } else continue;
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //this method will be running on UI thread
        }
    }
//    public String convert(byte[] buffer) {
//        String s = "";
//        for(int i=0; i < buffer.length; i++) {
//            byte[] tmp = {buffer[i]};
//            Log.d("myTag", "Bluetooth: BYTE = " + tmp);
//            try {
//                s = s.concat(new String(tmp, "ASCII") );
//                //Log.d("myTag", "Bluetooth: String is " +s);
//
//            } catch (UnsupportedEncodingException e) {
//                Log.d("myTag", "Bluetooth: Wrong encoding");
//            }
//        }
//        return s;
//    }
//LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
//                new DataPoint(0, 1),
//                new DataPoint(1, 5),
//                new DataPoint(2, 3)
//        });
//
//ASYNC CODE FOR FETCHING AND PAIRING, BUT CAN DO WITH ANDROID GUI
//            Log.d("myTag", "Bluetooth: About to enter fetching while loop.");
//            while (!fetched) {
//                fetched = true;
//                try {
//                    device = mBluetoothAdapter.getRemoteDevice(DEVICE_MAC);
//                } catch (IllegalArgumentException e) {
//                    //Log.d("myTag", "Bluetooth: SMiRF not found.");
//                    fetched = false;
//                }
//            }
//            Log.d("myTag", "Bluetooth: About to enter pairing while loop.");
//            while(!paired) {
//                paired = true;
//                try {
//                    bluetooth.pair(device);
//                } catch (IllegalArgumentException e) {
//                    //Log.d("myTag", "Bluetooth: Failed SMIRF pairing.");
//                }
//                if (pairedDevices.size() <= numPaired) paired = false; //Keep trying to pair
//                // There are paired devices, assume it's ours and connect
//            }
//
//USING HANDLERS INSTEAD OF ASYNC
//handler.postDelayed(new Runnable() {
//    public void run() {
//        paired = true;
//        try {
//            bluetooth.pair(device);
//        } catch (IllegalArgumentException e) {
//            //Log.d("myTag", "Bluetooth: Failed SMIRF pairing.");
//        }
//        if (pairedDevices.size() <= numPaired) paired = false; //Keep trying to pair
//        // There are paired devices, assume it's ours and connect
//    }
//}, delay);
//
//BLUETOOTH PAIRING FROM ONLINE, SUPER OLD
//Log.d("myTag", "Bluetooth: Successful SMiRF pair.");
//                for (BluetoothDevice d : pairedDevices) {
//                    //String deviceName = d.getName();
//                    String deviceMAC = d.getAddress(); // MAC address
//                    Log.d("myTag", "Device with address" + deviceMAC + " paired.");
//                    //Try and connect to our BlueSMiRF
//                    if (deviceMAC == DEVICE_MAC) {
//                        paired = true;
//                          break;
//                    }
//                }
}