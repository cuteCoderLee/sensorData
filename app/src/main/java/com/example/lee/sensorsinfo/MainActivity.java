package com.example.lee.sensorsinfo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = "SensorInfo";
    //另起一个线程用来采样数据
    private HandlerThread saveInfoThread;
    private Handler saveInfoHandler;

    //定义sensor管理器
    private SensorManager sensorManager;
    //wifi管理器
    private WifiManager wifiManager;
    private MyWifiReceiver wifiReceiver;
    //位置管理器
    private LocationManager locationManager;
    private Location location;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    Context mContext;

    //熄屏问题
    // private PowerManager.WakeLock wakeLock;

    //传感器监听
    private SensorEventListener gyroscopeListener;//陀螺仪
    private SensorEventListener accelerometerListener;//加速度
    private SensorEventListener gravityListener;//重力
    private SensorEventListener orientationListener;//方向
    private SensorEventListener magneticListener;//磁场
    private SensorEventListener lightListener;//光敏
    private SensorEventListener proximityListener;//红外
    private SensorEventListener rotatevectorListener;//旋转矢量

    Button start;
    Button stop;
    TextView fileDir;
    TextView timeStamp;
    TextView gpsLocationText;
    TextView gyroscopeText;
    TextView accelerometerText;
    TextView gravityText;
    TextView orientationText;
    TextView magneticText;
    TextView lightText;
    TextView proximityText;
    TextView rotatevectorText;
    TextView wifiInfoText;
    //wifi信息列表
    List<ScanResult> allWifiList;
    StringBuilder wifiInfoSb;
    //数据信息
    String timeData, locationData, gyroscopeData, accelerometerData, gravityData, orientationData;
    String magneticData, lightData, proximityData, rotatevectorData, wifiData;
    //文件位置
    String path = null;
    //File sensorInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //New Handler and Thread
        saveInfoThread = new HandlerThread("SaveInfoThread");
        saveInfoThread.start();
        saveInfoHandler = new Handler(saveInfoThread.getLooper());
        //保持屏幕长亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //
