package com.tevapharm.automation.dhp.tests.configuration.partner;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.DataProviders;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.IpAddressUtils;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;


@Listeners(TestListeners.class)
public class GetApiKeyDetailsPositiveTest extends PartnerApiTestBase {

    @Test(priority = 1, testName = "Generate API key with different valid IP octal", dataProvider = "getValidIPOctals", dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1654"})
    public void tc01_getApiKeyDetailsValidIp(String ip) throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), ip);


        extentTest.info("My IP is - " + IpAddressUtils.getPublicIpAddress());
        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        String partnerID = createPartner(extentTest, partnerRequest);

        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);
        apiKeyRequest.ipAddresses[0] = ip;

        extentTest.info("Generate a new API key with different IP octal");
        Response response2 = given().log().all()
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

        extentTest.info("Get the created API key details");
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
        System.out.println("Now Date Is - " + date.format(formatter));
        String localDate = date.format(formatter);
        String localMonth = localDate.substring(5, 7);
        String localDay = localDate.substring(8, 10);


        JsonPath json = response3.jsonPath();
        String grantAccessDate = json.get("apiKey.grantAccessDate");
        System.out.println("answer is " + grantAccessDate);
        String grantAccessFullDate = grantAccessDate.substring(0, 10);
        String grantAccessYearStr = grantAccessDate.substring(0, 4);
        int grantAccessYearInt = Integer.parseInt(grantAccessYearStr);

        TevaAssert.assertEquals(extentTest, localDate, grantAccessFullDate, "Grant access date is present");

        String grantExpirationDate = json.get("apiKey.grantExpirationDate");
        String grantExpirationYearStr = grantExpirationDate.substring(0, 4);
        int grantExpirationYearInt = Integer.parseInt(grantExpirationYearStr);
        String grantExpirationMonth = grantExpirationDate.substring(5, 7);
        String grantExpirationDay = grantExpirationDate.substring(8, 10);

        TevaAssert.assertEquals(extentTest, (grantAccessYearInt + 1), grantExpirationYearInt, "grant expiration date is equal to 12 months from today");
        TevaAssert.assertEquals(extentTest, localMonth, grantExpirationMonth, "grant expiration month is present");
        TevaAssert.assertEquals(extentTest, localDay, grantExpirationDay, "grant expiration day is present");


        //Request scopes list
        List<String> requestScopes = new ArrayList<>();
        requestScopes.add("data:inhalation:read");
        requestScopes.add("data:inhaler:read");
        requestScopes.add("data:dsa:read");
        requestScopes.add("data:connection:delete");
        requestScopes.add("data:api:partner:read");
        requestScopes.add("data:api:key:read");
        requestScopes.add("data:api:key:create");


        //Get scopes for the api keys we generated
        List<String> scopes = json.getList("apiKey.scopes");
        TevaAssert.assertEquals(extentTest, scopes, requestScopes, "All API key scopes are displayed in HTTP Response");

        TevaAssert.assertEquals(extentTest, response3.getStatusCode(), 200, "Request is expected to have HTTP Response Code `200`");

    }
}
