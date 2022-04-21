package com.tevapharm.automation.dhp.tests.configuration.image;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.given;


@Listeners(TestListeners.class)
public class UploadImageNegativeTest extends PartnerApiTestBase {

    @Test(priority = 1, testName = "Onboard a file that isn't an image file")
    @Traceability(FS = {"1702"})
    public void tc01_uploadInvalidImageType() throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Onboard a txt file as an image file to S3 Bucket, Expect HTTP Response error code '400', because Image must be of image type");
        File InvalidImageFile = new File("./documents/privacyNotice.txt");

        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .multiPart("file", InvalidImageFile, "text/plain")
                .log().all()
                .post("/configuration/image")
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 400, "Image must be of type image");
    }
}
