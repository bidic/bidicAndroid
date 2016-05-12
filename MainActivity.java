package asus4.wiigeeandroid;

import java.io.IOException;

import org.wiigee.control.AndroidWiigee;
import org.wiigee.control.Wiigee;
import org.wiigee.event.GestureEvent;
import org.wiigee.event.GestureListener;


import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import asus4.wiigeeandroid.R.id;

///////////////////////////////////////library achartengine////////////////////////////////////////////////////////////////////
import java.util.ArrayList;
import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
/////////////////////////////////////////library achartengine/////////////////////////////////////////////////////////////////////////////

public class MainActivity extends Activity implements SensorEventListener, OnClickListener {
	
	static final int _TRAIN_BUTTON = 0x01;
	static final int _SAVE_BUTTON = 0x02;
	static final int _RECOGNIZE_BUTTON = 0x03;
	
//////////////////////////////////////achartengine instances define////////////////////////////////////////////////////////////////
	private SensorManager sensorManager;
	private Button btnStart, btnStop, btnUpload;
	private boolean started = false;
	private ArrayList<AccelData> sensorData;
	private LinearLayout layout;
	private View mChart;
//////////////////////////////////////achartengine instances define////////////////////////////////////////////////////////////////
	
	AndroidWiigee wiigee;
	Logger logger;
	
	boolean isRecording;
	boolean isRecognizing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		layout = (LinearLayout) findViewById(R.id.chart_container); ////////////achartengine//////////////////////
		
		wiigee = new AndroidWiigee(this);
		logger = new Logger((TextView) findViewById(id.logText), 20);
		isRecording = false;
		isRecognizing = false;
		
		wiigee.setTrainButton(_TRAIN_BUTTON);
		wiigee.setCloseGestureButton(_SAVE_BUTTON);
		wiigee.setRecognitionButton(_RECOGNIZE_BUTTON);
		wiigee.addGestureListener(new GestureListener() {
			
			@Override
			public void gestureReceived(GestureEvent event) {
				logger.addLog("Recognized: "+event.getId()+" Probability: "+event.getProbability());
			}
		});

//////////////////////////////////////////////////////display graph/////////////////////////////////////////////////////////////////		
		//layout = (LinearLayout) findViewById(R.id.chart_container); 
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorData = new ArrayList<AccelData>();
		//btnStart = (Button) findViewById(R.id.btnStart);
		//btnStop = (Button) findViewById(R.id.btnStop);
		//btnUpload = (Button) findViewById(R.id.btnUpload);
		//btnStart.setOnClickListener(this);
		//btnStop.setOnClickListener(this);
		//btnUpload.setOnClickListener(this);
		//btnStart.setEnabled(true);
		//btnStop.setEnabled(false);
		//if (sensorData == null || sensorData.size() == 0) {
			//btnUpload.setEnabled(false);
		//}
//////////////////////////////////////////////////////display graph/////////////////////////////////////////////////////////////////
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		try {
			wiigee.getDevice().setAccelerationEnabled(true);
		}
		catch(IOException e) {
			
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		try {
			wiigee.getDevice().setAccelerationEnabled(false);			
		}
		catch(IOException e) {
			
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	// events from xml
	public void onRecordButtonClick(View view) {
		Button btn = (Button) view;
		if(isRecording) {
			btn.setText(R.string.button_train_start);
			isRecording = false;
			wiigee.getDevice().fireButtonReleasedEvent(_TRAIN_BUTTON);
			
			started = false;////////////achartengine//////////////////////
			sensorManager.unregisterListener(this);////////////achartengine//////////////////////
			layout.removeAllViews();////////////achartengine//////////////////////
			openChart();////////////achartengine//////////////////////
		}
		else {
			btn.setText(R.string.button_train_stop);
			isRecording = true;
			wiigee.getDevice().fireButtonPressedEvent(_TRAIN_BUTTON);
			
			sensorData = new ArrayList<AccelData>();////////////achartengine//////////////////////
			started = true;////////////achartengine//////////////////////
			Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);////////////achartengine//////////////////////
			sensorManager.registerListener(this, accel,SensorManager.SENSOR_DELAY_FASTEST);////////////achartengine//////////////////////
		}
		//logger.addLog("click:onRecord:"+isRecording);////////////display log//////////////////////
	}
	
	public void onSaveButtonClick(View view) {
		logger.addLog("click:onSave");
		wiigee.getDevice().fireButtonPressedEvent(_SAVE_BUTTON);
		wiigee.getDevice().fireButtonReleasedEvent(_SAVE_BUTTON);
	}
	
	public void onRecognizeButtonClick(View view) {
		Button btn = (Button) view;
		if(isRecognizing) {
			btn.setText(R.string.button_recognize_start);
			isRecognizing = false;
			wiigee.getDevice().fireButtonReleasedEvent(_RECOGNIZE_BUTTON);
			
			started = false;////////////achartengine//////////////////////
			sensorManager.unregisterListener(this);////////////achartengine//////////////////////
			layout.removeAllViews();////////////achartengine//////////////////////
			openChart();////////////achartengine//////////////////////
		}
		else {
			btn.setText(R.string.button_recognize_stop);
			isRecognizing = true;
			wiigee.getDevice().fireButtonPressedEvent(_RECOGNIZE_BUTTON);
			
			sensorData = new ArrayList<AccelData>();////////////achartengine//////////////////////
			started = true;////////////achartengine//////////////////////
			Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);////////////achartengine//////////////////////
			sensorManager.registerListener(this, accel,SensorManager.SENSOR_DELAY_FASTEST);////////////achartengine//////////////////////
		}
		logger.addLog("click:onRecognize");////////////display log//////////////////////
	}
	////////////////////////////////////////achartengine sensor function///////////////////////////////////////////////////////////
	public void onSensorChanged(SensorEvent event) {
		if (started) {
			double x = event.values[0];
			double y = event.values[1];
			double z = event.values[2];
			long timestamp = System.currentTimeMillis();
			AccelData data = new AccelData(timestamp, x, y, z);
			sensorData.add(data);
		}

	}
////////////////////////////////////////achartengine sensor function///////////////////////////////////////////////////////////

