package com.tevapharm.automation.dhp.tests.configuration.partner;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.request.GenerateApiRequest;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class GenerateApiKeyPositiveTest extends PartnerApiTestBase {

    @Test(priority = 1, testName = "Generate a new API key to onboarded partner and getting its details")
    @Traceability(FS = {"1654", "1605"})
    public void tc01_generateNewPartnerApiKey() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        extentTest.info("Onboard a new partner");
        String partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Generate a new API key to onboarded partner and verify API key is not null in response");
        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);

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
        String apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);
        TevaAssert.assertNotNull(extentTest, apiKey, "");

        extentTest.info("Get the generated API key details");
        Response response3 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .when()
                .get("/data/api/key")
                .then()
                .log()
                .all()
                .extract()
                .response();

        LocalDate date = LocalDate.now(ZoneId.of("GMT"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String localDate = date.format(formatter);
        String localMonth = localDate.substring(5, 7);
        String localDay = localDate.substring(8, 10);


        JsonPath json = response3.jsonPath();
        String grantAccessDate = json.get("apiKey.grantAccessDate");
        System.out.println("answer is " + grantAccessDate);
        String grantAccessFullDate = grantAccessDate.substring(0, 10);
        String grantAccessYearStr = grantAccessDate.substring(0, 4);
        int grantAccessYearInt = Integer.parseInt(grantAccessYearStr);
        extentTest.info("Verify the access date is correct");
        TevaAssert.assertEquals(extentTest, localDate, grantAccessFullDate, "grant access date is correct");

        String grantExpirationDate = json.get("apiKey.grantExpirationDate");
        String grantExpirationYearStr = grantExpirationDate.substring(0, 4);
        int grantExpirationYearInt = Integer.parseInt(grantExpirationYearStr);
        String grantExpirationMonth = grantExpirationDate.substring(5, 7);
        String grantExpirationDay = grantExpirationDate.substring(8, 10);
        extentTest.info("Verify the grant expiration date is correct");
        TevaAssert.assertEquals(extentTest, (grantAccessYearInt + 1), grantExpirationYearInt, "Year of grant access is 1 year from now");
        TevaAssert.assertEquals(extentTest, localMonth, grantExpirationMonth, "grant expiration month is correct");
        TevaAssert.assertEquals(extentTest, localDay, grantExpirationDay, "grant expiration day is correct");


        List<Object> scopes = json.getList("apiKey.scopes");
        TevaAssert.assertEquals(extentTest, response3.getStatusCode(), 200, "Request is expected to have HTTP Response Code `200`");
        //System.out.println(scopes);
        extentTest.info("Verify scope field is not null and all scopes are presented");
        TevaAssert.assertNotNull(extentTest, scopes, "Scope field is not null");
        String responseAsStr = response3.getBody().asString();
        TevaAssert.assertTrue(extentTest, responseAsStr.contains("data:inhalation:read"), "data:inhalation:read is present in the payload.");
        TevaAssert.assertTrue(extentTest, responseAsStr.contains("data:inhaler:read"), "data:inhaler:read is present in the payload.");
        TevaAssert.assertTrue(extentTest, responseAsStr.contains("data:dsa:read"), "data:dsa:read is present in the payload.");
        TevaAssert.assertTrue(extentTest, responseAsStr.contains("data:connection:delete"), "data:inhalation:read is present in the payload.");
        TevaAssert.assertTrue(extentTest, responseAsStr.contains("ata:api:partner:read"), "data:inhalation:read is present in the payload.");
        TevaAssert.assertTrue(extentTest, responseAsStr.contains("data:api:key:read"), "data:inhalation:read is present in the payload.");
        TevaAssert.assertTrue(extentTest, responseAsStr.contains("data:api:key:create"), "data:inhalation:read is present in the payload.");

        //REVOKE API KEY AND DELETE PARTNER FROM DB
        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        given()
                //.filter(new ConsoleReportFilter(extentTest))
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

    }

}
