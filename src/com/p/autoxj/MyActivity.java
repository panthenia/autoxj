package com.p.autoxj;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.Log;
import android.view.KeyEvent;
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
import java.util.HashSet;
import java.util.Iterator;

public class MyActivity extends Activity implements
        com.lef.scanner.IBeaconConsumer {
    public static final int REDRAW_SCAN_VIEW = 1;
    public static final int LOGIN_SUCCESS = 2;
    public static final int REQUEST_FINISH_SUCCESS = 0;
    public static final int KEY_TIME_OUT = 4;
    public static final int REQUEST_FINISH_FAIL = 3;
    public static final int UPLOAD_WAIT_SUCCESS = 5;
    public static final int UPLOAD_WAIT_FAIL = 6;
    public static final int UPLOAD_TYPE_CHECKED = 7;
    public static final int UPLOAD_TYPE_UNCHECKED = 8;
    public static final int No_AVILIBLE_NETWORK = 9;
    public static final int START_UPLOAD = 10;
    private IBeaconManager iBeaconManager;
    ScanView scanView = null;
    boolean goon_upload = true;
    Button btSee, btReset, btUpload, btMyloc, bt_Cloc;
    public LocationClient mLocationClient = null;
    public LocationClientOption locationClientOption = null;
    public MapView mMapView = null;
    public BaiduMap mBaiduMap = null;
    public BDLocationListener myListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            Log.d("定位到位置",""+bdLocation.getLongitude()+""+bdLocation.getLatitude());
            if (bdLocation.getLongitude()!=PublicData.getInstance().getLongitude() || bdLocation.getLatitude()!=PublicData.getInstance().getLatitude()){
                //Toast.makeText(MyActivity.this,"定位位置："+bdLocation.getLatitude()+","+bdLocation.getLongitude(),Toast.LENGTH_SHORT).show();
                PublicData.getInstance().setLatitude(bdLocation.getLatitude());
                PublicData.getInstance().setLongitude(bdLocation.getLongitude());
                PublicData.getInstance().setRadius(bdLocation.getRadius());
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                                // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(100).latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude()).build();
                if (mBaiduMap != null) {

                    mBaiduMap.setMyLocationData(locData);
                    //mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
                    //mBaiduMap.zxssetMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude())));
                }
            }
        }
    };
    Handler mhandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REDRAW_SCAN_VIEW:
                    scanView.rePaint();
                    break;
                case KEY_TIME_OUT:
                    Toast.makeText(MyActivity.this, "登录信息超时，正在重新登录！", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(MyActivity.this, NetWorkService.class);
                    intent.putExtra("ActivityName", MyActivity.class.getName());
                    intent.putExtra("ReuqestType", "login");
                    startService(intent);
                    break;
                case REQUEST_FINISH_SUCCESS:
                    Toast.makeText(MyActivity.this, "巡检数据上报成功！", Toast.LENGTH_LONG).show();
                    break;
                case REQUEST_FINISH_FAIL:
                    Toast.makeText(MyActivity.this, "巡检数据上报失败！", Toast.LENGTH_LONG).show();
                    break;
                case No_AVILIBLE_NETWORK:
                    Toast.makeText(MyActivity.this, "无网络连接！", Toast.LENGTH_SHORT).show();
                    break;
                case START_UPLOAD:
                    Toast.makeText(MyActivity.this, "开始上传！", Toast.LENGTH_SHORT).show();
                    break;
                case LOGIN_SUCCESS:
                    PublicData.getInstance().setLogin(true);
                    break;
//                case 100:
//                    Toast.makeText(MyActivity.this, "beacon位置变更！", Toast.LENGTH_SHORT).show();
//                    break;

            }
            super.handleMessage(msg);
        }
    };

