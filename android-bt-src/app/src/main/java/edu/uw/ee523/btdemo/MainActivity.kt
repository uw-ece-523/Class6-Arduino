package edu.uw.ee523.btdemo

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import edu.uw.ee523.btdemo.databinding.ActivityMainBinding

// https://developer.android.com/guide/topics/connectivity/bluetooth/find-ble-devices

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_ENABLE_BT: Int = 42
    var bluetoothManager: BluetoothManager? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val handler = Handler()
    var bluetoothGatt: BluetoothGatt? = null

    private lateinit var binding: ActivityMainBinding
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private lateinit var myBtDeviceListAdapter: BluetoothDeviceListAdapter
    private lateinit var tracker: SelectionTracker<String>
    private var selectedDevice:BluetoothDevice? = null

    private lateinit var myBtGattListAdapter: GattListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Check permissions in the onCreate of your main Activity
        ActivityCompat.requestPermissions(this,
            arrayOf( Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 1)

        // Get Bluetooth stuff
        this.bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = this.bluetoothManager!!.getAdapter()
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        // Set up the UI
        myBtDeviceListAdapter = BluetoothDeviceListAdapter()
        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDevices.adapter = myBtDeviceListAdapter

        myBtGattListAdapter = GattListAdapter()
        binding.recyclerViewGatt.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewGatt.adapter = myBtGattListAdapter

        // Setup the selection tracker (more UI stuff)
        tracker = SelectionTracker.Builder(
            "mySelection",
            binding.recyclerViewDevices,
            MyItemKeyProvider(myBtDeviceListAdapter),
            MyItemDetailsLookup(binding.recyclerViewDevices),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectSingleAnything()
        ).build()

        // Set up the selection tracker to do something when something is selected
        myBtDeviceListAdapter.tracker = tracker
        tracker?.addObserver(
            object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                        myBtGattListAdapter.clearServices()
                    val items = tracker?.selection!!.size()
                    if (items > 0) {
                        binding.textViewStatus.text = "Something selected"
                        binding.buttonConnect.isEnabled = true
                        selectedDevice = myBtDeviceListAdapter.getDeviceFromAddress(
                            tracker?.selection.elementAt(0)
                        )
                    } else {
                        binding.textViewStatus.text = "Nothing selected"
                        binding.buttonConnect.isEnabled = false
                        selectedDevice = null
                    }
                }
            })

        // More UI setup-- now, make the buttons do something
        binding.buttonScan.setOnClickListener { scanLeDevice() }
        binding.buttonConnect.setOnClickListener { connectToDevice() }
        binding.buttonConnect.isEnabled = false
        binding.buttonDisconnect.isEnabled = false

        // Check that bluetooth is available
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling ActivityCompat#requestPermissions
                    return
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        // If we have no bluetooth, don't scan for BT devices
        if (bluetoothAdapter == null){
            binding.buttonScan.isEnabled = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                    "Got permissions",
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        if (!scanning) {
            binding.textViewStatus.text = "Scannning for LE devices"
            binding.buttonScan.text = "Scanning..."
            binding.buttonScan.isEnabled = false

            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(stopLeScan(), SCAN_PERIOD)
            // Start the scan
            scanning = true
            bluetoothLeScanner?.startScan(leScanCallback)
        } else {
            // Will hit here if we are already scanning
            binding.textViewStatus.text = "hmm..."
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopLeScan() = {
        scanning = false
        bluetoothLeScanner?.stopScan(leScanCallback)
        binding.textViewStatus.text = "Scan completed."
        binding.buttonScan.text = "Scan"
        binding.buttonScan.isEnabled = true
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            binding.textViewStatus.text = "Scan Result arrived: " + result.device.name
            myBtDeviceListAdapter.addDevice(result.device)
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                Log.i(TAG, "Starting service discovery")
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered: ")
                Log.i(TAG, gatt?.services.toString())
                displayGattServices(bluetoothGatt?.services)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Not doing anything with this right now
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        if (selectedDevice != null) {
            binding.textViewStatus.text = "Connecting to device: " + selectedDevice!!.name
            bluetoothAdapter?.let { adapter ->
                try {
                    val device = adapter.getRemoteDevice(selectedDevice!!.address)
                    // connect to the GATT server on the device
                    bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                    return
                } catch (exception: IllegalArgumentException) {
                    Log.w(TAG, "Device not found with provided address.  Unable to connect.")
                    return
                }
            } ?: run {
                Log.w(TAG, "BluetoothAdapter not initialized")
                return
            }
        }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        handler.post {
            myBtGattListAdapter.addServices(gattServices)
        }
    }

}