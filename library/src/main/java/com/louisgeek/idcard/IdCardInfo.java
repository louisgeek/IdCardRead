package com.louisgeek.idcard;

import android.graphics.Bitmap;

public class IdCardInfo {
    public String name;
    //性别
    public String gender;
    public String folk;
    //CCYYMMDD
    public String birthday;
    public String address;
    //身份证号
    public String id;
    //发证机关
    public String agency;
    //CCYYMMDD
    public String expireStart;
    //CCYYMMDD 或“长期”
    public String expireEnd;
    public Bitmap photo;
    //fingerPrint == 1024 ? "有指纹" : "无指纹"
    public int fingerPrint;
    @Deprecated
    public String englishName;
    //67 代表二代身份证
    @Deprecated
    public int cardType = 67;
    //消息
    public int code;
    public String message;
}
