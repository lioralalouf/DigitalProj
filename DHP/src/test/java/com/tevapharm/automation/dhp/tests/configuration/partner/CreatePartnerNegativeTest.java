package com.tevapharm.automation.dhp.tests.configuration.partner;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.DataProviders;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;


@Listeners(TestListeners.class)
public class CreatePartnerNegativeTest extends PartnerApiTestBase {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final ExtentReports extent = ExtentManager.createInstance();

    @Test(priority = 1, testName = "Onboard partner with invalid rate", dataProvider = "getLimits",
            dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1599", "1604"})
    public void tc01_createPartnerInvalidRate(int rate) throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), rate);

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.throttle.rate = rate;

        extentTest.info("Onboard a new partner with invalid rate");
        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json").request()
                .body(partnerRequest).when().log().all().post("/configuration/partners").then().log().all().assertThat()
                .statusCode(400).extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 400, "Request is expected to have HTTP Response Code `400`");
    }

    @Test(priority = 2, testName = "Onboard partner with invalid burst",
            dataProvider = "getLimits", dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1599", "1604"})
    public void tc02_createPartnerInvalidBurst(int burst) throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), burst);

        extentTest.info("Onboard a new partner with invalid burst");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.throttle.burst = burst;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .request()
                .body(partnerRequest).when().log().all().post("/configuration/partners").then().log().all().assertThat()
                .statusCode(400).extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 400, "Request is expected to have HTTP Response Code `400`");
    }

    @Test(priority = 3, testName = "Onboard partner with invalid limit", dataProvider = "getLimits",
            dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1599", "1604"})
    public void tc03_createPartnerInvalidLimit(int limit) throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), limit);
        extentTest.info("Onboard a new partner with invalid limit");

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.quota.limit = limit;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .request()
                .body(partnerRequest).when().log().all().post("/configuration/partners").then().log().all().assertThat()
                .statusCode(400).extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 400, "Request is expected to have HTTP Response Code `400`");
    }

    @Test(priority = 4, testName = "Onboard partner with invalid period parameters", dataProvider = "getPeriodFalse",
            dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1599", "1604"})
    public void tc04_createPartnerInvalidPeriod(String period) throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), period);
        extentTest.info("Onboard a new partner with invalid period");

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.quota.period = period;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json").request()
                .body(partnerRequest).when().log().all().post("/configuration/partners").then().log().all().assertThat()
                .statusCode(400).extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 400, "Request is expected to have HTTP Response Code `400`");
    }

    @Test(priority = 5, testName = "Onboard partner with duplicated name")
    @Traceability(FS = {"1599", "1604"})
    public void tc05_createPartnerDuplicatedName() throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.quota.period = "week";
        String partnerName = partnerRequest.name;
        extentTest.pass("Onboard a new partner, partner name equals to - " + partnerName);

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json").request()
                .body(partnerRequest).when().log().all().post("/configuration/partners").then().log().all().assertThat()
                .statusCode(200).extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 200, "Request is expected to have HTTP Response Code `200`");

        JsonPath js = new JsonPath(response.asString());

        this.registerPartnerID(js.getString("partnerID"));

        extentTest.info("Onboard same partner again, partner name equals also to - " + partnerName);
        Response response2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json").request()
                .body(partnerRequest).when().log().all().post("/configuration/partners").then().log().all().assertThat()
                .statusCode(400).extract().response();

        TevaAssert.assertEquals(extentTest, response2.getStatusCode(), 400, "Request is expected to have HTTP Response Code `400`");
    }
}
