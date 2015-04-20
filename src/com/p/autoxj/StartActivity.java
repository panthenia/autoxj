package com.p.autoxj;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by p on 2015/4/17.
 */
public class StartActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PublicData.getInstance().start_activity = this;
        SharedPreferences preferences = getSharedPreferences(getResources().getString(R.string.check_login_preference),MODE_PRIVATE);
        if(preferences.getBoolean("FirstRun", true)){
            Intent intent = new Intent(StartActivity.this,LoginActivity.class);
            startActivityForResult(intent,6);
        }else{
            getSavedInfo();
            Intent intent = new Intent(StartActivity.this,MyActivity.class);
            startActivity(intent);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("return", "resultCode" + requestCode);
        if (requestCode == 6 && resultCode == 5) {
            Intent intent = new Intent(StartActivity.this,MyActivity.class);
            startActivity(intent);
        }else if (requestCode == 6 && resultCode == 2){
            finish();
        }

    }
    public void getSavedInfo(){
        SharedPreferences preferences = getSharedPreferences(getResources().getString(R.string.login_preference_name),MODE_PRIVATE);
        if(preferences.getBoolean("SaveInfo", false)){
            PublicData.getInstance().setIp(preferences.getString("Ip",""));
            PublicData.getInstance().setPort(preferences.getString("Port", ""));
            PublicData.getInstance().setHas_save_ip(true);
        }else PublicData.getInstance().setHas_save_ip(false);
        if(preferences.getBoolean("SaveUser", false)){
            PublicData.getInstance().setUser(preferences.getString("User", ""));
            PublicData.getInstance().setPsw(preferences.getString("Psw", ""));
            PublicData.getInstance().setHas_save_user(true);
        }else PublicData.getInstance().setHas_save_user(false);

    }
}