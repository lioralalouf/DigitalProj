package com.tevapharm.automation.dhp.tests.configuration.partner;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class GetListOfPartnersPositiveTest extends PartnerApiTestBase {


    @Test(priority = 1, testName = "Create multiple partners and get a list of these partners, All partner should be displayed")
    @Traceability(FS = {"1601"})
    public void tc01_getPartnersList() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Create partner number 1");
        String partnerID = createPartner(extentTest);

        extentTest.info("Create partner number 2");
        String partnerID2 = createPartner(extentTest);

        extentTest.info("Get the list of partners");
        Response response = given().baseUri(PropertyUtils.readProperty("adminUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/configuration/partners")
                .then()
                .log()
                .all()
                .extract().response();

        extentTest.info("Verify getting both partners in the HTTP Response ");
        String responseAsStr = response.getBody().asString();
        TevaAssert.assertTrue(extentTest, responseAsStr.contains(partnerID), "Partner number 1 is in the list");
        TevaAssert.assertTrue(extentTest, responseAsStr.contains(partnerID2), "Partner number 2 is in the list");
    }
}
