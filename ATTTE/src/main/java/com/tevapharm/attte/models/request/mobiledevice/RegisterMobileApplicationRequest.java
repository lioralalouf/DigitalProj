package com.tevapharm.attte.models.request.mobiledevice;

import com.tevapharm.attte.models.database.MobileApplication;
import com.tevapharm.attte.models.request.PlatformRequestBase;

import java.util.UUID;

public class RegisterMobileApplicationRequest extends PlatformRequestBase {

    public MobileDeviceDto mobileDevice;

    public RegisterMobileApplicationRequest(MobileApplication mobileApplication) {

        this.mobileDevice = new MobileDeviceDto();
        this.mobileDevice.UUID = mobileApplication.getUUID();
        this.mobileDevice.operatingSystem = mobileApplication.getOperatingSystem();
        this.mobileDevice.appName = mobileApplication.getAppName();
        this.mobileDevice.appVersionNumber = mobileApplication.getAppVersionNumber();
        this.mobileDevice.firebaseToken = UUID.randomUUID().toString();

    }
}
