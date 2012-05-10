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
import android.util.Log;
import android.widget.TextView;

public class PointerActivity extends Activity {
	public static final String tag = "pointer";

	private InetAddress HOST;
	private final int PORT = 50003;

	private SensorManager sm;
	private Sensor rv;
	private RotationVectorListener rv_sel;

	private DatagramSocket ds;
	private byte[] msg = new byte[8 + 3 * 4];
	private DatagramPacket dp = new DatagramPacket(msg, msg.length);

	private Thread nt;
	private boolean end_nt;

	private TextView info;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		info = (TextView) findViewById(R.id.info);

		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		// TODO provide support for gyroscope (rotation vector is flawed in early
		// versions of android)
		rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		// network thread
		nt = new Thread(new Runnable() {
			@Override
			public void run() {
				long lt = 0;
				// setup socket
				try {
					// TODO select host
					// TODO support bluetooth TCP socket
//					HOST = InetAddress.getByName("10.150.9.25");
					HOST = InetAddress.getByName("192.168.1.11");
					ds = new DatagramSocket();
					// InetAddress ia = InetAddress.getByName("192.168.1.255");
					// ds.setBroadcast(true);
					ds.connect(HOST, PORT);
					Log.d(tag,
							"Socket is bound to "
									+ String.valueOf(ds.getLocalPort()));
					info.setText("Running...");
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(tag, "Failed to make a socket.");
				}
				while (true) {
					if (end_nt) {
						Log.d(tag, "Network thread ends.");
						break;
					}
					
					long ct = rv_sel.getLatestTimestamp();
					if (ct > lt) {
						try {
							ds.send(dp);
							lt = ct;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, "UdpThread");

		// TODO solve hiccups due to GC
		// nt.setPriority(Thread.MAX_PRIORITY);
		nt.start();

		// TODO rewrite the sensor acquisition with NDK
		rv_sel = new RotationVectorListener();
	}

	class RotationVectorListener implements SensorEventListener {
		private long t = 0;
		private long mt = 0;
//		private byte[] buf = new byte[8 + 3 * 4];
//		private String info_text;
		
		public long getLatestTimestamp() {
			return t;
		}
		
		@Override
		public void onSensorChanged(SensorEvent se) {
			if (t == 0) {
				t = se.timestamp;
			}
			mt = (se.timestamp - t > mt) ? se.timestamp - t : mt;
//			 info_text = "timestamp: "+String.valueOf(se.timestamp)+'\n'
//			 + String.valueOf(se.values[0])+'\n'
//			 + String.valueOf(se.values[1])+'\n'
//			 + String.valueOf(se.values[2])+'\n'
//			 + "magnitude: " + String.valueOf(magnitude(se.values))+'\n'
//			 + "update rate: "+ String.valueOf(1e9 / (se.timestamp - t))+"Hz\n"
//			 + "maximum wait time: "+String.valueOf(mt/1e6)+"ms\n";

//			 info.setText(info_text);
//			 Log.d(tag, sb.toString());

			try {
				// package the sensor event
				packageSensorEvent(se, dp);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			t = se.timestamp;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}
	}
	
	@Override
	public void onResume() {
		super.onResume();

		sm.registerListener(rv_sel, rv, SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	public void onPause() {
		super.onPause();

		sm.unregisterListener(rv_sel);
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

	protected void packageSensorEvent(SensorEvent se, DatagramPacket packet) {
		byte[] buf = packet.getData();
		writeByteBuffer(buf, 0, se.timestamp);
		writeByteBuffer(buf, 8, se.values[0]);
		writeByteBuffer(buf, 12, se.values[1]);
		writeByteBuffer(buf, 16, se.values[2]);
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
}