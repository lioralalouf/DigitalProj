package com.tevapharm.automation.dhp.tests.dss.data;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.database.PartnerKey;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.repository.PartnerRepository;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.LambdaUtils;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.utils.TevaAssert;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
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
public class ExpiredApiKeyPositiveTest extends PartnerApiTestBase {
    private final PartnerRepository partnerRepository = new PartnerRepository();

    @Test(priority = 1, testName = "Check that API key is expired successfully exactly after 12 months")
    @Traceability(FS = {"1653"})
    public void tc01_keyExpirationTrue() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Onboard A new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        String partnerID = createPartner(extentTest, partnerRequest);

        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);

        extentTest.info("Generate A new API key for onboarded partner");
        Response response2 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .basePath("configuration/partners")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .request()
                .body(apiKeyRequest)
                .when()
                .log().all()
                .post("/{partnerID}/key")
                .then()
                .log().all()
                .extract().response();

        JsonPath extractor = response2.jsonPath();
        String apiKey = extractor.get("apiKey");
        TevaAssert.assertNotNull(extentTest, apiKey, "");
        TevaAssert.assertEquals(extentTest, response2.getStatusCode(), 200, "API key has been generated successfully");

        List<PartnerKey> partnerKeys = partnerRepository.findApiKeyByPartnerID(partnerID);
        registerApiKey(partnerID, apiKey);
        TevaAssert.assertNotNull(extentTest, apiKey, "");

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate yearBack = today.minusDays(365);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String y = yearBack.format(formatter);


        extentTest.info("Set API key setGrantAccessTimestamp in DB 1 year back");
        PartnerKey partnerKey = partnerKeys.get(0);
        Assert.assertNotNull(partnerKey);
        partnerKey.setGrantAccessDate(y);
        partnerKey.setGrantAccessTimestamp(y);
        partnerRepository.persistPartnerKey(partnerKey);

        List<PartnerKey> updateKeys = partnerRepository.findApiKeyByPartnerID(partnerID);
        PartnerKey updatedPartnerKey = updateKeys.get(0);
        Assert.assertEquals(y, updatedPartnerKey.getGrantAccessDate());

       LambdaUtils.invoke(PropertyUtils.readProperty("revokeExpiredApiKeys"));


        extentTest.info("Get the API key details, Expecting HTTP Response error code '401', because API Key doesnt exist");
        Response response3 = given().log().all()
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

        TevaAssert.assertEquals(extentTest, response3.getStatusCode(), 401, "Expecting HTTP Response error code '401', because Api Key doesnt exist");

    }

    @Test(priority = 2, testName = "Check that API key is NOT expired one day before expected expiration date")
    @Traceability(FS = {"1653"})
    public void tc02_keyExpirationFalse() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Onboard A new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        String partnerID = createPartner(extentTest, partnerRequest);

        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);

        extentTest.info("Generate A new API key for onboarded partner");
        Response response2 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .basePath("configuration/partners")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .request()
                .body(apiKeyRequest)
                .when()
                .post("/{partnerID}/key")
                .then()
                .log().all()
                .extract().response();

        JsonPath extractor = response2.jsonPath();
        String apiKey = extractor.get("apiKey");
        TevaAssert.assertNotNull(extentTest, apiKey, "");
        TevaAssert.assertEquals(extentTest, response2.getStatusCode(), 200, "API key has been generated successfully");

        List<PartnerKey> partnerKeys = partnerRepository.findApiKeyByPartnerID(partnerID);
        registerApiKey(partnerID, apiKey);
        TevaAssert.assertNotNull(extentTest, apiKey, "");


        extentTest.info("Set API key setGrantAccessTimestamp in DB 364 days back");
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate yearBack = today.minusDays(364);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String y = yearBack.format(formatter);
        System.out.println("updatedDate is " + y);

        PartnerKey partnerKey = partnerKeys.get(0);
        Assert.assertNotNull(partnerKey);
        partnerKey.setGrantAccessDate(y);
        partnerKey.setGrantAccessTimestamp(y);
        partnerRepository.persistPartnerKey(partnerKey);

        List<PartnerKey> updateKeys = partnerRepository.findApiKeyByPartnerID(partnerID);
        PartnerKey updatedPartnerKey = updateKeys.get(0);
        Assert.assertEquals(y, updatedPartnerKey.getGrantAccessDate());

        LambdaUtils.invoke(PropertyUtils.readProperty("revokeExpiredApiKeys"));

        extentTest.info("Get the created API key details");
        Response response3 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .when()
                .log().all()
                .get("/data/api/key")
                .then()
                .log()
                .all()
                .extract()
                .response();

        TevaAssert.assertEquals(extentTest, response2.getStatusCode(), 200, "API key has been generated successfully");

    }

}
