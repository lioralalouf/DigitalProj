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
public class GetSpecificPartnerDetailsNegativeTest extends PartnerApiTestBase {

    @Test(priority = 1, testName = "Get partner details with not existing partner id")
    @Traceability(FS = {"1603"})
    public void tc01_getPartnerDetailsInvalidId() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Onboard a new partner");
        String partnerID = createPartner(extentTest);

        extentTest.info("Delete the partner");
        Response response;

        do {
            response = given()
                    .filter(new ConsoleReportFilter(extentTest))
                    .baseUri(PropertyUtils.readProperty("adminUrl"))
                    .basePath("configuration/partners")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .pathParam("partnerID", partnerID)
                    .when()
                    .delete("/{partnerID}")
                    .then()
                    .log().all()
                    .extract().response();

            if (response.getStatusCode() != 200) {
                Thread.sleep(20_000);
            }

        } while (response.getStatusCode() != 200);

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 200, "The partner has been deleted successfully");

        extentTest.info("Get the partner details, expecting HTTP Response error code '404', because partner with this id doesn't exist");
        Response response2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .basePath("configuration/partners")
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .when()
                .log().all()
                .when()
                .get("/{partnerID}")
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, response2.getStatusCode(), 404, "Request is expected to have HTTP Response Code `404`");
    }
}
