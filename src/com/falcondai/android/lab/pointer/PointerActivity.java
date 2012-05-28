package com.falcondai.android.lab.pointer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class PointerActivity extends Activity {
	public static final String tag = "pointer";

	private InetAddress HOST;
	private final int PORT = 50003;

	private SensorManager sm;
	private Sensor gyro;
	private RotationVectorListener rv_sel;
	private float[] rv = { 0.0f, 0.0f, 0.0f };

	private DatagramSocket ds;
	private byte[] msg = new byte[8 + 3 * 4];
	private DatagramPacket dp = new DatagramPacket(msg, msg.length);
	private long ct = 0L;

	private PowerManager pm;
	private WakeLock wl;
	
	private Thread nt;
	private boolean end_nt;
	private boolean init_host = true;

	private EditText host_text;
	
	private InputMethodManager imm;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		host_text = (EditText) findViewById(R.id.host_text);
		
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		// TODO provide support for gyroscope (rotation vector is flawed in early
		// versions of android)
		gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		pm = (PowerManager) getSystemService(POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, tag);
		
		imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		
		// network thread
		nt = new Thread(new Runnable() {
			@Override
			public void run() {
				long lt = 0;
				try {
					ds = new DatagramSocket();
				} catch (Exception e) {
					Log.e(tag, "Failed to make a datagram socket.");
					e.printStackTrace();
				}
				while (true) {
					if (init_host) {
						// setup socket
						try {
							// TODO select host
							
							// TODO support bluetooth TCP socket
							
							HOST = InetAddress.getByName(host_text.getText().toString());
							dp.setAddress(HOST);
							dp.setPort(PORT);
							Log.d(tag,
									"Socket is bound to "
											+ String.valueOf(ds.getLocalPort()));

							Log.d(tag, "Connected to "+HOST.getHostName());
						} catch (Exception e) {
							e.printStackTrace();
							Log.d(tag, "Failed to connect to "+host_text.getText().toString());
						}
						
						init_host = false;
						packageOrientationResetEvent(dp);
					}
					
					if (end_nt) {
						// end network thread
						ds.close();
						Log.d(tag, "Network thread ends.");
						break;
					}
					
					if (ct > lt) {
						// there is a new packet
						try {
							ds.send(dp);
							lt = ct;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, "NetworkThread");

		// nt.setPriority(Thread.MAX_PRIORITY);
		nt.start();

		// TODO rewrite the sensor acquisition with NDK
		rv_sel = new RotationVectorListener();
		
		// register mouse buttons' listeners
		((Button) findViewById(R.id.lmb)).setOnTouchListener(
				new OnTouchListener() {
					public boolean onTouch(View v, MotionEvent me) {
						if (me.getActionMasked() == MotionEvent.ACTION_DOWN) {
							packageMouseButtonEvent(0, 0, dp);
						} else if (me.getActionMasked() == MotionEvent.ACTION_UP) {
							packageMouseButtonEvent(0, 1, dp);
						}
						return false;
					}
				});
		
		((Button) findViewById(R.id.mmb)).setOnTouchListener(
				new OnTouchListener() {
					public boolean onTouch(View v, MotionEvent me) {
						if (me.getActionMasked() == MotionEvent.ACTION_DOWN) {
							packageMouseButtonEvent(1, 0, dp);
						} else if (me.getActionMasked() == MotionEvent.ACTION_UP) {
							packageMouseButtonEvent(1, 1, dp);
						}
						return false;
					}
				});
		
		((Button) findViewById(R.id.rmb)).setOnTouchListener(
				new OnTouchListener() {
					public boolean onTouch(View v, MotionEvent me) {
						if (me.getActionMasked() == MotionEvent.ACTION_DOWN) {
							packageMouseButtonEvent(2, 0, dp);
						} else if (me.getActionMasked() == MotionEvent.ACTION_UP) {
							packageMouseButtonEvent(2, 1, dp);
						}
						return false;
					}
				});
		
		// host address change
		((Button) findViewById(R.id.connect_btn)).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						rv[0] = 0.0f; rv[1] = 0.0f; rv[2] = 0.0f;
						init_host = true;
						imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				});
	}

	class RotationVectorListener implements SensorEventListener {
		private long t = 0;
		private double dt = 0;
//		private long mt = 0;
		private float mag;
		private float sin;
		private float[] arv = new float[3];
		private float[] brv = new float[3];
		private final float EPSILON = (float) 1.0e-18;
//		private String info_text;
		
		public long getLatestTimestamp() {
			return t;
		}
		
		@Override
		public void onSensorChanged(SensorEvent se) {
			if (t == 0) {
				t = se.timestamp;
			} else {
	//			mt = (se.timestamp - t > mt) ? se.timestamp - t : mt;
	//			info_text = "timestamp: "+String.valueOf(se.timestamp)+'\n'
	//					+"sys timestamp: "+String.valueOf(System.nanoTime())+'\n'
	//					+ String.valueOf(se.values[0])+'\n'
	//					+ String.valueOf(se.values[1])+'\n'
	//					+ String.valueOf(se.values[2])+'\n'
	//					+ "magnitude: " + String.valueOf(magnitude(se.values))+'\n'
	//					+ "update rate: "+ String.valueOf(1e9 / (se.timestamp - t))+"Hz\n"
	//					+ "maximum wait time: "+String.valueOf(mt/1e6)+"ms\n";
	//			info.setText(info_text);
	//			Log.d(tag, sb.toString());
				
				dt = (se.timestamp - t) / 1.0e9;
				mag = (float) Math.sqrt(se.values[0]*se.values[0]+se.values[1]*se.values[1]+se.values[2]*se.values[2]);
				if (mag > EPSILON) {
					arv[0] = se.values[0] / mag; arv[1] = se.values[1] / mag; arv[2] = se.values[2] / mag;
					sin = (float) Math.sin(mag * dt / 2.0f);
					arv[0] *= sin; arv[1] *= sin; arv[2] *= sin;
					brv[0] = rv[0]; brv[1] = rv[1]; brv[2] = rv[2];
					uqmultiply(arv, brv, rv);
	
					try {
						// package the sensor event
						packageSensorEvent(se.timestamp, rv, dp);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				t = se.timestamp;
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	}
	
	@Override
	public void onResume() {
		super.onResume();

		// reset orientation
		sm.registerListener(rv_sel, gyro, SensorManager.SENSOR_DELAY_FASTEST);
		wl.acquire();
	}

	@Override
	public void onPause() {
		super.onPause();

		sm.unregisterListener(rv_sel);
		wl.release();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (nt.isAlive()) {
			end_nt = true;
		}
	}

	protected float magnitude(float[] values) {
		double s = 0.0;
		for (int i = 0; i < values.length; i++) {
			s += values[i] * values[i];
		}
		return (float) Math.sqrt(s);
	}

	protected void packageSensorEvent(long timestamp, float[] rv, DatagramPacket packet) {
		byte[] buf = packet.getData();
		writeByteBuffer(buf, 0, timestamp);
		writeByteBuffer(buf, 8, rv[0]);
		writeByteBuffer(buf, 12, rv[1]);
		writeByteBuffer(buf, 16, rv[2]);
		
		ct = timestamp;
	}

	protected void packageMouseButtonEvent(int button, int state, DatagramPacket packet) {
		byte[] buf = packet.getData();
		long timestamp = System.nanoTime();
		writeByteBuffer(buf, 0, timestamp);
		buf[8] = 0x00; buf[9] = 0x00; buf[10] = 0x00; buf[11] = 0x40;
		writeByteBuffer(buf, 12, button);
		writeByteBuffer(buf, 16, state);
		
		ct = timestamp;
	}
	
	protected void packageOrientationResetEvent(DatagramPacket packet) {
		byte[] buf = packet.getData();
		long timestamp = System.nanoTime();
		writeByteBuffer(buf, 0, timestamp);
		buf[8] = 0x00; buf[9] = 0x00; buf[10] = 0x40; buf[11] = 0x40;
		
		ct = timestamp;
	}
	
	protected void writeByteBuffer(byte[] buf, int offset, long n) {
		if (offset + 8 > buf.length) {
			// the buffer is not big enough for the data
			// TODO throws an exception
			return;
		}

		for (int i = 0; i < 8; i++) {
			buf[offset + i] = (byte) ((n >>> i * 8) & 0xff);
		}
	}
	
	protected void writeByteBuffer(byte[] buf, int offset, int n) {
		if (offset + 4 > buf.length) {
			// the buffer is not big enough for the data
			// TODO throws an exception
			return;
		}

		for (int i = 0; i < 4; i++) {
			buf[offset + i] = (byte) ((n >>> i * 8) & 0xff);
		}
	}

	protected void writeByteBuffer(byte[] buf, int offset, float f) {
		if (offset + 4 > buf.length) {
			// the buffer is not big enough for the data
			// TODO throws an exception
			return;
		}

		int n = Float.floatToRawIntBits(f);
		for (int i = 0; i < 4; i++) {
			buf[offset + i] = (byte) ((n >>> i * 8) & 0xff);
		}
	}
	
	// unit quaternion multiplication 
	protected void uqmultiply(float[] a, float[] b, float[] c) {
		float w1 = (float) Math.sqrt(1.0f - a[0]*a[0] - a[1]*a[1] - a[2]*a[2]);
		float w2 = (float) Math.sqrt(1.0f - b[0]*b[0] - b[1]*b[1] - b[2]*b[2]);
		
		c[0] = w1 * b[0] + w2 * a[0] + a[1] * b[2] - a[2] * b[1];
		c[1] = w1 * b[1] + w2 * a[1] + a[2] * b[0] - a[0] * b[2];
		c[2] = w1 * b[2] + w2 * a[2] + a[0] * b[1] - a[1] * b[0];
	}
}