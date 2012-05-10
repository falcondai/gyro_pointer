package com.falcondai.android.lab.pointer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
	private SensorEventListener rv_sel;

	private DatagramSocket ds;
	private DatagramPacket dp = null;

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
		rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		// network thread
		nt = new Thread(new Runnable() {
			@Override
			public void run() {
				// setup socket
				try {
					// TODO select host
					HOST = InetAddress.getByName("10.150.9.25");
					// HOST = InetAddress.getByName("192.168.1.11");
					ds = new DatagramSocket();
					// InetAddress ia = InetAddress.getByName("192.168.1.255");
					// ds.setBroadcast(true);
					ds.connect(HOST, PORT);
					Log.d(tag,
							"Socket is bound to "
									+ String.valueOf(ds.getLocalPort()));
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(tag, "Failed to make a socket.");
				}
				while (true) {
					if (end_nt) {
						Log.d(tag, "Network thread ends.");
						break;
					}

					if (dp != null) {
						try {
							ds.send(dp);
							dp = null;
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
		rv_sel = new SensorEventListener() {
			private long t = 0;
//			private StringBuilder sb = new StringBuilder();
			private long mt = 0;
			private byte[] buf = new byte[8+3*4];
			
			@Override
			public void onSensorChanged(SensorEvent se) {
//				sb.setLength(0);
//				sb.append("timestamp: ").append(se.timestamp).append('\n');
//				sb.append(se.values[0]).append('\n');
//				sb.append(se.values[1]).append('\n');
//				sb.append(se.values[2]).append('\n');
//				sb.append("magnitude: ").append(magnitude(se.values))
//						.append('\n');
//				sb.append("update rate: ").append(1e9 / (se.timestamp - t))
//						.append("Hz\n");
//				sb.append("maximnum wait: ").append(mt / 1e6).append("ms\n");
				if (t == 0) {
					t = se.timestamp;
				}
				mt = (se.timestamp - t > mt) ? se.timestamp - t : mt;
				// info_text = "timestamp: "+String.valueOf(se.timestamp)+'\n'
				// + String.valueOf(se.values[0])+'\n'
				// + String.valueOf(se.values[1])+'\n'
				// + String.valueOf(se.values[2])+'\n'
				// + "magnitude: " + String.valueOf(magnitude(se.values))+'\n'
				// + "update rate: "+ String.valueOf(1e9 / (se.timestamp - t));

				t = se.timestamp;

//				info.setText(sb.toString());
				// info.setText(info_text);
				// Log.d(tag, sb.toString());

				try {
					// package the sensor event
					dp = packageSensorEvent(se, buf);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub

			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

//		int x = Float.floatToRawIntBits(100.0f);
//		int y = 200;
//		long t = 1000L;
//		 byte[] buf = { (byte) ((x >>> 0) & 0xff), (byte) ((x >>> 8) & 0xff),
//		 (byte) ((x >>> 16) & 0xff), (byte) ((x >>> 24) & 0xff),
//		 (byte) ((y >>> 0) & 0xff), (byte) ((y >>> 8) & 0xff),
//		 (byte) ((y >>> 16) & 0xff), (byte) ((y >>> 24) & 0xff) };
//		byte[] buf = { (byte) ((t >>> 0) & 0xff), (byte) ((t >>> 8) & 0xff),
//				(byte) ((t >>> 16) & 0xff), (byte) ((t >>> 24) & 0xff),
//				(byte) ((t >>> 32) & 0xff), (byte) ((t >>> 40) & 0xff),
//				(byte) ((t >>> 48) & 0xff), (byte) ((t >>> 56) & 0xff), };
//		byte[] buf = new byte[16];
//		writeByteBuffer(buf, 0, 123456789L);
//		writeByteBuffer(buf, 8, 3.1415926f);
//		writeByteBuffer(buf, 12, 2.17f);
//		dp = new DatagramPacket(buf, buf.length);

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

	protected DatagramPacket packageSensorEvent(SensorEvent se, byte[] buf)
			throws UnknownHostException {

//		StringBuilder sb = new StringBuilder();
//		sb.append(se.timestamp).append(' ').append(se.values[0]).append(' ')
//				.append(se.values[1]).append(' ').append(se.values[2]);
//		byte[] buffer = sb.toString().getBytes();
		
		// TODO package the data more compactly
		writeByteBuffer(buf, 0, se.timestamp);
		writeByteBuffer(buf, 8, se.values[0]);
		writeByteBuffer(buf, 12, se.values[1]);
		writeByteBuffer(buf, 16, se.values[2]);

		return new DatagramPacket(buf, buf.length);
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