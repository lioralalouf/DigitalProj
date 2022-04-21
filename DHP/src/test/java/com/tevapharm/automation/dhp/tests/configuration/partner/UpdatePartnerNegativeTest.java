package com.tevapharm.automation.dhp.tests.configuration.partner;

import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.DataProviders;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.utils.TevaAssert;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class UpdatePartnerNegativeTest extends PartnerApiTestBase {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Test(priority = 1, testName = "Update partner with invalid rate", dataProvider = "getLimits", dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1600"})
    public void tc01_updatePartnerInvalidRate(int rate) throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), rate);

        extentTest.pass("This is a test with parameters");
        extentTest.info("Onboard a new partner with invalid rate");
        String partnerID = createPartner(extentTest);
        extentTest.info("Update partner with invalid rate");
        Response response = updateInvalidRate(extentTest,partnerID, rate);
        int code = response.getStatusCode();
        TevaAssert.assertEquals(extentTest, code, 400, "Request is expected to have HTTP Response Code `400`");
    }

    @Test(priority = 2, testName = "Update partner with invalid burst", dataProvider = "getLimits", dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1600"})
    public void tc02_updatePartnerInvalidBurst(int burst) throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), burst);

        extentTest.pass("This is a test with parameters");
        extentTest.info("Onboard a new partner");
        String partnerID = createPartner(extentTest);
        extentTest.info("Update partner with invalid burst");
        Response response = updateInvalidBurst(extentTest,partnerID, burst);
        int code = response.getStatusCode();
        TevaAssert.assertEquals(extentTest, code, 400, "Request is expected to have HTTP Response Code `400`");
    }

    @Test(priority = 3, testName = "Update partner with invalid limit", dataProvider = "getLimits", dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1600"})
    public void tc03_updatePartnerInvalidLimit(int limit) throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), limit);

        extentTest.pass("This is a test with parameters");
        extentTest.info("Onboard a new partner");
        String partnerID = createPartner(extentTest);
        extentTest.info("Update partner with invalid limit");
        Response response = updateInvalidLimit(extentTest,partnerID, limit);
        int code = response.getStatusCode();
        TevaAssert.assertEquals(extentTest, code, 400, "Request is expected to have HTTP Response Code `400`");
    }

    @Test(priority = 4, testName = "Update partner with invalid period", dataProvider = "getPeriodFalse", dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1600"})
    public void tc04_updatePartnerInvalidPeriod(String period) throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), period);

        extentTest.pass("This is a test with parameters");
        extentTest.info("Onboard a new partner");
        String partnerID = createPartner(extentTest);
        extentTest.info("Update partner with invalid period");
        Response response = updateInvalidPeriod(extentTest,partnerID, period);
        int code = response.getStatusCode();
        TevaAssert.assertEquals(extentTest, code, 400, "Request is expected to have HTTP Response Code `400`");
    }

    public static Response updateInvalidRate(ExtentTest  extentTest,String partnerID, int rate) throws IOException {
        String accessToken = RestAssuredOAuth.getToken();

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.throttle.rate = rate;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given().basePath("configuration/partners")
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
                .pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
                .then().log().all().extract().response();
        return response;

    }

    public Response updateInvalidBurst(ExtentTest  extentTest,String partnerID, int burst) throws IOException {
        String accessToken = RestAssuredOAuth.getToken();

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.throttle.burst = burst;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given().basePath("configuration/partners")
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
                .pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
                .then().log().all().extract().response();
        return response;
    }

    public Response updateInvalidLimit(ExtentTest  extentTest,String partnerID, int limit) throws IOException {
        String accessToken = RestAssuredOAuth.getToken();

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.quota.limit = limit;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given().basePath("configuration/partners")
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
                .pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
                .then().log().all().extract().response();
        return response;
    }

    public Response updateInvalidPeriod(ExtentTest  extentTest,String partnerID, String period) throws IOException {
        String accessToken = RestAssuredOAuth.getToken();

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.quota.period = period;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given().basePath("configuration/partners")
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
                .pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
                .then().log().all().extract().response();
        return response;
    }
}
