package com.louisgeek.idcard;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.Routon.iDRHIDLib.iDRHIDDev;

public class IdCardManager implements LifecycleObserver {
    private static final String TAG = "IdCardManager";
    private static final String ACTION_USB_PERMISSION = "com.Routon.HIDTest.USB_PERMISSION";
    private static final int USB_DEVICE_VENDOR_ID = 1061;
    private static final int USB_DEVICE_PRODUCT_ID = 33113;

    private static final int USB_OPEN_SUCCESS = 0;
    //参数不正确
    private static final int USB_OPEN_ERROR_PARAM = -1;
    //USB接口出错
    private static final int USB_OPEN_ERROR_INTERFACE = -2;
    //USB端点出错
    private static final int USB_OPEN_ERROR_PORT = -3;
    //USB连接出错
    private static final int USB_OPEN_ERROR_CONNECT = -4;

    private static final int AUTHENTICATE_SUCCESS = 0;
    //找卡失败
    private static final int AUTHENTICATE_ERROR_FIND = -1;
    //选卡失败
    private static final int AUTHENTICATE_ERROR_SELECT = -2;

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private iDRHIDDev mHIDDev;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    //
                    release();
                    //
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device == null) {
                        Log.d(TAG, "device is null");
                        return;
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.i(TAG, "已授权 ");
                        int code = mHIDDev.openDevice(mUsbManager, device);
                        if (code == USB_OPEN_SUCCESS) {
                            mDevice = device;
                            Log.i(TAG, "open device");
                        } else {
                            mDevice = null;
                            Log.i(TAG, "open device 失败 " + code);
                        }
                    } else {
                        Log.e(TAG, "permission denied for device " + device);
                    }
                }


            }
        }
    };
    private FragmentActivity mFragmentActivity;

    public void init(FragmentActivity fragmentActivity) {
        mFragmentActivity = fragmentActivity;
        mFragmentActivity.getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void create() {
        mHIDDev = new iDRHIDDev();
        mUsbManager = (UsbManager) mFragmentActivity.getSystemService(Context.USB_SERVICE);
        //
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_USB_PERMISSION);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        mFragmentActivity.registerReceiver(mUsbReceiver, intentFilter);
        //
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            Log.e(TAG, "vid: " + device.getVendorId() + " pid:" + device.getProductId());
            if (device.getVendorId() == USB_DEVICE_VENDOR_ID && device.getProductId() == USB_DEVICE_PRODUCT_ID) {
                Intent intent = new Intent(ACTION_USB_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(mFragmentActivity, 0, intent, 0);
                mUsbManager.requestPermission(device, pendingIntent);
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void destroy() {
        mFragmentActivity.unregisterReceiver(mUsbReceiver);
        release();
        mUsbManager = null;
    }

    public IdCardInfo readIdCard() {
        IdCardInfo idCardInfo = new IdCardInfo();
        if (mFragmentActivity == null) {
            Log.e(TAG, "IdCardManager: 未初始化！");
            idCardInfo.message = "IdCardManager 未初始化！";
            return idCardInfo;
        }
        if (mDevice == null) {
            Log.e(TAG, "readIdCard: 请插入读卡器！");
            idCardInfo.message = "请插入读卡器！";
            return idCardInfo;
        }
        //读安全模块的状态
        int samStaus = mHIDDev.GetSamStaus();
        if (samStaus < 0) {
            Log.e(TAG, "读卡器未准备好！");
            idCardInfo.message = "读卡器未准备好！";
        }
        iDRHIDDev.SamIDInfo samIDInfo = mHIDDev.new SamIDInfo();
        //读安全模块号
        int result = mHIDDev.GetSamId(samIDInfo);
        //卡认证接口  发现身份证卡并选择卡
        int code = mHIDDev.Authenticate();
        if (code == AUTHENTICATE_SUCCESS) {
            // 找到卡，然后读卡
            iDRHIDDev.SecondIDInfo secondIDInfo = mHIDDev.new SecondIDInfo();
            byte[] fingerPrint = new byte[1024];
//           读卡信息
            // code = mHIDDev.ReadBaseMsg(sIDInfo);
            code = mHIDDev.ReadBaseFPMsg(secondIDInfo, fingerPrint);
            if (code < 0) {
                //-1 发送命令出错，-2 接收数据出错，-3数据格式出错，-4 数据校验出错，-5 返回结果出错
                Log.e(TAG, "readIdCard:读卡失败 ");
                return null;
            }
            Log.e(TAG, secondIDInfo.fingerPrint == 1024 ? "有指纹" : "无指纹");
            //设置蜂鸣器和LED指示灯
            mHIDDev.BeepLed(true, true, 500);
            idCardInfo.message = "识别成功！";
            idCardInfo.code = 1;
            idCardInfo.name = secondIDInfo.name;
            idCardInfo.gender = secondIDInfo.gender;
            idCardInfo.folk = secondIDInfo.folk;
            idCardInfo.birthday = secondIDInfo.birthday;
            idCardInfo.address = secondIDInfo.address;
            idCardInfo.id = secondIDInfo.id;
            idCardInfo.agency = secondIDInfo.agency;
            idCardInfo.expireStart = secondIDInfo.expireStart;
            idCardInfo.expireEnd = secondIDInfo.expireEnd;
            idCardInfo.photo = secondIDInfo.photo;
            idCardInfo.fingerPrint = secondIDInfo.fingerPrint;
            idCardInfo.englishName = secondIDInfo.englishName;
            idCardInfo.cardType = secondIDInfo.cardType;
            return idCardInfo;
        } else {
            // 未找到卡
            iDRHIDDev.MoreAddrInfo moreAddrInfo = mHIDDev.new MoreAddrInfo();
            // 通过读追加地址来判断卡是否在机具上
            //读追加地址
            int size = mHIDDev.GetNewAppMsg(moreAddrInfo);
            if (size < 0) {
                // 机具上没有放卡
                Toast.makeText(mFragmentActivity, "未读取到数据，请放卡！", Toast.LENGTH_SHORT).show();
                return null;
            }
            Log.e(TAG, "readIdCard: addr0 " + moreAddrInfo.addr0);
            Log.e(TAG, "readIdCard: addr1 " + moreAddrInfo.addr1);
            Log.e(TAG, "readIdCard: addr2 " + moreAddrInfo.addr2);
            Log.e(TAG, "readIdCard: addr3 " + moreAddrInfo.addr3);
        }
        return idCardInfo;
    }

    private void release() {
        if (mDevice != null) {
            mHIDDev.closeDevice();
            mDevice = null;
        }
    }
}
