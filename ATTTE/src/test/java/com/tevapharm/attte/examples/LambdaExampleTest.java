package com.tevapharm.attte.examples;


import com.tevapharm.attte.utils.LambdaUtils;
import com.tevapharm.attte.utils.PropertyUtils;

import java.util.Map;

public class LambdaExampleTest {

    public static void main(String args[]) {
        Map<String,String> environment = LambdaUtils.getLambdaConfiguration(PropertyUtils.readProperty("getPatientInhalations"));

        environment.put("volumeTransferEnabled","false");
        environment.put("peakFlowTransferEnabled","false");

        LambdaUtils.updateLambdaConfiguration(PropertyUtils.readProperty("getPatientInhalations"), environment);

    }
}
