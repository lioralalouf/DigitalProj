package com.tevapharm.attte.generators;


import com.tevapharm.attte.models.database.MobileApplication;

import java.util.UUID;

public class MobileApplicationGenerator {

    public static MobileApplication getDigihaler() {
        MobileApplication mobileApplication = new MobileApplication();

        mobileApplication.setAppName("Digihaler");
        mobileApplication.setAppVersionNumber("1.0");
        mobileApplication.setOperatingSystem("IOS");
        mobileApplication.setUUID(UUID.randomUUID().toString());

        return mobileApplication;
    }

    public static MobileApplication getATTTE() {
        MobileApplication mobileApplication = new MobileApplication();

        mobileApplication.setAppName("ATTE");
        mobileApplication.setAppVersionNumber("1.0");
        mobileApplication.setOperatingSystem("IOS");
        mobileApplication.setUUID(UUID.randomUUID().toString());

        return mobileApplication;
    }

}
