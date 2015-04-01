package com.p.autoxj;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.lef.scanner.*;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import java.util.Collection;
import java.util.Iterator;

public class MyActivity extends Activity implements
        com.lef.scanner.IBeaconConsumer{
    public static final int REDRAW_SCAN_VIEW = 1;
    public static final int FIND_NEW_BEACON = 2;
    public static final int REQUEST_FINISH_SUCCESS = 0;
    public static final int KEY_TIME_OUT = 4;
    public static final int REQUEST_FINISH_FAIL = 3;
    public static final int UPLOAD_WAIT_SUCCESS = 5;
    public static final int UPLOAD_WAIT_FAIL = 6;
    public static final int UPLOAD_TYPE_CHECKED = 7;
    public static final int UPLOAD_TYPE_UNCHECKED = 8;
    private IBeaconManager iBeaconManager;
    ScanView scanView = null;
    Button btSee,btReset,btUpload,btMyloc;
    public LocationClient mLocationClient = null;
    public LocationClientOption locationClientOption = null;
    public MapView mMapView = null;
    public BaiduMap mBaiduMap = null;
    public BDLocationListener myListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
//            Toast.makeText(MyActivity.this,"定位到位置："+""+bdLocation.getLongitude()+""+bdLocation.getLatitude(),Toast.LENGTH_LONG).show();
            //Log.d("定位到位置",""+bdLocation.getLongitude()+""+bdLocation.getLatitude());
            PublicData.getInstance().setLatitude(bdLocation.getLatitude());
            PublicData.getInstance().setLongitude(bdLocation.getLongitude());
            PublicData.getInstance().setRadius(bdLocation.getRadius());
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())
                            // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude()).build();
            if(mBaiduMap != null) {

                mBaiduMap.setMyLocationData(locData);
                //mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
                //mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude())));
            }

//            Log.d("location",bdLocation.getProvince());
        }
    };
    Handler mhandler = new Handler(){
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what){
                case REDRAW_SCAN_VIEW:
                    scanView.rePaint();
                    break;
                case FIND_NEW_BEACON:
                    scanView.setFindNum(PublicData.getInstance().beacons.size());
                    scanView.rePaint();

                    break;
                case KEY_TIME_OUT:
                    new AlertDialog.Builder(MyActivity.this)
                            .setTitle("警告")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("登录信息已过期，请重新登录!")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(MyActivity.this, LoginActivity.class);
                                    startActivityForResult(intent, 5);
                                }
                            })
                            .create().show();
                    break;
                case REQUEST_FINISH_SUCCESS:
                    Toast.makeText(MyActivity.this,"巡检数据上报成功！",Toast.LENGTH_LONG).show();
                    break;
                case REQUEST_FINISH_FAIL:
                    Toast.makeText(MyActivity.this, "巡检数据上报失败！", Toast.LENGTH_LONG).show();
                    break;
            }
            super.handleMessage(msg);
        }
    };
    public void startDrawSignal(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    Message ms = new Message();
                    ms.what = REDRAW_SCAN_VIEW;
                    mhandler.sendMessage(ms);
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public void startUploadTHread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {

                    if(PublicData.getInstance().isLogin()) {
                        if (PublicData.getInstance().isNetworkAvailable()) {
                            if (PublicData.getInstance().isLogin()) {
                                Intent intent = new Intent(MyActivity.this, NetWorkService.class);
                                intent.putExtra("ActivityName", MyActivity.class.getName());
                                intent.putExtra("ReuqestType", "upload_checked");
                                startService(intent);
                                Toast.makeText(MyActivity.this, "开始上传...", Toast.LENGTH_SHORT).show();
                            } else {
                                Intent intent = new Intent(MyActivity.this, LoginActivity.class);
                                startActivityForResult(intent, 5);
                            }

                        } else {
                            Toast.makeText(MyActivity.this, "当前无网络连接！", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        scanView.setLogin(false);
                    }
                    try {
                        Thread.sleep(1000 * 120);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("周围的Beacons");
        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {
            TextView title = (TextView) findViewById(actionBarTitleId);
            if (title != null) {
                title.setTextColor(Color.WHITE);
            }
        }
        getActionBar().setIcon(
                new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.backcolor_norock));
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.myactivity_activity);
        iBeaconManager = IBeaconManager.getInstanceForApplication(this);
        PublicData.getInstance().getHandlerHashMap().put(MyActivity.class.getName(),mhandler);
        btReset = (Button)findViewById(R.id.bt_reset);
        btSee = (Button)findViewById(R.id.bt_see);
        btUpload = (Button)findViewById(R.id.bt_upload);
        btMyloc = (Button)findViewById(R.id.bt_mylocation);
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
        //导入离线地图
        final MKOfflineMap offlineMap = new MKOfflineMap();
        offlineMap.init(new MKOfflineMapListener() {
            @Override
            public void onGetOfflineMapState(int i, int i1) {
                switch (i){
                    case MKOfflineMap.TYPE_NEW_OFFLINE:
                        if(i1 > 0){
                            offlineMap.importOfflineData();
                        }
                }
            }
        });
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );    //注册监听函数
        locationClientOption = new LocationClientOption();
        locationClientOption.setOpenGps(true);
        locationClientOption.setCoorType("bd09ll");
        locationClientOption.setScanSpan(1000);
        locationClientOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        mLocationClient.setLocOption(locationClientOption);
        mLocationClient.start();
        mLocationClient.requestLocation();
        btReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MyActivity.this)
                        .setTitle("警告")
                        .setMessage("是否重置状态").setCancelable(false)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                PublicData.getInstance().checkBeaconSet.clear();
                                PublicData.getInstance().beacons.clear();
                                PublicData.getInstance().uploadBeaconSet.clear();
                                scanView.setFindNum(0);
                                PublicData.getInstance().removeCheckedBeaconInDb();
                                Toast.makeText(MyActivity.this, "状态重置成功！", Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create().show();

            }
        });
        btMyloc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(DBIbeancon dbIbeancon:PublicData.getInstance().beacons){
                    LatLng latLng = new LatLng(Double.valueOf(dbIbeancon.getLatitude()),Double.valueOf(dbIbeancon.getLongitude()));
                    Bitmap bitmap = dbIbeancon.getBitmap();
                    MarkerOptions options = new MarkerOptions();
                    options.position(latLng)
                            .icon(BitmapDescriptorFactory.fromBitmap(PublicData.getInstance().createBeaconLocationBitmap(10))).zIndex(5);
                    options.perspective(true);
                    Marker marker = (Marker)mBaiduMap.addOverlay(options);
                }
            }
        });
        btSee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyActivity.this,ShowActivity.class);
                startActivity(intent);
            }
        });
        btUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyActivity.this,LoginActivity.class);
                startActivityForResult(intent,6);