//        PowerManager manager = (PowerManager)getSystemService(POWER_SERVICE);
//        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);//CPU依然运行
//        wakeLock.acquire();
//        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);//屏幕熄灭后依然运行
//        filter.addAction(Intent.ACTION_SCREEN_OFF);

        //获取传感器管理服务
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //获取位置管理服务
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocationData(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                updateLocationData(locationManager.getLastKnownLocation(s));
            }

            @Override
            public void onProviderDisabled(String s) {
                updateLocationData(null);
            }

        });

        //获取wifi管理服务
        wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        //初始化监听wifi扫描完成的广播信息的接收器
        wifiReceiver = new MyWifiReceiver();
        //获取textview组件
        fileDir = (TextView)findViewById(R.id.fileDir);
        timeStamp = (TextView)findViewById(R.id.timeStamp);
        gpsLocationText = (TextView)findViewById(R.id.gpsLocation);
        gyroscopeText=(TextView)findViewById(R.id.gyroscope);
        accelerometerText = (TextView)findViewById(R.id.accelerometer);
        gravityText=(TextView)findViewById(R.id.gravity);
        orientationText = (TextView) findViewById(R.id.orientation);
        magneticText=(TextView)findViewById(R.id.magnetic);
        lightText = (TextView)findViewById(R.id.light);
        proximityText=(TextView)findViewById(R.id.proximity);
        rotatevectorText = (TextView) findViewById(R.id.rotatevector);
        wifiInfoText = (TextView) findViewById(R.id.wifiInfo);
        //开始和结束记录
        start = (Button)findViewById(R.id.start);
        stop = (Button)findViewById(R.id.stop);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!wifiManager.isWifiEnabled()) {
                    //Toast.makeText(this, "Wifi is disabled, now turn it on.", Toast.LENGTH_LONG).show();
                    wifiManager.setWifiEnabled(true);
                }
                wifiInfoText.setText("Here is all scanned wifi info");
                //开始扫描
                wifiManager.startScan();
                //注册
                //陀螺仪
                sensorManager.registerListener(gyroscopeListener,sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_NORMAL);
                //加速度
                sensorManager.registerListener(accelerometerListener,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
                //重力
                sensorManager.registerListener(gravityListener,sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),SensorManager.SENSOR_DELAY_NORMAL);
                //方向,来自重力和磁场
                sensorManager.registerListener(orientationListener,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_NORMAL);
                //磁场
                sensorManager.registerListener(magneticListener,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);
                ////压力
      //        sensorManager.registerListener((SensorEventListener) this,sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),SensorManager.SENSOR_DELAY_NORMAL);
                //光敏
                sensorManager.registerListener(lightListener,sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),SensorManager.SENSOR_DELAY_NORMAL);
                //距离（红外）
                sensorManager.registerListener(proximityListener,sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),SensorManager.SENSOR_DELAY_NORMAL);
                //旋转矢量
                sensorManager.registerListener(rotatevectorListener,sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_NORMAL);

                registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

                saveInfoHandler.removeCallbacks(saveInfoRunnable);
                saveInfoHandler.postDelayed(saveInfoRunnable, 500);//保存采样率
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorManager.unregisterListener(gyroscopeListener);
                sensorManager.unregisterListener(accelerometerListener);
                sensorManager.unregisterListener(gravityListener);
                sensorManager.unregisterListener(orientationListener);
                sensorManager.unregisterListener(magneticListener);
                sensorManager.unregisterListener(lightListener);
                sensorManager.unregisterListener(proximityListener);
                sensorManager.unregisterListener(rotatevectorListener);
                saveInfoHandler.removeCallbacks(saveInfoRunnable);
                unregisterReceiver(wifiReceiver);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        //时间信息
        //timeStamp.setText(getDate());
        //wifi信息
        //wifiInfoText.setText(getWifiInfo());
        //打开wifi
        //
//        if (!wifiManager.isWifiEnabled()) {
//            Toast.makeText(this, "Wifi is disabled, now turn it on.", Toast.LENGTH_LONG).show();
//            wifiManager.setWifiEnabled(true);
//        }
//        wifiInfoText.setText("Here is all scanned wifi info");
        //注册监听器
        //registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
