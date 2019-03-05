/**
 * Project Name: Super-Pedometer
 * @version  3.6
 */
package com.example.other.step;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.stepdetector.R;

/**
 * Description: <br/>
 * Company: ZhongSou.com<br/>
 * Copyright: 2003-2014 ZhongSou All right reserved<br/>
 * 
 * @date 2015-5-11 下午3:29:00
 * @author xinghua.cao
 */
@SuppressLint({ "HandlerLeak", "SimpleDateFormat", "Wakelock" })
public class StepService extends Service implements SensorEventListener {
	private final String TAG = "Pedometer";
	private SensorManager mSensorManager;
	private Sensor mSensor;
	// private TimerNotifier mTimerNotifier;
	private static int mSteps = 0;
	// private static long mTimeValues = 0;
	public boolean isFirst = true;

	private int targetStepValue = 10000;
	// private static MediaPlayer mPlayer;
	private boolean ped_helper;

	private Handler handler;
	public int SHOWSTEPS = 2;// 0为息屏状态，1为非显示界面状�?�?为显示界面状�?

	public class StepBinder extends Binder {
		public StepService getService() {
			return StepService.this;
		}
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "[SERVICE] onCreate");
		super.onCreate();
		handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					break;
				default:
					break;
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction("USRCHANGE");
		// filter.addAction(Constants.SEND_CONTINUE_ACTION);//移除
		registerReceiver(mReceiver, filter);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		ped_helper = true;
		initValues(false);
		Log.i(TAG, "[SERVICE] onStart");
		flags = Service.START_REDELIVER_INTENT;
		try {
		} catch (Exception e) {
			// TODO: handle exception
		}
		registerDetector();
		stepNotifyCation(StepService.this, mSteps, targetStepValue);
		// START_STICKY_COMPATIBILITY：START_STICKY的兼容版本，但不保证服务被kill后一定能重启�?
		return START_STICKY;
	};

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(mReceiver);
			unregisterDetector();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stopForeground(true);
		Intent intent = new Intent();
		intent.setAction("com.example.stepdetector");
		sendBroadcast(intent);
		// if (mPlayer != null) {
		// mPlayer.release();
		// }
		super.onDestroy();

	}

	private void registerDetector() {
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mSensor,
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	private void unregisterDetector() {
		mSensorManager.unregisterListener(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "[SERVICE] onBind");
		return null;
	}

	/**
	 * Forwards pace values from PaceNotifier to the activity.
	 */

	public void passValue() {
		// if (mCallback != null) {
		if (ped_helper) {// 在息屏状态不刷新通知�?
			stepNotifyCation(StepService.this, mSteps, targetStepValue);
		}
		// mCallback.stepsChanged(mSteps);
		Intent intent2 = new Intent();
		intent2.setAction("COM.STEP.SERVICE");
		intent2.putExtra("overStep", mSteps);
		// 发�? �?��步数刷新广播
		sendBroadcast(intent2);
	}

	public void stepsChanged(int value) {
		// TODO Auto-generated method stub
		mSteps = value;
		// Log.i("xx", "kuaitian33:"+mSteps+"//"+initSteps);
		if (mSteps > 0 && mSteps % 30 == 0) {
			saveData();
		}
		passValue();

	}

	public void timeOut() {
		// TODO Auto-generated method stub
		handler.sendEmptyMessage(2);
		saveData();
	}
	/**
	 * 数据保存
	 */
	private void saveData() {
		if (isFastSave()) {
			return;
		}
		try {
//			DBHandle db = new DBHandle(this);
//			boolean  state =db.addUserGroupInfo(mSteps + "");
//			if(state){
//				System.out.println("ssssssssssss");
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SharedPreferences getSp() {
		return getApplication().getSharedPreferences("stepDetector1",
				Context.MODE_WORLD_READABLE);
	}

	// PendingIntent sender;// action
	// AlarmManager alarm;// 定时唤醒计步服务
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {// 息屏
				SHOWSTEPS = 0;
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {// 屏幕�?��时刷新界�?
				SHOWSTEPS = 1;
				stepNotifyCation(StepService.this, mSteps, targetStepValue);
			}
		}
	};


	public void initValues(boolean userChange) {
	}

	private Notification getNotification(int numStep,String result) {
		Notification notification = new NotificationCompat.Builder(this)
				.setTicker("开始计步").setOngoing(true)
				.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("今日行走步数 " + numStep )
                .setContentText(result)
                .build();
		return notification;
	}

	Notification notification;

	@SuppressWarnings("deprecation")
	public void stepNotifyCation(Context context, int numStep, int aimStep) {
		boolean ped_helper = true;
		if (!ped_helper) {
			return;
		}

		int num = aimStep - numStep;
		//final Intent intent = new Intent();
		//final PendingIntent contentIntent = PendingIntent.getActivity(context,
				//101, intent, 101);
		String result = "距离目标还差" + num + "步，继续保持";
		if (num <= 0) {
			result = "今日目标已完成！";
		}
        //if (notification == null) {
            notification = getNotification(numStep,result);
        //}

		startForeground(123, notification);
	}

	String currentName;
	private static long lastSaveTime;

	/**
	 * 10秒内不重复缓�?
	 * 
	 * @return
	 */
	public synchronized boolean isFastSave() {
		long time = System.currentTimeMillis();
		long timeD = time - lastSaveTime;
		if (0 < timeD && timeD < 10 * 1000) {
			return true;
		}
		lastSaveTime = time;
		return false;
	}

	public int getSteps() {
		return mSteps;

	}

	public void switchNotAndMus(boolean status) {
		// TODO Auto-generated method stub
		ped_helper = status;
		if (status) {
			stepNotifyCation(StepService.this, mSteps, targetStepValue);
		} else {
			try {
				stopForeground(true);
			} catch (Exception e) {
				// TODO: handle exception
			}
			// if (mPlayer != null) {
			// mPlayer.release();
			// }
		}

	}

	/***************************** 计步相关 *************************/
	int index = 0;
	double[] info = new double[12];
	int infoIndex = 0;
	private long lastGetTime;
	private long lastTime;
	double newValue = -1.0D;
	private long nowTime;
	double oldValue = -1.0D;
	double[] queue = new double[12];
	double result_X = 0.0D;
	double result_Y = 0.0D;
	double result_Z = 0.0D;
	public double UPPERTHRESHOLDVALUE = 10.5D;
	private double UPPERPARAM = 1.095;
	double value = 0.0D;
	double INTERVALVALUE = 20;
	public static double SAMPLEVALUE = 7;// 取平均数的样�?
	public static int SPECIMENVALUE = 11;// 标本
	public final String STEPSAMPLEVALUE = "STEPSAMPLEVALUE";
	public final String STEPSPECIMENVALUE = "STEPSPECIMENVALUE";

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		synchronized (this) {
			if (event.sensor.getType() == 1) {// 加�?度传感器
				nowTime = System.currentTimeMillis();
				if ((nowTime - lastGetTime) <= INTERVALVALUE) {
					return;
				}
				result_X = keepTwoDecimalPlaces(event.values[0]);
				result_Y = keepTwoDecimalPlaces(event.values[1]);
				result_Z = keepTwoDecimalPlaces(event.values[2]);
				newValue = keepTwoDecimalPlaces(Math.sqrt(result_X * result_X
						+ result_Y * result_Y + result_Z * result_Z));
				if (oldValue == -1.0D) {
					oldValue = newValue;
				}
				if ((Math.abs(newValue - oldValue) > 8.0D)) {
					oldValue = newValue;
					if ((nowTime - lastTime) <= 280L) {
						return;
					}
					lastTime = nowTime;
					onStep();
					return;
				}
				lastGetTime = nowTime;
				oldValue = newValue;
				double[] arrayOfDouble = queue;
				int i = index;
				index = (i + 1);
				arrayOfDouble[i] = newValue;
				if (index < SAMPLEVALUE)
					return;
				value = keepTwoDecimalPlaces(getResultValue());
				// Log.i("value", value + "  ");
				if (infoIndex < SPECIMENVALUE) {
					info[infoIndex] = value;
					infoIndex = (1 + infoIndex);
					return;
				}
				if (((nowTime - lastTime) <= 280L) || !JudgeStep(value)) {
					long delteTime = nowTime - lastTime;
					if (delteTime % 1180L < 40) {
						timeOut();
					}
					return;
				}
				onStep();
				lastTime = nowTime;
			}
		}
	}

	/********************* 步数逻辑处理，连续行走十步开始计�? ***********************/
	private Long startTime;
	private Long currentTime;
	private int tempStep;
	private boolean isStepFirst = true;

	public void onStep() {
		long temp = System.currentTimeMillis();

		if (isStepFirst) {

			startTime = temp;
			notifyListener();
			isStepFirst = false;
			tempStep = 1;
		} else {
			currentTime = temp;
			if (currentTime - startTime > 2000) {
				tempStep = 1;
			} else {
				if (tempStep == 10) {
					mSteps += tempStep;

				} else if (tempStep >= 10) {
					mSteps++;
				}
				tempStep++;
			}
			startTime = currentTime;
			notifyListener();
		}
	}

	public void notifyListener() {
		stepsChanged(mSteps);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	double keepTwoDecimalPlaces(double paramDouble) {
		return (int) (paramDouble * 1000.0D) / 1000.0D;
	}

	private double getUpperThresholdValue() {
		return UPPERTHRESHOLDVALUE * UPPERPARAM;
	}

	public boolean JudgeStep(double paramDouble) {
		double flag = getUpperThresholdValue();
		for (int i = 1;; i++) {
			if (i >= SPECIMENVALUE) {
				info[SPECIMENVALUE - 1] = paramDouble;
				int middleNum = (int) (Math.floor(SPECIMENVALUE / 2));
				for (double dou : info) {
					if (info[middleNum] >= dou) {

					} else {
						return false;
					}
				}
				if (info[middleNum] >= flag) {
					if ((info[middleNum] - flag) < 1) {
						double average = getAverage(info);
						double delte = info[middleNum] - average;
						float param = 0.15f;
						if (SPECIMENVALUE == 7) {
							param = 0.2f;
						}
						if (delte < param) {
							return false;
						}
					}
					return true;
				}
				return false;
			}
			info[(i - 1)] = info[i];
		}
	}

	private double getAverage(double[] dous) {
		double average = 0;
		int num = 0;
		for (double dou : dous) {
			average += dou;
			if (dou != 0) {
				num++;
			}
		}
		return average / num;
	}

	public double getResultValue() {
		double d = queue[0];
		for (int i = 1;; ++i) {
			if (i >= SAMPLEVALUE) {
				index = (-1 + index);
				return d / (1 + index);
			}
			d += queue[i];
			queue[(i + -1)] = queue[i];
		}
	}

}
