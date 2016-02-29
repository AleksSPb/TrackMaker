package com.company;

import java.util.Date;

import static java.lang.Math.*;

/**
 * Created by aleks on 28.02.2016.
 */
public class GPS_Point {
    long objID;
    long pointNum;
    Double plainX;
    Double plainY;
    Double latitude;
    Double longitude;
    Double elevation;
    Long millesecundFromLast = 0L;
    Date date;
    String note;
    byte type = 0;

    public GPS_Point(long eldID, long pointNum, Double plainX, Double plainY, Double latitude, Double longitude, Double elevation, String note) {
        this.objID = eldID;
        this.pointNum = pointNum;
        this.plainX = plainX;
        this.plainY = plainY;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.note = note;
    }

    public String toString() {
        return "Point num " + objID + ":" + pointNum + ", lat " + latitude + " , lon " + longitude + ";" + note + "\n";
    }

    public Double distance(GPS_Point toPoint) {
        return sqrt(pow((toPoint.getPlainX() - plainX),2) + pow((toPoint.getPlainY() - plainY),2));
    }
    public long getObjID() {
        return objID;
    }

    public void setObjID(long objID) {
        this.objID = objID;
    }

    public long getPointNum() {
        return pointNum;
    }

    public void setPointNum(long pointNum) {
        this.pointNum = pointNum;
    }

    public Double getPlainX() {
        return plainX;
    }

    public void setPlainX(Double plainX) {
        this.plainX = plainX;
    }

    public Double getPlainY() {
        return plainY;
    }

    public void setPlainY(Double plainY) {
        this.plainY = plainY;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }
}
