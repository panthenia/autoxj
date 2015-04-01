package com.p.autoxj;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


/**
 * Created by p on 2015/3/3.
 */
public class ScanView extends View {
    Paint mPaint = null;
    Paint wpaint1 = null;
    Paint wpaint2 = null;
    Paint wpaint3 = null;
    Paint wpaint4 = null;
    int raduis = 80;
    int alpha = 250;
    int findNum = PublicData.getInstance().beacons.size();
    boolean flag = true;
    boolean findNewBeacon = false;
    double lastDistance = 0;
    public void setLogin(boolean isLogin) {
        this.isLogin = isLogin;
    }
    Path path = new Path();
    boolean isLogin = true;
    int newBeaconAlpha = 255;
    public ScanView(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
        mPaint = new Paint();
        wpaint1 = new Paint();
        wpaint2 = new Paint();
        wpaint3 = new Paint();
        wpaint4 = new Paint();
        mPaint.setColor(0x0180ff);
        mPaint.setAlpha(alpha);
        mPaint.setAntiAlias(true);
        wpaint1.setColor(0xff0180ff);
        wpaint1.setAntiAlias(true);
        wpaint2.setAntiAlias(true);
        wpaint3.setAntiAlias(true);
        wpaint2.setStyle(Paint.Style.STROKE);
        wpaint3.setStyle(Paint.Style.STROKE);
        wpaint4.setStyle(Paint.Style.STROKE);
        wpaint2.setColor(0xff0180ff);
        wpaint3.setColor(0xff0180ff);
        wpaint4.setColor(0xffff0000);
        wpaint2.setTextSize(50);
        wpaint3.setTextSize(45);
        wpaint4.setTextSize(35);
        wpaint1.setStrokeWidth(10);
        wpaint1.setTextSize(100);
        wpaint2.setTextAlign(Paint.Align.CENTER);
        wpaint1.setTextAlign(Paint.Align.CENTER);
        wpaint3.setTextAlign(Paint.Align.CENTER);
        wpaint4.setTextAlign(Paint.Align.CENTER);
        wpaint3.setAlpha(newBeaconAlpha);
    }
    public void setFindNum(int newNum){
        findNum = newNum;
    }
    public void setFindNewBeacon(){
        findNewBeacon = true;
        newBeaconAlpha = 255;
    }
    public void rePaint(){
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getPointerCount() > 2){
            path.reset();
            path.moveTo(event.getX(0),event.getY(0));
            for(int i=1;i<event.getPointerCount();++i){
                path.lineTo(event.getX(i),event.getY(i));
            }
            path.lineTo(event.getX(0),event.getY(0));
            invalidate();
        }
        //两点触摸
//        if(event.getPointerCount() == 2){
//            switch (event.getActionMasked()){
//                case MotionEvent.ACTION_POINTER_DOWN:
//                    lastDistance = distanceBetweenFingers(event);
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    double newDistance = distanceBetweenFingers(event);
//                    raduis = (int) (raduis * (newDistance / lastDistance));
//                    if(raduis < 1)
//                        raduis = 1;
//                    if(raduis > (getWidth()/3))
//                        raduis = getWidth()/3;
//                    lastDistance = newDistance;
//                    Log.d(getClass().getName(),String.valueOf(raduis));
//                    invalidate();
//            }
//        }
        //return super.onTouchEvent(event);
        return true;
    }
    private double distanceBetweenFingers(MotionEvent event) {
        float disX = Math.abs(event.getX(0) - event.getX(1));
        float disY = Math.abs(event.getY(0) - event.getY(1));
        return Math.sqrt(disX * disX + disY * disY);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        if(raduis > getWidth()/3){
//            raduis = 10;
//            alpha = 250;
//        }
//        else {
//            raduis += 10;
//            alpha -= 10;
//        }
        mPaint.setAlpha(alpha);
        canvas.drawPath(path,wpaint4);
        canvas.drawCircle(getWidth() / 2, (getHeight() / 8)*3, raduis, mPaint);
        canvas.drawText("已检测到Beacon数量",getWidth() / 2, (getHeight() / 8) *6,  wpaint2);
        canvas.drawText(String.valueOf(findNum), getWidth() / 2, (getHeight() / 8) *7, wpaint1);
        if(findNewBeacon){
            wpaint3.setAlpha(newBeaconAlpha);
            canvas.drawText("发现新Beacon！",getWidth() / 2,getHeight() / 8,wpaint3);
            newBeaconAlpha -= 5;
            if (newBeaconAlpha < 0){
                findNewBeacon = false;
                newBeaconAlpha = 250;
            }
        }
        if(!isLogin){
            canvas.drawText("尚未登录，将无法启动自动上传功能！",getWidth() / 2, getHeight() - 10,wpaint4);
        }
    }

}