//    public void startDrawSignal() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    Message ms = new Message();
//                    ms.what = REDRAW_SCAN_VIEW;
//                    mhandler.sendMessage(ms);
//                    try {
//                        Thread.sleep(30);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
//    }

    public void startUploadTHread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (goon_upload) {
                    try {
                        Thread.sleep(1000 * 60 * 50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (PublicData.getInstance().isNetworkAvailable()) {
                        if (PublicData.getInstance().isLogin()) {

                            Intent intent = new Intent(MyActivity.this, NetWorkService.class);
                            intent.putExtra("ActivityName", MyActivity.class.getName());
                            intent.putExtra("ReuqestType", "upload_checked");
                            startService(intent);
                            mhandler.sendEmptyMessage(MyActivity.START_UPLOAD);
                            //Toast.makeText(MyActivity.this, "开始上传...", Toast.LENGTH_SHORT).show();
                        }else{
                            Intent intent = new Intent(MyActivity.this, NetWorkService.class);
                            intent.putExtra("ActivityName", MyActivity.class.getName());
                            intent.putExtra("ReuqestType", "login");
                            startService(intent);
                        }

                    } else {
                        mhandler.sendEmptyMessage(MyActivity.No_AVILIBLE_NETWORK);
                        //Toast.makeText(MyActivity.this, "当前无网络连接！", Toast.LENGTH_SHORT).show();
                    }

                }

            }
        }).start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getActionBar();
        if (bar != null)
            bar.setTitle("周围的Beacons");
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
        PublicData.getInstance().getHandlerHashMap().put(MyActivity.class.getName(), mhandler);
        btReset = (Button) findViewById(R.id.bt_reset);
        btSee = (Button) findViewById(R.id.bt_see);
        btUpload = (Button)findViewById(R.id.bt_upload);
        btMyloc = (Button) findViewById(R.id.bt_mylocation);
        bt_Cloc = (Button) findViewById(R.id.bt_cloc);
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
        //导入离线地图
        final MKOfflineMap offlineMap = new MKOfflineMap();
        offlineMap.init(new MKOfflineMapListener() {
            @Override
            public void onGetOfflineMapState(int i, int i1) {
                switch (i) {
                    case MKOfflineMap.TYPE_NEW_OFFLINE:
                        if (i1 > 0) {
                            offlineMap.importOfflineData();
                        }
                }
            }
        });
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);    //注册监听函数
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
                                PublicData.getInstance().beaconMap.clear();
                                PublicData.getInstance().beaconLocSet.clear();
                                //scanView.setFindNum(0);
                                PublicData.getInstance().removeCheckedBeaconInDb();
                                PublicData.getInstance().freeAllBeaconIcon();
                                mBaiduMap.clear();
                                Toast.makeText(MyActivity.this, "状态重置成功！", Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create().show();

            }
        });
        bt_Cloc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(PublicData.getInstance().getLatitude(), PublicData.getInstance().getLongitude())));
            }
        });
        btMyloc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("label", "start label");
                mBaiduMap.clear();
                for (DBIbeancon dbIbeancon : PublicData.getInstance().beacons) {

                    LatLng latLng = new LatLng(Double.valueOf(dbIbeancon.getLatitude()), Double.valueOf(dbIbeancon.getLongitude()));
                    MarkerOptions options = new MarkerOptions();
                    BitmapDescriptor descriptor = PublicData.getInstance().getBeaconIcon(dbIbeancon);
                    if (descriptor != null) {
                        options.position(latLng)
                                .icon(descriptor).zIndex(5);
                        mBaiduMap.addOverlay(options);
                    }
                }
            }
        });
        btSee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyActivity.this, ShowActivity.class);
                startActivity(intent);
            }
        });
        btUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyActivity.this,LoginActivity.class);
                startActivityForResult(intent, 6);
            }
        });
        scanView = (ScanView) findViewById(R.id.scan_view);
        scanView.setFindNum(PublicData.getInstance().beacons.size());
        goon_upload = true;
        btMyloc.callOnClick();
        startUploadTHread();
    }

    private void initBluetooth() {
        // TODO Auto-ge,nerated method stub
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
            iBeaconManager.setForegroundScanPeriod(500);
            iBeaconManager.bind(this);
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
//         if (iBeaconManager.isBound(this)) {
//         iBeaconManager.unBind(this);
//         }
//        mMapView.onPause();
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
        Log.d("return", "resultCode" + requestCode);
        if (requestCode == 5 && resultCode == 5) {
//            Intent intent = new Intent(MyActivity.this, NetWorkService.class);
//            intent.putExtra("ActivityName", MyActivity.class.getName());
//            intent.putExtra("ReuqestType", "upload_checked");
//            startService(intent);
//            Toast.makeText(this, "开始上传...", Toast.LENGTH_SHORT).show();
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
        goon_upload = false;
        mLocationClient.stop();
        if (iBeaconManager != null && iBeaconManager.isBound(this)) {
            iBeaconManager.unBind(this);
        }
        PublicData.getInstance().freeAllBeaconIcon();
    }

    @Override
    public void onIBeaconServiceConnect() {
        // TODO Auto-generated method stub
        // 启动Range服务
        iBeaconManager.setRangeNotifier(new RangeNotifier() {

            public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons,
                                                Region region) {
                boolean need_repaint = false;
                for (IBeacon temp : iBeacons) {
                    if (!PublicData.getInstance().checkBeaconSet.contains(temp.getBluetoothAddress())) {
                        DBIbeancon dbIbeancon = new DBIbeancon(temp);
                        dbIbeancon.setBeaconNumber(PublicData.getInstance().beacons.size());
                        PublicData.getInstance().makeBeaconIcon(dbIbeancon);
                        HashSet<String> aset = new HashSet<String>();
                        aset.add(String.valueOf(PublicData.getInstance().getLatitude())+String.valueOf(PublicData.getInstance().getLongitude()));
                        PublicData.getInstance().beaconLocSet.put(temp.getBluetoothAddress(), aset);
                        dbIbeancon.setLatitude(String.valueOf(PublicData.getInstance().getLatitude()));
                        dbIbeancon.setLongitude(String.valueOf(PublicData.getInstance().getLongitude()));
                        PublicData.getInstance().saveBeaconLocation2Db(temp,new LatLng(PublicData.getInstance().getLatitude(),PublicData.getInstance().getLongitude()));
                        PublicData.getInstance().beacons.add(dbIbeancon);
                        PublicData.getInstance().checkBeaconSet.add(temp.getBluetoothAddress());
                        PublicData.getInstance().beaconMap.put(dbIbeancon.getBluetoothAddress(),dbIbeancon);
                        //mhandler.sendEmptyMessage(FIND_NEW_BEACON);
                        PublicData.getInstance().saveCheckBeacon2Db(dbIbeancon);
                        LatLng latLng = new LatLng(Double.valueOf(dbIbeancon.getLatitude()), Double.valueOf(dbIbeancon.getLongitude()));
                        MarkerOptions options = new MarkerOptions();
                        BitmapDescriptor descriptor = PublicData.getInstance().getBeaconIcon(dbIbeancon);
                        if (descriptor != null) {
                            options.position(latLng)
                                    .icon(descriptor).zIndex(5);
                            mBaiduMap.addOverlay(options);
                        }
                    } else {
                        if (PublicData.getInstance().beaconMap.containsKey(temp.getBluetoothAddress())){
                            DBIbeancon dbIbeancon = PublicData.getInstance().beaconMap.get(temp.getBluetoothAddress());
                            dbIbeancon.setIntRsst(temp.getRssi());
                            dbIbeancon.setMajor(String.valueOf(temp.getMajor()));
                            dbIbeancon.setMinor(String.valueOf(temp.getMinor()));
                            HashSet<String> aset = PublicData.getInstance().beaconLocSet.get(temp.getBluetoothAddress());
//                            if (aset == null){
//                                aset = new HashSet<String>();
//                                PublicData.getInstance().beaconLocSet.put(temp.getBluetoothAddress(),aset);
//                            }
//                            if (!aset.contains(String.valueOf(PublicData.getInstance().getLatitude())+String.valueOf(PublicData.getInstance().getLongitude()))){
                                double k;
                                if (temp.getRssi() > dbIbeancon.getRssi()){
                                    k = 0.3;
                                }else k = 0.7;
                                double newla = Double.valueOf(dbIbeancon.getLatitude())*(1.0-k)+PublicData.getInstance().getLatitude()*k;
                                double newlo = Double.valueOf(dbIbeancon.getLongitude())*(1.0-k)+PublicData.getInstance().getLongitude()*k;
                                dbIbeancon.setLatitude(String.valueOf(PublicData.getInstance().getLatitude()));
                                dbIbeancon.setLongitude(String.valueOf(PublicData.getInstance().getLongitude()));
//                                aset.add(String.valueOf(PublicData.getInstance().getLatitude())+String.valueOf(PublicData.getInstance().getLongitude()));
//                            dbIbeancon.setLatitude(String.valueOf(newla));
//                            dbIbeancon.setLongitude(String.valueOf(newlo));
////
                            PublicData.getInstance().saveBeaconLocation2Db(temp, new LatLng(PublicData.getInstance().getLatitude(), PublicData.getInstance().getLongitude()));
//                                for (DBIbeancon ibeancon:PublicData.getInstance().beacons){
//                                    if (ibeancon.getBluetoothAddress().contains(temp.getBluetoothAddress())){
//                                        ibeancon.setLatitude(String.valueOf(PublicData.getInstance().getLatitude()));
//                                        ibeancon.setLongitude(String.valueOf(PublicData.getInstance().getLongitude()));
//                                    }
//                                }
                                need_repaint = true;
                                boolean r = PublicData.getInstance().updateBeacon2Db(dbIbeancon);
                                Log.d("update beacon", "" + r);
                                //mhandler.sendEmptyMessage(100);
//                            }
                        }

                    }

                    //
                }
                if (need_repaint)
                    btMyloc.callOnClick();

            }

            @Override
            public void onNewBeacons(Collection<IBeacon> iBeacons, Region region) {
                // TODO Auto-generated method stub
                // beaconDataListA.addAll(iBeacons);
                // handler.sendEmptyMessage(UPDATEUI);

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
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            finish();
            if (PublicData.getInstance().start_activity != null){
                PublicData.getInstance().start_activity.finish();
            }
            System.exit(0);
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
