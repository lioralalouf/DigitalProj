package com.tevapharm.automation.dhp.tests.configuration.consent;


import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.request.OnboardOrganizationRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class OnboardOrganizationConsentNegativeTest extends PartnerApiTestBase {

    private String organizationID = "";

    @Test(priority = 1, testName = "Getting consents for organization with no consents")
    @Traceability(FS = {"1627", "1629", "1659"})
    public void tc01_getNoConsents() throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        //Create organization
        OnboardOrganizationRequest organizationRequest = objectMapper.readValue(PropertyUtils.readRequest("organization", "onboardOrganization"),
                OnboardOrganizationRequest.class);

        organizationRequest.mnemonic = UUID.randomUUID().toString();

        Response responseOrganization = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .request()
                .body(organizationRequest)
                .when()
                .log().all()
                .post("/configuration/organizations")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath js = responseOrganization.jsonPath();
        this.organizationID = js.get("organizationID");


        extentTest.info("Get consents for the organization, Expecting HTTP Response Error code '404' for no existing consents for the organization");
        Response responseGet = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("organizationID", organizationID)
                .log().all()
                .get(PropertyUtils.readProperty("getConsentsUrl_organization"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, responseGet.getStatusCode(), 404, "Getting HTTP Response Error code 404 for no existing consents for the organization");

    }

    @Test(priority = 2, testName = "Onboard file with no content or mismatched consent legalType")
    @Traceability(FS = {"1627", "1629", "1659"})
    public void tc02_getMismatchedTypeOrNoContent() throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Uploading privacy notice file for the organization with empty content");
        //upload privacy notice for the organization
        File pnTxt = new File("./icons/privacyNotice.txt");
        Response responseFile = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("organizationID", organizationID)
                .multiPart("file", pnTxt, "text/plain")
                .log().all()
                .post(PropertyUtils.readProperty("onboardPn01ConsentUrl_organization"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, responseFile.getStatusCode(), 400, "Getting HTTP Response Error code 400 for no proper content");

        extentTest.info("Uploading privacy notice file for the organization with empty content");
        //upload privacy notice for the organization
        File pnTxt2 = new File("./icons/privacyNotice.txt");
        Response responseFile2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("organizationID", organizationID)
                .multiPart("file", pnTxt2, "text/plain")
                .log().all()
                .post(PropertyUtils.readProperty("onboardPn01ConsentUrl_organization"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, responseFile2.getStatusCode(), 400, "Getting HTTP Response Error code 400 for no proper content");
    }
}
