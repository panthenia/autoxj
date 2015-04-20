package com.p.autoxj;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by p on 2015/3/3.
 */
public class PublicData extends Application {
    private static PublicData self;
    public ArrayList<DBIbeancon> beacons = new ArrayList<DBIbeancon>();
    public HashSet<String> checkBeaconSet = new HashSet<String>();
    public CopyOnWriteArraySet<String> uploadBeaconSet = new CopyOnWriteArraySet<String>();
    public DataUtil du;
    private String ip;
    public Activity start_activity = null;
    public MessageDigest md5_encriptor = null;
    public HashMap<String,BitmapDescriptor> beaconIconHahsmap = new HashMap<String, BitmapDescriptor>();
    public boolean isHas_save_ip() {
        return has_save_ip;
    }

    public void setHas_save_ip(boolean has_save_ip) {
        this.has_save_ip = has_save_ip;
    }

    private boolean has_save_ip;

    public boolean isHas_save_user() {
        return has_save_user;
    }

    public void setHas_save_user(boolean has_save_user) {
        this.has_save_user = has_save_user;
    }

    private boolean has_save_user;
    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
    private String user,psw;

    public String getPsw() {
        return psw;
    }

    public void setPsw(String psw) {
        this.psw = psw;
    }

    public String getUser() {
        return user;
    }

    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    boolean login = false;
    double latitude, longitude;
    float radius;

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setUser(String user) {
        this.user = user;
    }
    private ConcurrentHashMap<String, Handler> handlerHashMap = new ConcurrentHashMap<String, Handler>();

