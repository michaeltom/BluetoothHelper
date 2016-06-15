package net.flyget.bluetoothhelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private static final int REQUEST_ENABLE_BT = 0;
	private static final int REQUEST_CONNECT_DEVICE = 1;

	private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	private final int HEX = 0;
	private final int DEC = 1;
	private final int ASCII = 2;

	private boolean isPause = false;

	private List<Integer> mBuffer;

	private int mCodeType = HEX;

	private static final String TAG = "MainActivity";
	private BluetoothAdapter mBluetoothAdapter;
	private ConnectThread mConnectThread;
	public ConnectedThread mConnectedThread;
	private Button mPauseBtn, mClearBtn, mScanBtn, mSendBtn;
	private TextView mTextView;
	private EditText mEditText;
	private static final int MSG_NEW_DATA = 3;
	private String mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the window layout
		// requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_main);
		mTitle = "Bluetooth Debug Helper";
		setTitle(mTitle);
		// note the user
		Toast t = Toast.makeText(this, R.string.note_str, 12);
		t.setGravity(Gravity.TOP, 0, 240);
		t.show();

		mPauseBtn = (Button) findViewById(R.id.pauseBtn);
		mClearBtn = (Button) findViewById(R.id.clearBtn);
		mScanBtn = (Button) findViewById(R.id.scanBtn);
		mSendBtn = (Button) findViewById(R.id.sendBtn);
		mPauseBtn.setOnClickListener(this);
		mClearBtn.setOnClickListener(this);
		mScanBtn.setOnClickListener(this);
		mSendBtn.setOnClickListener(this);

		mTextView = (TextView) findViewById(R.id.mTextView);
		mEditText = (EditText) findViewById(R.id.mEditText);

		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.custom_title);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		mBuffer = new ArrayList<Integer>();

	}

	@Override
	public void onStart() {
		super.onStart();
		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		}

	}



	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case MSG_NEW_DATA:
				if (isPause) {
					break;
				} else {
					StringBuffer buf = new StringBuffer();
					synchronized (mBuffer) {
						if (mCodeType == ASCII) {
							for (int i : mBuffer) {
								buf.append((char) i);
								buf.append(' ');
							}
						} else if (mCodeType == HEX) {
							for (int i : mBuffer) {
								buf.append(Integer.toHexString(i));
								buf.append(' ');
							}
						} else {
							for (int i : mBuffer) {
								buf.append(i);
								buf.append(' ');
							}
						}
					}
					mTextView.setText(buf.toString());
				}

				break;

			default:
				break;
			}
		}

	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled Launch the DeviceListActivity to see
				// devices and do scan
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, "BT not enabled", Toast.LENGTH_SHORT)
						.show();
				return;
			}
			break;
		case REQUEST_CONNECT_DEVICE:
			if (resultCode != Activity.RESULT_OK) {
				return;
			} else {
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				connect(device);
			}
			break;
		}
	}

	public void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);
		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(UUID
						.fromString(SPP_UUID));
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mBluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {

				Log.e(TAG, "unable to connect() socket", e);
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"unable to close() socket during connection failure",
							e2);
				}
				return;
			}

			mConnectThread = null;

			// Start the connected thread
			// Start the thread to manage the connection and perform
			// transmissions
			mConnectedThread = new ConnectedThread(mmSocket);
			mConnectedThread.start();

		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[256];
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					synchronized (mBuffer) {
						for (int i = 0; i < bytes; i++) {
							mBuffer.add(buffer[i] & 0xFF);
						}
					}
					mHandler.sendEmptyMessage(MSG_NEW_DATA);
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		isBackCliecked = false;
		menu.add(0, 0, 0, "Hex");
		menu.add(0, 1, 0, "Dec");
		menu.add(0, 2, 0, "Ascii");
		menu.add(0, 3, 0, "About");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			mCodeType = HEX;
			setTitle(mTitle + " [ Hex data mode ]");
			mEditText.setHint("Send commands by Hex");
			mHandler.sendEmptyMessage(MSG_NEW_DATA);
			break;
		case 1:
			mCodeType = DEC;
			setTitle(mTitle + " [ Dec data mode ]");
			mEditText.setHint("Send commands by Dec");
			mHandler.sendEmptyMessage(MSG_NEW_DATA);
			break;
		case 2:
			mCodeType = ASCII;
			setTitle(mTitle + " [ ASCII data mode ]");
			mEditText.setHint("Send commands by ASCII");
			mHandler.sendEmptyMessage(MSG_NEW_DATA);
			break;
		case 3:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		}
		return true;
	}

	private boolean isBackCliecked = false;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isBackCliecked) {
				this.finish();
			} else {
				isBackCliecked = true;
				Toast t = Toast.makeText(this, "Press \'Back\' again to exit.",
						Toast.LENGTH_LONG);
				t.setGravity(Gravity.CENTER, 0, 0);
				t.show();
			}
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		isBackCliecked = false;
		if(v == mClearBtn){
			mBuffer.clear();
			mTextView.setText("");
		}else if(v == mScanBtn){
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
			// if(mConnectedThread != null) {mConnectedThread.cancel();
			// mConnectedThread = null;}
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		}else if(v == mPauseBtn){
			if (isPause) {
				isPause = false;
				mPauseBtn.setText("Pause");
			} else {
				isPause = true;
				mPauseBtn.setText("Resume");
			}
		}else if(v == mSendBtn){
			String input = mEditText.getText().toString().trim();
			if (input != null && !"".equals(input)) {
				String[] data = input.split(" ");
				byte[] tmp = new byte[data.length];
				switch (mCodeType) {
				case HEX:
				case ASCII:
					for (int i = 0; i < data.length; i++) {
						tmp[i] = (byte) Integer.parseInt(data[i], 16);
					}
					break;
				case DEC:
					for (int i = 0; i < data.length; i++) {
						tmp[i] = (byte) Integer.parseInt(data[i], 10);
					}
					break;

				}
				mConnectedThread.write(tmp);
			}
		}
	}

}
