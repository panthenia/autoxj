package com.p.autoxj;

import android.graphics.Bitmap;
import com.lef.scanner.IBeacon;

/**
 * Created by p on 2015/1/23.
 */
public class DBIbeancon extends IBeacon {
    public DBIbeancon(){
        proximityUuid = "";
    }
    public DBIbeancon(IBeacon iBeacon){
        setMajor(iBeacon.getMajor());
        setMinor(iBeacon.getMinor());
        setUuid(iBeacon.getProximityUuid());
        setMac(iBeacon.getBluetoothAddress());
        setRssi(String.valueOf(iBeacon.getRssi()));
    }
    public String getAdress() {
        return adress;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }
    public void setMac(String mac){
        bluetoothAddress = mac;
    }
    String adress = "";
    String coordx = "";

    public String getBeaconDeployType() {
        return beaconDeployType;
    }

    public void setBeaconDeployType(String beaconDeployType) {
        this.beaconDeployType = beaconDeployType;
    }

    String beaconDeployType = "";
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    String status = "2";
    public String getIsour() {
        return isour;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
        setProximityUuid(uuid);
    }

    public String uuid = "";
    public String rssi = "";
    public void setIsour(String isour) {
        this.isour = isour;
    }

    String isour = "";

    public String getCoordy() {
        return coordy;
    }

    public void setCoordy(String coordy) {
        this.coordy = coordy;
    }

    public String getCoordx() {
        return coordx;
    }

    public void setCoordx(String coordx) {
        this.coordx = coordx;
    }

    String coordy = "";
    public void setMajor(String major){
        super.setMajor(Integer.valueOf(major));
    }
    public void setMinor(String minor){
        super.setMinor(Integer.valueOf(minor));
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String address = "";
    public String building = "";
    public String longitude;

    public String getLatitude() {
        return latitude;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap bitmap = null;
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String latitude;
    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String floor = "";

}