//                if(PublicData.getInstance().isNetworkAvailable()) {
//                    if(PublicData.getInstance().isLogin()){
//                        Intent intent = new Intent(MyActivity.this, NetWorkService.class);
//                        intent.putExtra("ActivityName", MyActivity.class.getName());
//                        intent.putExtra("ReuqestType", "upload_checked");
//                        startService(intent);
//                        Toast.makeText(MyActivity.this, "开始上传...", Toast.LENGTH_SHORT).show();
//                    }else {
//                        Intent intent = new Intent(MyActivity.this,LoginActivity.class);
//                        startActivityForResult(intent,5);
//                    }
//
//                }else{
//                    Toast.makeText(MyActivity.this, "当前无网络连接！", Toast.LENGTH_SHORT).show();
//                }
            }
        });
        scanView = (ScanView) findViewById(R.id.scan_view);
        scanView.setFindNum(PublicData.getInstance().beacons.size());
        startDrawSignal();
        startUploadTHread();
    }
    private void initBluetooth() {
        // TODO Auto-generated method stub
        final BluetoothAdapter blueToothEable = BluetoothAdapter
                .getDefaultAdapter();
        if (!blueToothEable.isEnabled()) {
            new AlertDialog.Builder(MyActivity.this)
                    .setTitle("蓝牙开启")
                    .setMessage("配置需要开启蓝牙").setCancelable(false)
                    .setPositiveButton("开启", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            blueToothEable.enable();
                            iBeaconManager.bind(MyActivity.this);
                        }
                    }).setNegativeButton("退出", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    MyActivity.this.finish();
                }
            }).create().show();
        } else {
            iBeaconManager.setForegroundScanPeriod(800);
            iBeaconManager.bind(this);
        }
    }
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        // if (iBeaconManager.isBound(this)) {
        // iBeaconManager.unBind(this);
        // }
        mMapView.onPause();
    }
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mMapView.onResume();
        if (iBeaconManager != null && !iBeaconManager.isBound(this)) {
//            if(PublicData.getInstance().beacons.size() > 0)
//                PublicData.getInstance().beacons.clear();
            // 蓝牙dialog
            initBluetooth();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        scanView.setLogin(true);
        Log.d("return","resultCode"+requestCode);
        if(requestCode == 5 && resultCode == 5) {
            Intent intent = new Intent(MyActivity.this, NetWorkService.class);
            intent.putExtra("ActivityName", MyActivity.class.getName());
            intent.putExtra("ReuqestType", "upload_checked");
            startService(intent);
            Toast.makeText(this, "开始上传...", Toast.LENGTH_SHORT).show();
        }

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.upload, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_upload:
//                if(PublicData.getInstance().isNetworkAvailable()) {
//                    if(PublicData.getInstance().isLogin()){
//                        Intent intent = new Intent(MyActivity.this, NetWorkService.class);
//                        intent.putExtra("ActivityName", MyActivity.class.getName());
//                        intent.putExtra("ReuqestType", "upload_checked");
//                        startService(intent);
//                        Toast.makeText(this, "开始上传...", Toast.LENGTH_SHORT).show();
//                    }else {
//                        Intent intent = new Intent(MyActivity.this,LoginActivity.class);
//                        startActivityForResult(intent,5);
//                    }
//
//                }else{
//                    Toast.makeText(this, "当前无网络连接！", Toast.LENGTH_SHORT).show();
//                }
//                return true;
//
//            case R.id.action_see:
//                Intent intent = new Intent(MyActivity.this,ShowActivity.class);
//                startActivity(intent);
//                return true;
//            case R.id.action_reset:
//                PublicData.getInstance().checkBeaconSet.clear();
//                PublicData.getInstance().beacons.clear();
//                PublicData.getInstance().uploadBeaconSet.clear();
//                scanView.setFindNum(0);
//                PublicData.getInstance().removeCheckedBeaconInDb();
//                Toast.makeText(this,"状态重置成功！",Toast.LENGTH_SHORT).show();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mMapView.onDestroy();
        mLocationClient.stop();
        if (iBeaconManager != null && iBeaconManager.isBound(this)) {
            iBeaconManager.unBind(this);
        }
    }
    @Override
    public void onIBeaconServiceConnect() {
        // TODO Auto-generated method stub
        // 启动Range服务
        iBeaconManager.setRangeNotifier(new RangeNotifier() {

            public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons,
                                                Region region) {
                //if (ProgressBarVisibile) {
                //    handler.sendEmptyMessage(PROGRESSBARGONE);
                // }
                // java.util.Iterator<IBeacon> iterator = iBeacons.iterator();
                // while (iterator.hasNext()) {
                // IBeacon temp = iterator.next();
                // if (beaconDataListA.contains(temp)) {
                // beaconDataListA.set(beaconDataListA.indexOf(temp), temp);
                // handler.sendEmptyMessage(UPDATEUI);
                // } else {
                // beaconDataListA.add(temp);
                // handler.sendEmptyMessage(UPDATEUI);
                // }
                //
                // }

            }

            @Override
            public void onNewBeacons(Collection<IBeacon> iBeacons, Region region) {
                // TODO Auto-generated method stub
                // beaconDataListA.addAll(iBeacons);
                // handler.sendEmptyMessage(UPDATEUI);
                Iterator<IBeacon> iterator = iBeacons.iterator();
                while (iterator.hasNext()) {
                    IBeacon temp = iterator.next();
                    //Log.d("uuid",temp.getProximityUuid());
//                    if(temp.getProximityUuid().contains("fda50693-a4e2-4fb1-afcf-c6eb07647825") && temp.getMajor() == 10001) {
                    if(true) {
                        if (!PublicData.getInstance().checkBeaconSet.contains(temp.getBluetoothAddress())) {
                            scanView.setFindNewBeacon();
                            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            //vibrator.vibrate(1000);
                            DBIbeancon dbIbeancon = new DBIbeancon(temp);
                            dbIbeancon.setLatitude(String.valueOf(PublicData.getInstance().getLatitude()));
                            dbIbeancon.setLongitude(String.valueOf(PublicData.getInstance().getLongitude()));
                            PublicData.getInstance().beacons.add(dbIbeancon);
                            PublicData.getInstance().checkBeaconSet.add(temp.getBluetoothAddress());
                            mhandler.sendEmptyMessage(FIND_NEW_BEACON);
                            PublicData.getInstance().saveCheckBeacon2Db(dbIbeancon);
                        }
                    }
                    //
                }
            }

            @Override
            public void onGoneBeacons(Collection<IBeacon> iBeacons,
                                      Region region) {
                // TODO Auto-generated method stub
//                Iterator<IBeacon> iterator = iBeacons.iterator();
//                while (iterator.hasNext()) {
//                    IBeacon temp = iterator.next();
//                    if (aroundBeaconDataList.contains(temp)) {
//                        aroundBeaconDataList.remove(temp);
//                    }
//                    handler.sendEmptyMessage(UPDATEUI);
//                }
            }

            @Override
            public void onUpdateBeacon(Collection<IBeacon> iBeacons,
                                       Region region) {
                // TODO Auto-generated method stub
//                Iterator<IBeacon> iterator = iBeacons.iterator();
//                while (iterator.hasNext()) {
//                    IBeacon temp = iterator.next();
//                    if (!PublicData.getInstance().beacons.contains(temp)) {
//                        PublicData.getInstance().beacons.add(temp);
//                    }
//                    mhandler.sendEmptyMessage(FIND_NEW_BEACON);
//                }
            }

        });
        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {

            @Override
            public void didExitRegion(Region region) {
                // TODO Auto-generated method stub
            }

            @Override
            public void didEnterRegion(Region region) {
                // TODO Auto-generated method stub

            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                // TODO Auto-generated method stub

            }
        });
        try {
            Region myRegion = new Region("myRangingUniqueId", null, null, null);
            iBeaconManager.startRangingBeaconsInRegion(myRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
