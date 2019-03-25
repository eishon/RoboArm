package eishon.roboarm;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.R.integer;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{
	
	private static final String TAG = "ROBO_ARM";

	Button btn_start, btn_test,btn_stop,btn_reset;
	static TextView txt_report;
	
	int resultChk=0;

	private static final int REQUEST_ENABLE_BT = 0;
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
	private static final UUID MY_UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String address = "98:D3:31:40:17:09";
	
	DataThread dataThread;
	
	private static float gX,gY,gZ,proX;
	private static int grnd,arm_1,arm_2,grip;
	private Sensor mASensor,mPSensor;
	private SensorManager mSensorManager;
	private SensorEventListener mSensorEventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "In onCreate()");
		
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		setContentView(R.layout.activity_main);
		
		btn_start=(Button) findViewById(R.id.btn_start);
		btn_stop=(Button) findViewById(R.id.btn_stop);
		btn_test=(Button) findViewById(R.id.btn_test);
		btn_reset=(Button) findViewById(R.id.btn_reset);
		
		txt_report=(TextView) findViewById(R.id.txt_report);
		
		btAdapter=BluetoothAdapter.getDefaultAdapter();
		
		checkBT();
		initializeSensors();
		
		btn_start.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dataThread=new DataThread(btSocket,"10");
				dataThread.start();
				report("connnect clicked");
				report("flag- "+dataThread.threadFlag);
				txt_report.setText("Started");
			}
		});
		btn_stop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dataThread.stopDataThread();
				report("flag- "+dataThread.threadFlag);
				txt_report.setText("Ended");
			}
		});
		btn_test.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sendData("90\n180\n30\n115\n");
				txt_report.setText("Test Data Sent");
			}
		});
		btn_reset.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/*dataThread.stopDataThread();
				stopSensors();
				closeBT();
				connectBT();
				startSensors();*/
				txt_report.setText("Restarted");
			}
		});
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		connectBT();
	}
	

	@Override
	protected void onDestroy() {
		closeBT();
		stopSensors();
		super.onDestroy();
	}
	
	
	public void checkBT() {
		  if(btAdapter==null) { 
		    errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
		  } else {
		    if (btAdapter.isEnabled()) {
		  	  report("Bluetooth On");
		      Log.d(TAG, "...Bluetooth is enabled...");
		    } else {
		      Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
		      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		    }
		  }
		}
	
	private void connectBT(){
		Log.d(TAG, "...In connectBT() : Attempting client connect...");

		  BluetoothDevice device = btAdapter.getRemoteDevice(address);

		  try {
		    btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		    report("Socket-"+MY_UUID);
		  } catch (IOException e) {
		    errorExit("Fatal Error", "In connectBT() : socket create failed: " + e.getMessage() + ".");
		  }
		  
		  btAdapter.cancelDiscovery();
		  
		  Log.d(TAG, "...Connecting to Remote...");
		  
		  try {
		    btSocket.connect();
		    report("Connected to - "+address);
		    Log.d(TAG, "...Connection established and data link opened...");
		  } catch (IOException e) {
		    try {
		      btSocket.close();
		      report("Socket Closed due to IO");
		    } catch (IOException e2) {
		      errorExit("Fatal Error", "In connectBT() : unable to close socket during connection failure" + e2.getMessage() + ".");
		    }
		  }
		  
		  Log.d(TAG, "...Creating Socket...");

		  try {
		    outStream = btSocket.getOutputStream();
		  } catch (IOException e) {
		    errorExit("Fatal Error", "In connectBT() : output stream creation failed:" + e.getMessage() + ".");
		  }
		
	}
	
	private void closeBT(){
		Log.d(TAG, "...In closeBT()...");

		  if (outStream != null) {
		    try {
		      outStream.flush();
		      report("Outstream Flushed");
		    } catch (IOException e) {
		      errorExit("Fatal Error", "In closeBT() and failed to flush output stream: " + e.getMessage() + ".");
		    }
		  }

		  try     {
		    btSocket.close();
		    report("Socket Closed");
		  } catch (IOException e2) {
		    errorExit("Fatal Error", "In closeBT() and failed to close socket." + e2.getMessage() + ".");
		  }
	}


		public void errorExit(String title, String message){
		  Toast msg = Toast.makeText(getBaseContext(),
		      title + " - " + message, Toast.LENGTH_SHORT);
		  msg.show();
		  finish();
		}
		
		public void dataProcessing(float x,float y,float z){
			
			grnd=Math.round(18*x);  // for value (10*0.9)
			grnd=90-grnd;
			
			arm_1=Math.round(18*y);
			arm_1=120+arm_1;
			arm_2=30+(180-arm_1);
			
			if (z>-5.0) {
				resultChk=resultChk+1;
				arm_2=arm_2+5*resultChk;
			}else if (z<-15.0) {
				resultChk=resultChk-1;
				arm_2=arm_2+5*resultChk;
			}
			
		}
		
		public int pDataProcessing(float data){
			int result;
			
			if (data==0.00) {
				result=115;
			}else {
				result=175;
			}
			
			return result;
		}
		
		public void report(String message){
			  Toast msg = Toast.makeText(getBaseContext(),
			      message, Toast.LENGTH_SHORT);
			  msg.show();
			}

		public void sendData(String message){
		  byte[] msgBuffer = message.getBytes();

		  Log.d(TAG, "...Sending data: " + message + "...");
		  try {  
			    outStream.write(msgBuffer);
			    report("sending - "+message);
			  } catch (IOException e) {
			    String msg = "In sendData() and an exception occurred during write: " + e.getMessage();			    
			    errorExit("Fatal Error", msg);       
			  }

		  
		}
		
		private void initializeSensors(){
			mSensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
			mSensorEventListener=new SensorEventListener() {
				
				@Override
				public void onSensorChanged(SensorEvent event) {
					if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
						gX=event.values[0];
						gY=event.values[1];
						gZ=event.values[2];
						dataProcessing(gX, gY, gZ);
					}else if (event.sensor.getType()==Sensor.TYPE_PROXIMITY) {
						proX=event.values[0];
						grip=pDataProcessing(proX);
					}
					
				}
				
				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					
				}
			};
			mASensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			mPSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			startSensors();
		}
		
		
		private void startSensors() {
			mSensorManager.registerListener(mSensorEventListener, mASensor, SensorManager.SENSOR_DELAY_NORMAL);
			mSensorManager.registerListener(mSensorEventListener, mPSensor, SensorManager.SENSOR_DELAY_NORMAL);
			report("Sensor Started");
		}
		
		private void stopSensors() {
			mSensorManager.unregisterListener(mSensorEventListener);
			report("Sensor Stopped");
			
		}


		private class DataThread extends Thread {
			
			boolean threadFlag=false;
			int value;
			String msg;
			
			BluetoothSocket mSocket;
		    OutputStream mOutStream;

			public DataThread(BluetoothSocket socket,String msg) {
				mSocket = socket;
		        OutputStream tmpOut = null;
				this.msg = msg;
				value=Integer.parseInt(msg);
				try {
		            tmpOut = mSocket.getOutputStream();
		        } catch (IOException e) { }
		 
		        mOutStream = tmpOut;
			}

			@Override
			public void run() {
				threadFlag=true;
				while(threadFlag){
					try {
						msg=grnd+"\n"+arm_1+"\n"+arm_2+"\n"+grip+"\n";
						byte[] msgBuffer = msg.getBytes();
						mOutStream.write(msgBuffer);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					try {
						sleep(300);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			}
			
			void stopDataThread(){
				threadFlag=false;
			}
			
		}
	
	
}