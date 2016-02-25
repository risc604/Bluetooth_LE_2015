package com.tomcat.simple.bluetooth_le2015;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;



@TargetApi(21)
public class MainActivity extends AppCompatActivity
{
    private BluetoothAdapter    mBluetoothAdapter;
    private int                 REQUEST_ENABLE_BT = 1;
    private Handler             mHandler;
    private static final long   SCAN_PERIOD = 30000;    // 30s
    private BluetoothLeScanner  mLEScanner;
    private ScanSettings        settings;
    private List<ScanFilter>    filters;
    private BluetoothGatt       mGatt;

    private final int           API_LEVEL = 21;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userInterface();
        userControl();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled()))
        {
            Intent  enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            if (Build.VERSION.SDK_INT >= API_LEVEL)
            {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if ((mBluetoothAdapter == null) && mBluetoothAdapter.isEnabled())
        {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (mGatt == null)
        {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (requestCode == Activity.RESULT_CANCELED)
            {
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (Build.VERSION.SDK_INT < API_LEVEL)
                    {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                    else
                    {
                        mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            if (Build.VERSION.SDK_INT < API_LEVEL)
            {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT < API_LEVEL)
            {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    // API < 21 that BLE scan device CallBack function.
    private ScanCallback    mScanCallback = new ScanCallback()
    {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered. Could be one of
         *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
            //super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            for (ScanResult sr : results)
            {
                Log.i("ScanResult - Results", sr.toString());
            }
            //super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            Log.e("Scan Fialed", "Error Code" + errorCode);
            //super.onScanFailed(errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback()
            {
                /**
                 * Callback reporting an LE device found during a device scan initiated
                 * by the {@link BluetoothAdapter#startLeScan} function.
                 *
                 * @param device     Identifies the remote device
                 * @param rssi       The RSSI value for the remote device as reported by the
                 *                   Bluetooth hardware. 0 if no RSSI value is available.
                 * @param scanRecord The content of the advertisement record offered by
                 */
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device)
    {
        if (mGatt == null)
        {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback()
    {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt     GATT client
         * @param status   Status of the connect or disconnect operation.
         *                 {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState Returns the new connection state. Can be one of
         *                 {@link BluetoothProfile#STATE_DISCONNECTED} or
         *                 {@link BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;

                default:
                    Log.e("gattCallback", "STATE_OTHER");
                    break;
            }
            //super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            List<BluetoothGattService>  services = gatt.getServices();

            Log.i("onServicesDiscovered()", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
            //super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            Log.i("onCharacteristicRead()", characteristic.toString());
            gatt.disconnect();
            //super.onCharacteristicRead(gatt, characteristic, status);
        }
    };

    protected void userInterface()
    {

    }

    protected void userControl()
    {
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE Not Supported !", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager  bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }
}