//        //开始扫描
//        wifiManager.startScan();
//        Toast.makeText(this, "Now scanning all wifi info...", Toast.LENGTH_LONG).show();

        //陀螺仪监听器
        gyroscopeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--陀螺仪角速度--");
                sb.append("\n绕X轴：");
                sb.append(values[0]);
                sb.append("\n绕Y轴：");
                sb.append(values[1]);
                sb.append("\n绕z轴：");
                sb.append(values[2]);
                gyroscopeText.setText(sb.toString());
                gyroscopeData = ""+values[0]+","+values[1]+","+values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        //加速度监听
        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--加速度--");
                sb.append("\n沿X轴：");
                sb.append(values[0]);
                sb.append("\n沿Y轴：");
                sb.append(values[1]);
                sb.append("\n沿z轴：");
                sb.append(values[2]);
                accelerometerText.setText(sb.toString());
                accelerometerData = ""+values[0]+","+values[1]+","+values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        //重力
        gravityListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--重力加速度--");
                sb.append("\nX轴：");
                sb.append(values[0]);
                sb.append("\nY轴：");
                sb.append(values[1]);
                sb.append("\nz轴：");
                sb.append(values[2]);
                gravityText.setText(sb.toString());
                gravityData = ""+values[0]+","+values[1]+","+values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        //方向
        orientationListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--方向角度--");
                sb.append("\n绕Z轴：");
                sb.append(values[0]);
                sb.append("\n绕X轴：");
                sb.append(values[1]);
                sb.append("\n绕Y轴：");
                sb.append(values[2]);
                orientationText.setText(sb.toString());
                orientationData = ""+values[0]+","+values[1]+","+values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        //磁场
        magneticListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--磁场强度--");
                sb.append("\nX轴：");
                sb.append(values[0]);
                sb.append("\nY轴：");
                sb.append(values[1]);
                sb.append("\nz轴：");
                sb.append(values[2]);
                magneticText.setText(sb.toString());
                magneticData = ""+values[0]+","+values[1]+","+values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        //光强
        lightListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--光强--\n");
                sb.append(values[0]);
                lightText.setText(sb.toString());
                lightData = ""+values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        //距离，近程
        proximityListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--距离传感器--\n");
                sb.append(values[0]);
                proximityText.setText(sb.toString());
                proximityData = ""+values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        //旋转矢量
        rotatevectorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                StringBuilder sb = new StringBuilder();
                sb.append("--旋转矢量--");
                sb.append("\n绕X轴：");
                sb.append(values[0]);
                sb.append("\n绕Y轴：");
                sb.append(values[1]);
                sb.append("\n绕z轴：");
                sb.append(values[2]);
                rotatevectorText.setText(sb.toString());
                rotatevectorData = ""+values[0]+","+values[1]+","+values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        //生成文件位置
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            File sdcardDir = Environment.getExternalStorageDirectory();
//            File dir = new File(sdcardDir.toString() + "/mySensorData");
            // Check if we have write permission
            int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG,"do not have this permission!");
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
            String Dir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/0data";
            File dir = new File(Dir);
            if (!dir.exists())
                dir.mkdir();
            System.out.println(dir);
            fileDir.setText("result's here! "+dir);
            path = dir + "/" + getDate() + ".txt";//用系统时间为数据文件命名
            //path = dir+"/"+"sensorInfo.txt";
        }
    }