    public ConcurrentHashMap<String, Handler> getHandlerHashMap() {
        return handlerHashMap;
    }
    private String port;
    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        du = new DataUtil(this, this.getString(R.string.unupload_dbname), null, 1);
        try {
            md5_encriptor = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        getCheckedBeaconInDb();
    }
    public static PublicData getInstance(){
        return self;
    }
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    String key;
    public String getMd5(String data){

        md5_encriptor.reset();
        byte[] data_byte = null;
        data_byte = data.getBytes();

        byte[] hash_data = md5_encriptor.digest(data_byte);
        StringBuilder md5StrBuff = new StringBuilder();

        for (byte aHash_data : hash_data) {
            if (Integer.toHexString(0xFF & aHash_data).length() == 1)
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & aHash_data));
            else
                md5StrBuff.append(Integer.toHexString(0xFF & aHash_data));
        }

        return md5StrBuff.toString();
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            //如果仅仅是用来判断网络连接
            //则可以使用 cm.getActiveNetworkInfo().isAvailable();
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public BitmapDescriptor getBeaconIcon(DBIbeancon ibeancon){
        return beaconIconHahsmap.get(ibeancon.getBluetoothAddress());
    }
    public void makeBeaconIcon(DBIbeancon ibeancon){
        if (beaconIconHahsmap.containsKey(ibeancon.getBluetoothAddress()))
            return;
        else {
            Bitmap bitmap = createBeaconLocationBitmap(ibeancon.getBeaconNumber());
            BitmapDescriptor descriptor = null;
            if (bitmap != null) {
                descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                beaconIconHahsmap.put(ibeancon.getBluetoothAddress(),descriptor);
            }
        }
    }
    public void freeAllBeaconIcon(){
        Iterator iterator = beaconIconHahsmap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry entry = (Map.Entry) iterator.next();
            BitmapDescriptor descriptor = (BitmapDescriptor)entry.getValue();
            descriptor.recycle();
        }
        beaconIconHahsmap.clear();
    }
    public String getImei() {
        return ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                .getDeviceId();
    }
    public boolean updateBeacon2Db(DBIbeancon ibeancon){
        boolean result = true;
        SQLiteDatabase db = du.getReadableDatabase();
        // area text,type text,time text,val text

        String sql = String.format("update unupbeacon set latitude='%s',longitude='%s', rssi='%s' where mac_id='%s'",ibeancon.getLatitude(),ibeancon.getLongitude(),ibeancon.getRssi(),ibeancon.getBluetoothAddress());
        uploadBeaconSet.remove(ibeancon.getBluetoothAddress());
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            result = false;
        } finally {
            db.close();
        }
        return result;
    }
    public boolean saveCheckBeacon2Db(DBIbeancon iBeacon) {
        boolean result = true;
        SQLiteDatabase db = du.getReadableDatabase();
        String sql;
        // area text,type text,time text,val text

        sql = "insert into unupbeacon(mac_id,uuid,rssi,major,minor,latitude,longitude) values('";
        sql += iBeacon.getBluetoothAddress() + "','" + iBeacon.getProximityUuid()
                + "','" + iBeacon.getRssi()
                + "','" + iBeacon.getMajor() + "','" + iBeacon.getMinor()
                + "','" + iBeacon.getLatitude() + "','" + iBeacon.getLongitude()
                +"')";
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            result = false;
        } finally {
            db.close();
        }
        Log.d("save beacon",String.valueOf(result));
        return result;

    }
    public void removeCheckedBeaconInDb(){
        SQLiteDatabase db = du.getReadableDatabase();
        String sql = "delete from unupbeacon";
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
    public void removeUploadCheckedBeaconInDb(){
        SQLiteDatabase db = du.getReadableDatabase();

        String sql = "delete from unupbeacon where mac_id = '";
        try {
            for(String mac:uploadBeaconSet){
                sql += mac+"';";
                db.execSQL(sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
    public Bitmap createBeaconLocationBitmap(int seq){
        Bitmap bitmap = Bitmap.createBitmap(50,50, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        Paint paint = new Paint();
        paint.setColor(0xff0180ff);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);
        canvas.setBitmap(bitmap);
        Paint paint1 = new Paint();
        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint1.descent() + paint1.ascent()) / 2)) ;
        //((textPaint.descent() + textPaint.ascent()) / 2) is the distance from the baseline to the center.

        paint1.setColor(0xffffffff);
        paint1.setAntiAlias(true);
        paint1.setTextAlign(Paint.Align.CENTER);
        paint1.setTextSize(25);
        canvas.drawCircle(25,25,25,paint);
        canvas.drawText(String.valueOf(seq),xPos,yPos,paint1);
        return bitmap;
    }
    public void getCheckedBeaconInDb() {
        SQLiteDatabase db = du.getReadableDatabase();
        String sql;
        Cursor cursor = null;
        // area text,type text,time text,val text
        sql = "select * from unupbeacon";
        try {
            cursor = db.rawQuery(sql, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {//直到返回false说明表中到了数据末尾
                    DBIbeancon ibeacon = new DBIbeancon();
                    //Log.d("savebeacon",cursor.getString(cursor.getColumnIndex("mac_id")));
                    ibeacon.setMac(cursor.getString(cursor.getColumnIndex("mac_id")));
                    ibeacon.setMajor(cursor.getString(cursor.getColumnIndex("major")));
                    ibeacon.setMinor(cursor.getString(cursor.getColumnIndex("minor")));

                    ibeacon.setRssi(cursor.getString(cursor.getColumnIndex("rssi")));
                    ibeacon.setUuid(cursor.getString(cursor.getColumnIndex("uuid")));
                    ibeacon.setLatitude(cursor.getString(cursor.getColumnIndex("latitude")));
                    ibeacon.setLongitude(cursor.getString(cursor.getColumnIndex("longitude")));

                    beacons.add(ibeacon);
                    ibeacon.setBeaconNumber(beacons.size());
                    makeBeaconIcon(ibeacon);
                    Log.d("savebeacon",String.valueOf(beacons.size()));
                    checkBeaconSet.add(ibeacon.getBluetoothAddress());
                }
            }
        } catch (SQLException e) {
        } finally {
            db.close();
        }
    }
}
