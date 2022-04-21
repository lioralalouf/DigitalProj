package com.tevapharm.attte.qualification;


import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.utils.TevaAssert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;

@Listeners(TestListeners.class)
public class AuthenticationTest {
    private String accessToken = "";
    private String partnerID = "";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(testName = "Requesting an access token returns an access token.")
    @Traceability(URS = {"1733"})
    public void tc01_Authentication_request() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        this.accessToken = RestAssuredOAuth.getToken();
        TevaAssert.assertNotNull(extentTest, this.accessToken, "Valid access token returned proving that the tool can be used for testing APIs which require authentication.");
        Thread.sleep(2000);
    }
}
