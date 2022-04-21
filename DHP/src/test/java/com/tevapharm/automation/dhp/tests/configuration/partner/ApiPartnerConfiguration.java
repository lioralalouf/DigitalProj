package com.tevapharm.automation.dhp.tests.configuration.partner;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.generators.MobileApplicationGenerator;
import com.tevapharm.attte.generators.ProfileGenerator;
import com.tevapharm.attte.models.database.MobileApplication;
import com.tevapharm.attte.models.database.Profile;
import com.tevapharm.attte.models.request.ConnectRequest;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.account.ProfileCreationRequest;
import com.tevapharm.attte.models.request.mobiledevice.RegisterMobileApplicationRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.utils.TevaAssert;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class ApiPartnerConfiguration extends PartnerApiTestBase {
    private String apiKey;
    private String partnerID;

    @Test(priority = 1, testName = "Onboard and offboard partner")
    @Traceability(URS = {"1589"})
    public void tc01_partnerConfiguration() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Onboard a new partner");
        this.partnerID = createPartner(extentTest);

        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);

        extentTest.info("Generate a new API key to onboarded partner");
        Response response2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .basePath("configuration/partners")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .body(apiKeyRequest)
                .when()
                .post("/{partnerID}/key")
                .then()
                .log().all()
                .extract().response();

        JsonPath extractor = response2.jsonPath();
        this.apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);
        TevaAssert.assertNotNull(extentTest, apiKey, "");

        extentTest.info("Getting partner details");
        Response response3 =
                given()
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
                        .assertThat().statusCode(200)
                        .extract().response();

        JsonPath extractor2 = response3.jsonPath();
        String ResponsePartnerID = extractor2.get("partnerID").toString();
        extentTest.info("Verify HTTP Response status code is '200'");
        TevaAssert.assertEquals(extentTest, response3.getStatusCode(), 200, "Request is expected to have HTTP Response Code `200`");

        extentTest.info("Verify partner Id is identical to the onboarded partner ID");
        TevaAssert.assertEquals(extentTest, ResponsePartnerID, partnerID, "Partner Id should be identical to the original");

        extentTest.info("Verify all fields are displayed in the HTTP Response");
        TevaAssert.assertNotNull(extentTest, extractor2.get("callbacks.success"), "The callbacks.success should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("callbacks.failure"), "The callbacks.failure should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("redirects.success"), "The redirects.success should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("redirects.failure"), "The redirects.failure should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("icon"), "The icon should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("contact.firstName"), " The firstName should be present.");
        TevaAssert.assertNotNull(extentTest, extractor2.get("contact.lastName"), "Last name should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("contact.phoneNumber"), "The phone number should be present.");
        TevaAssert.assertNotNull(extentTest, extractor2.get("contact.email"), "The email should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("throttle.rate"), " The rate should be present.");
        TevaAssert.assertNotNull(extentTest, extractor2.get("throttle.burst"), "The burst should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("quota.limit"), " The limit should be present.");
        TevaAssert.assertNotNull(extentTest, extractor2.get("quota.period"), "The period should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("usagePlanID"), "The usagePlanID should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("activePrivacyNoticeVersion"), " The activePrivacyNoticeVersion should be present.");
        TevaAssert.assertNotNull(extentTest, extractor2.get("activeMarketingConsent"), "The burst should be present");
        TevaAssert.assertNotNull(extentTest, extractor2.get("activeSignature"), " The limit should be present.");
        TevaAssert.assertNotNull(extentTest, extractor2.get("activeHipaaDisclosure"), "The period should be present");

        extentTest.info("Revoke the partner's API key");
        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .basePath("configuration/partners")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .pathParam("apiKey", apiKey)
                .when()
                .delete("/{partnerID}/key/{apiKey}")
                .then()
                .log().all()
                .extract().response();

        extentTest.info("Delete partner");
        Response response4 = given()
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

        TevaAssert.assertEquals(extentTest, response4.getStatusCode(), 200, "Request is expected to have HTTP Response Code `200`");

        extentTest.info("Get the deleted partner details");
        Response response5 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .basePath("configuration/partners")
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .when()
                .log()
                .all()
                .when().get("/{partnerID}")
                .then()
                .log().all().
                extract().response();

        TevaAssert.assertEquals(extentTest, response5.getStatusCode(), 404, "Request is expected to have HTTP Response Code `404`");
    }
}