//    private String getWifiInfo() {
//        WifiInfo info = wifiManager.getConnectionInfo();
//        String mac = info.getMacAddress();
//        String ssid = info.getSSID();
//        int speed = info.getLinkSpeed();
//        int rssi = info.getRssi();
//        String wifiInfo = mac+","+ssid+","+speed+","+rssi;
//        return wifiInfo;
//    }

    @Override
    protected void onResume() {
        super.onResume();
//        //注册
//        //陀螺仪
//        sensorManager.registerListener(gyroscopeListener,sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_NORMAL);
//        //加速度
//        sensorManager.registerListener(accelerometerListener,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
//        //重力
//        sensorManager.registerListener(gravityListener,sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),SensorManager.SENSOR_DELAY_NORMAL);
//        //方向,来自重力和磁场
//        sensorManager.registerListener(orientationListener,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_NORMAL);
//        //磁场
//        sensorManager.registerListener(magneticListener,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);
//        ////压力
////        sensorManager.registerListener((SensorEventListener) this,sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),SensorManager.SENSOR_DELAY_NORMAL);
//        //光敏
//        sensorManager.registerListener(lightListener,sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),SensorManager.SENSOR_DELAY_NORMAL);
//        //距离（红外）
//        sensorManager.registerListener(proximityListener,sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),SensorManager.SENSOR_DELAY_NORMAL);
//        //旋转矢量
//        sensorManager.registerListener(rotatevectorListener,sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_NORMAL);
//
//        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
////        saveInfoHandler.removeCallbacks(saveInfoRunnable);
//        saveInfoHandler.postDelayed(saveInfoRunnable, 500);//保存采样率
    }

    @Override
    protected void onPause() {
        super.onPause();
//        sensorManager.unregisterListener(gyroscopeListener);
//        sensorManager.unregisterListener(accelerometerListener);
//        sensorManager.unregisterListener(gravityListener);
//        sensorManager.unregisterListener(orientationListener);
//        sensorManager.unregisterListener(magneticListener);
//        sensorManager.unregisterListener(lightListener);
//        sensorManager.unregisterListener(proximityListener);
//        sensorManager.unregisterListener(rotatevectorListener);
//        unregisterReceiver(wifiReceiver);
//        //saveInfoHandler.removeCallbacks(saveInfoRunnable);
//        //wakeLock.release();
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //位置数据字段
    private void updateLocationData(Location location) {
        if(location==null)
        {
            gpsLocationText.setText("null");
            locationData = "null,null";
        }
        else{
            StringBuilder sb = new StringBuilder();
            sb.append("--GPS定位信息--");
            sb.append("\n经度：");
            sb.append(location.getLongitude());
            sb.append("\n纬度：");
            sb.append(location.getLatitude());
            gpsLocationText.setText(sb.toString());
            locationData = ""+location.getLongitude()+","+location.getLatitude();
        }
    }

    //时间戳
    private String getDate() {
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);           // 获取年份
        int month = ca.get(Calendar.MONTH)+1;         // 获取月份
        int day = ca.get(Calendar.DATE);            // 获取日
        int minute = ca.get(Calendar.MINUTE);       // 分
        //int hour = ca.get(Calendar.HOUR);           // 小时
        int hour = ca.get(Calendar.HOUR_OF_DAY);//24小时制
        int second = ca.get(Calendar.SECOND);//秒
        int millisecond = ca.get(Calendar.MILLISECOND); //毫秒
        String M,D,H,m,s;
        if(month<10)
            M="0"+month;
        else M=""+month;
        if(day<10)
            D="0"+day;
        else D=""+day;
        if(hour<10)
            H="0"+hour;
        else H=""+hour;
        if(minute<10)
            m="0"+minute;
        else m=""+minute;
        if(second<10)
            s="0"+second;
        else s=""+second;
        String date = "" + year + M + D + H + m + s +millisecond;
        //Log.d(TAG, "date:" + date);
        return date;
    }
    //wifi信息
    class MyWifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Scan task finish, show info!");
            wifiInfoSb = new StringBuilder();
            StringBuilder temp = new StringBuilder();
            allWifiList = wifiManager.getScanResults();
            wifiInfoSb.append("--wifi信息-- "+ "\n");
            for(ScanResult scanResult:allWifiList)
            {
            	wifiInfoSb.append(scanResult.BSSID+","+scanResult.SSID+","+scanResult.level+"\n");
                temp.append(scanResult.BSSID+","+scanResult.SSID+","+scanResult.level+";");
            }
            wifiInfoText.setText(wifiInfoSb);
            wifiData = temp.toString();
        }
    }

    private Runnable saveInfoRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "0oooooops");
            //Do what you want to do ~0~
            //timeData = getDate();
            wifiManager.startScan();
            saveAllData2File();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timeStamp.setText(getDate());
                    //wifiInfoText.setText("hahahahahahahahahaha");
                }
            });
            saveInfoHandler.postDelayed(saveInfoRunnable, 500);
        }
    };

    private void saveAllData2File() {
        timeData=getDate();
        StringBuilder result = new StringBuilder();
        String allInfo = timeData+","+locationData+","+gyroscopeData+","+accelerometerData+","
                +gravityData+","+orientationData+","+magneticData+","
                +rotatevectorData+","+lightData+","+proximityData+","+wifiData+"\n";
        try {
            File sensorInfo = new File(path);
               //File sensorInfo = new File(sdcardDir.getAbsolutePath() + "/" + "sensorInfo.txt");
            if (!sensorInfo.exists()) {
                FileOutputStream fop = new FileOutputStream(sensorInfo);
                OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
                result.append(allInfo);
                writer.append(result);
                writer.close();
            }
            else{
                FileOutputStream fop = new FileOutputStream(sensorInfo, true);
                OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
                result.append(allInfo);
                writer.append(result);
                writer.close();
            }
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