	////////////////////////////////////////achartengine method for openChart()/////////////////////////////////////////////////////
	private void openChart() {
		if (sensorData != null || sensorData.size() > 0) {
			long t = sensorData.get(0).getTimestamp();
			XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

			XYSeries xSeries = new XYSeries("X");
			XYSeries ySeries = new XYSeries("Y");
			XYSeries zSeries = new XYSeries("Z");

			for (AccelData data : sensorData) {
				xSeries.add(data.getTimestamp() - t, data.getX());
				ySeries.add(data.getTimestamp() - t, data.getY());
				zSeries.add(data.getTimestamp() - t, data.getZ());
			}

			dataset.addSeries(xSeries);
			dataset.addSeries(ySeries);
			dataset.addSeries(zSeries);

			XYSeriesRenderer xRenderer = new XYSeriesRenderer();
			xRenderer.setColor(Color.RED);
			xRenderer.setPointStyle(PointStyle.CIRCLE);
			xRenderer.setFillPoints(true);
			xRenderer.setLineWidth(1);
			xRenderer.setDisplayChartValues(false);

			XYSeriesRenderer yRenderer = new XYSeriesRenderer();
			yRenderer.setColor(Color.GREEN);
			yRenderer.setPointStyle(PointStyle.CIRCLE);
			yRenderer.setFillPoints(true);
			yRenderer.setLineWidth(1);
			yRenderer.setDisplayChartValues(false);

			XYSeriesRenderer zRenderer = new XYSeriesRenderer();
			zRenderer.setColor(Color.BLUE);
			zRenderer.setPointStyle(PointStyle.CIRCLE);
			zRenderer.setFillPoints(true);
			zRenderer.setLineWidth(1);
			zRenderer.setDisplayChartValues(false);

			XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
			multiRenderer.setXLabels(0);
			multiRenderer.setLabelsColor(Color.RED);
			multiRenderer.setChartTitle("t vs (x,y,z)");
			multiRenderer.setXTitle("Sensor Data");
			multiRenderer.setYTitle("Values of Acceleration");
			multiRenderer.setZoomButtonsVisible(true);
			for (int i = 0; i < sensorData.size(); i++) {
				
				multiRenderer.addXTextLabel(i + 1, ""
						+ (sensorData.get(i).getTimestamp() - t));
			}
			for (int i = 0; i < 12; i++) {
				multiRenderer.addYTextLabel(i + 1, ""+i);
			}

			multiRenderer.addSeriesRenderer(xRenderer);
			multiRenderer.addSeriesRenderer(yRenderer);
			multiRenderer.addSeriesRenderer(zRenderer);

			// Getting a reference to LinearLayout of the MainActivity Layout
			

			// Creating a Line Chart
			mChart = ChartFactory.getLineChartView(getBaseContext(), dataset,
					multiRenderer);

			// Adding the Line Chart to the LinearLayout
			layout.addView(mChart);

		}
	}
////////////////////////////////////////achartengine method for openChart()/////////////////////////////////////////////////////

///////////////////////////achartengine auto create when implement SensorEventListener, OnClickListener/////////////////////////
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
///////////////////////////achartengine auto create when implement SensorEventListener, OnClickListener/////////////////////////
	
///////////////////////////achartengine auto create when implement SensorEventListener, OnClickListener/////////////////////////
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
///////////////////////////achartengine auto create when implement SensorEventListener, OnClickListener/////////////////////////


}
