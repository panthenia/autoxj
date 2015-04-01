package com.p.autoxj;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.lef.scanner.IBeacon;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by p on 2014/12/15.
 */
public class NetWorkService extends IntentService {
    private static final String LOGIN_URL = "http://" + PublicData.getInstance().getIp() + ":" + PublicData.getInstance().getPort() + "/beacon/patrol!login";
    private static final String UPLOAD_URL = "http://" + PublicData.getInstance().getIp() + ":" + PublicData.getInstance().getPort() + "/beacon/patrol!report";

    public NetWorkService() {
        super("NetWorkService");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */

    public NetWorkService(String name) {
        super(name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.getClass().getName(), "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private String doRequest(URL url, String sdata, Handler handler) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            // 打开和URL之间的连接
            URLConnection conn = url.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");

            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(sdata);
            // flush输出流的缓冲
            out.flush();

            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += "\n" + line;
            }
        } catch (Exception e) {
            Message msg = new Message();
            msg.what = LoginActivity.SERVER_ERR;
            handler.sendMessage(msg);
            System.out.println("发送POST请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Handler ahandler;
        URL url;
        Log.d(getClass().getName(), "into onHandleIntent");
        PublicData publicData = PublicData.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String current_time = formatter.format(new Date(System.currentTimeMillis()));//获取当前时间
        if (intent.hasExtra("ActivityName")) {
            ahandler = PublicData.getInstance().getHandlerHashMap().get(intent.getStringExtra("ActivityName"));
            Message message = new Message();
            if (intent.hasExtra("ReuqestType")) {
                String rtype = intent.getStringExtra("ReuqestType");
                if (rtype.contains("upload_checked")) {
                    try {
                        url = new URL(UPLOAD_URL);
                        String data;
                        JSONArray jsonArray = new JSONArray();
                        ArrayList<DBIbeancon> upBeacons = new ArrayList<DBIbeancon>();
                        for (DBIbeancon ibeacon : publicData.beacons){
                            if(!publicData.uploadBeaconSet.contains(ibeacon.getBluetoothAddress()))
                                upBeacons.add(ibeacon);
                        }
                        for (DBIbeancon ibeacon : upBeacons) {
                            JSONObject jsonObject = new JSONObject();

                            jsonObject.put("mac", ibeacon.getBluetoothAddress());
                            jsonObject.put("major", String.valueOf(ibeacon.getMajor()));
                            jsonObject.put("minor", String.valueOf(ibeacon.getMinor()));
                            jsonObject.put("rssi", String.valueOf(ibeacon.getRssi()));
                            jsonObject.put("uuid", ibeacon.getProximityUuid());
                            jsonObject.put("id", publicData.getImei());
                            jsonObject.put("time", current_time);
                            jsonObject.put("building", "");
                            jsonObject.put("floor", "");
                            jsonObject.put("coord_x", "");
                            jsonObject.put("coord_y", "");
                            jsonObject.put("address", "");
                            jsonObject.put("latitude", ibeacon.getLatitude());
                            jsonObject.put("longitude", ibeacon.getLongitude());
                            jsonObject.put("status", "");
                            jsonObject.put("type", "1");
                            jsonArray.put(jsonObject);
                        }
                        JSONObject finalJson = new JSONObject();
                        finalJson.put("type", 1);
                        finalJson.put("data", jsonArray);
                        finalJson.put("user_id", publicData.getUser());
                        finalJson.put("key", publicData.getKey());
                        data = "jsonstr=" + finalJson.toString();
                        Log.d(getClass().getName(), data);
                        String upresult = doRequest(url, data, ahandler);
                        Log.d(getClass().getName(), upresult);

                        //通过上传返回的结果判断后续操作
                       /*
                       * 上报成功，1更新UI，2存储已上报的beacon到数据库，3删除数据库中未上报的beacon；
                       * 上报失败，1UI提示上报失败。
                       *
                       * */
                        JSONObject rejson = new JSONObject(upresult);
                        Message msg = new Message();
                        if (rejson.getString("success").contains("true")) {
                            //成功，更新界面数据,同时创建插入已上传的sql
                            for (IBeacon ibeacon : upBeacons) {
                                publicData.uploadBeaconSet.add(ibeacon.getBluetoothAddress());
                            }
                            PublicData.getInstance().removeUploadCheckedBeaconInDb();
                            msg.what = MyActivity.REQUEST_FINISH_SUCCESS;
                            ahandler.sendMessage(msg);
                        } else {
                            if (rejson.getString("message").contains("数据格式错误")) {
                                msg.what = MyActivity.REQUEST_FINISH_FAIL;
                                ahandler.sendMessage(msg);
                            } else if (rejson.getString("message").contains("tempid_time_out")) {
                                msg.what = MyActivity.KEY_TIME_OUT;
                                ahandler.sendMessage(msg);
                            }
                        }


                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else if (rtype.contains("login")) {//登录
                    Log.d(getClass().getName(),"开始登录："+LOGIN_URL);
                    //test
                    //message.what = LoginActivity.LOGIN_SUCCESS;
                    //PublicData.getInstance().setKey("asdasdasdf-asdasd-dfdf");
                    //ahandler.sendMessage(message);
                    //test
                    String id = PublicData.getInstance().getUser();
                    String psw = PublicData.getInstance().getPsw();
                    String data = String.format("user_id=%s&pwd=%s", id, PublicData.getInstance().getMd5(psw));
                    String login_result = null;
                    try {
                        URL url1 = new URL(LOGIN_URL);
                        login_result = doRequest(url1, data, ahandler);
                        Log.d(getClass().getName(), login_result);
                        JSONObject ljson = null;
                        ljson = new JSONObject(login_result);
                        if(ljson != null) {
                            if (ljson.getString("success").contains("true")) {
                                PublicData.getInstance().setKey(ljson.getString("key"));
                                message.what = LoginActivity.LOGIN_SUCCESS;
                            } else {
                                if (ljson.getString("message").contains("此用户已登陆")) {
                                    message.what = LoginActivity.LOGIN_FAIL_ALREADY_LOGIN;
                                } else if (ljson.getString("message").contains("不存在")) {
                                    message.what = LoginActivity.LOGIN_FAIL_NO_ACCOUNT;
                                } else if (ljson.getString("message").contains("错误")) {
                                    message.what = LoginActivity.LOGIN_FAIL_NO_ACCOUNT_AND_PSW;
                                } else {
                                    message.what = LoginActivity.SERVER_ERR;
                                }
                            }
                        }else message.what = LoginActivity.SERVER_ERR;
                        ahandler.sendMessage(message);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        message.what = LoginActivity.SERVER_ERR;
                        ahandler.sendMessage(message);
                        e.printStackTrace();
                    }
                } 

            }
        }

    }
}
