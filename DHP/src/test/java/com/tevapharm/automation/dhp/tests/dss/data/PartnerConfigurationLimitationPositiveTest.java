package com.tevapharm.automation.dhp.tests.dss.data;


import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.generators.InhalationGenerator;
import com.tevapharm.attte.generators.MedicalDeviceGenerator;
import com.tevapharm.attte.generators.MobileApplicationGenerator;
import com.tevapharm.attte.generators.ProfileGenerator;
import com.tevapharm.attte.models.database.Inhalation;
import com.tevapharm.attte.models.database.MedicalDevice;
import com.tevapharm.attte.models.database.MobileApplication;
import com.tevapharm.attte.models.database.Profile;
import com.tevapharm.attte.models.request.ConnectRequest;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.models.request.account.ProfileCreationRequest;
import com.tevapharm.attte.models.request.inhalation.UploadInhalationsRequest;
import com.tevapharm.attte.models.request.medicaldevice.RegisterMedicalDeviceRequest;
import com.tevapharm.attte.models.request.mobiledevice.RegisterMobileApplicationRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.utils.TevaAssert;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class PartnerConfigurationLimitationPositiveTest extends PartnerApiTestBase {

    private String stateToken;
    private Profile profile;
    private MedicalDevice medicalDevice;
    private MobileApplication mobileApplication;
    private String apiKey;
    private String partnerID;

    @Test(priority = 1, testName = "Send two requests to Get Inhalations prove there's a limit will stop more than 1 request")
    @Traceability(FS = {"1604"})
    public void tc01_LimitConfiguration() throws IOException, ParseException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        String patientID = UUID.randomUUID().toString();

        //create partner
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.throttle.rate = 1000;
        partnerRequest.throttle.burst = 100;
        partnerRequest.quota.limit = 1;
        partnerRequest.quota.period = "day";
        this.partnerID = createPartner(extentTest, partnerRequest);

        //generate api key
        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);
        Response response = given().log().all()
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

        JsonPath extractor = response.jsonPath();
        this.apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);
        //create account
        profile = ProfileGenerator.getProfile();
        ProfileCreationRequest profileCreationRequest = new ProfileCreationRequest(profile);

        given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("email", profile.getEmail())
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .body(profileCreationRequest)
                .when()
                .log().all()
                .post("/account/profile")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        // Need Mobile App
        mobileApplication = MobileApplicationGenerator.getATTTE();
        RegisterMobileApplicationRequest registerMobileApplicationRequest = new RegisterMobileApplicationRequest(mobileApplication);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .body(registerMobileApplicationRequest)
                .when()
                .log().all()
                .post("/application/mobile")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        // Need Digihaler
        medicalDevice = MedicalDeviceGenerator.getProAir();
        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .header("device-uuid", mobileApplication.getUUID())
                .request()
                .body(registerMedicalDeviceRequest)
                .when()
                .log().all()
                .post("/device/digihaler")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        //need inhalations

        List<Inhalation> inhalations = new ArrayList<>();

        Inhalation inhalation = InhalationGenerator.generateGoodInhalation(medicalDevice);
        inhalation.event.id = 1;
        inhalations.add(inhalation);
        Inhalation inhalation2 = InhalationGenerator.generateFairInhalation(medicalDevice);
        inhalation2.event.id = 2;
        inhalations.add(inhalation2);
        Inhalation inhalation3 = InhalationGenerator.generateGoodInhalation(medicalDevice);
        inhalation3.event.id = 3;
        inhalations.add(inhalation3);
        Inhalation inhalation4 = InhalationGenerator.generateFairInhalation(medicalDevice);
        inhalation4.event.id = 4;
        inhalations.add(inhalation4);

        UploadInhalationsRequest uploadInhalationsRequest = new UploadInhalationsRequest(inhalations);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .header("device-uuid", mobileApplication.getUUID())
                .request()
                .body(uploadInhalationsRequest)
                .when()
                .log().all()
                .post(" /medication/administration/inhalations")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();


        //provision
        Response response3 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .post("/data/provision/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath extractor2 = response3.jsonPath();
        this.stateToken = extractor2.get("stateToken");

        String provisionID = getProvisionID(stateToken);
        String partner = getPartnerID(stateToken);

        //account connect
        ConnectRequest connectRequest = objectMapper.readValue(PropertyUtils.readRequest("data", "connect"),
                ConnectRequest.class);
        connectRequest.connection.provisionID = provisionID;

        given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("Content-Type", "application/json")
                .header("External-Entity-Id", profile.getExternalEntityID())
                .header("Authorization", "Bearer " + accessToken)
                .body(connectRequest)
                .when().log().all()
                .post("/account/connect")
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(200)
                .extract().response();


        int i = 0;
        //get inhalation data
        for (; i < 100; i++) {

            Response getInhalationDateResponse = given().log().all()
                    .filter(new ConsoleReportFilter(extentTest))
                    .baseUri(PropertyUtils.readProperty("platformUrl"))
                    .header("X-API-Key", apiKey)
                    .pathParam("patientID", patientID)
                    .get("data/inhalation/{patientID}")
                    .then()
                    .log().all()
                    .assertThat()
                    .extract().response();

            if (getInhalationDateResponse.getStatusCode() == 429) {
                break;
            }
        }

        TevaAssert.assertNotEquals(extentTest, i, 100, "Partner quota of 1 reached before making 100 requests.");

    }

    @Test(priority = 2, testName = "Send 20 requests to get Inhalations in a loop prove the throttle will eventually reject the requests")
    @Traceability(FS = {"1604"})
    public void tc02_ThrottleConfiguration() throws IOException, ParseException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        String patientID = UUID.randomUUID().toString();

        //create partner
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.throttle.rate = 1;
        partnerRequest.throttle.burst = 1;
        partnerRequest.quota.limit = 10000;
        partnerRequest.quota.period = "day";
        this.partnerID = createPartner(extentTest, partnerRequest);

        //generate api key
        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);
        Response response = given().log().all()
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

        JsonPath extractor = response.jsonPath();
        this.apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);
        //create account
        profile = ProfileGenerator.getProfile();
        ProfileCreationRequest profileCreationRequest = new ProfileCreationRequest(profile);

        given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("email", profile.getEmail())
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .body(profileCreationRequest)
                .when()
                .log().all()
                .post("/account/profile")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        // Need Mobile App
        mobileApplication = MobileApplicationGenerator.getATTTE();
        RegisterMobileApplicationRequest registerMobileApplicationRequest = new RegisterMobileApplicationRequest(mobileApplication);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .body(registerMobileApplicationRequest)
                .when()
                .log().all()
                .post("/application/mobile")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        System.out.println(profile.getExternalEntityID());

        // Need Digihaler
        medicalDevice = MedicalDeviceGenerator.getProAir();
        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .header("device-uuid", mobileApplication.getUUID())
                .request()
                .body(registerMedicalDeviceRequest)
                .when()
                .log().all()
                .post("/device/digihaler")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        //need inhalations

        List<Inhalation> inhalations = new ArrayList<>();

        Inhalation inhalation = InhalationGenerator.generateGoodInhalation(medicalDevice);
        inhalation.event.id = 1;
        inhalations.add(inhalation);
        Inhalation inhalation2 = InhalationGenerator.generateFairInhalation(medicalDevice);
        inhalation2.event.id = 2;
        inhalations.add(inhalation2);
        Inhalation inhalation3 = InhalationGenerator.generateGoodInhalation(medicalDevice);
        inhalation.event.id = 3;
        inhalations.add(inhalation3);
        Inhalation inhalation4 = InhalationGenerator.generateFairInhalation(medicalDevice);
        inhalation2.event.id = 4;
        inhalations.add(inhalation4);

        UploadInhalationsRequest uploadInhalationsRequest = new UploadInhalationsRequest(inhalations);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .header("device-uuid", mobileApplication.getUUID())
                .request()
                .body(uploadInhalationsRequest)
                .when()
                .log().all()
                .post(" /medication/administration/inhalations")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();


        //provision
        Response response3 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .post("/data/provision/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath extractor2 = response3.jsonPath();
        this.stateToken = extractor2.get("stateToken");

        String provisionID = getProvisionID(stateToken);
        String partner = getPartnerID(stateToken);

        //account connect
        ConnectRequest connectRequest = objectMapper.readValue(PropertyUtils.readRequest("data", "connect"),
                ConnectRequest.class);
        connectRequest.connection.provisionID = provisionID;

        given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("Content-Type", "application/json")
                .header("External-Entity-Id", profile.getExternalEntityID())
                .header("Authorization", "Bearer " + accessToken)
                .body(connectRequest)
                .when().log().all()
                .post("/account/connect")
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        boolean encounteredError = false;

        Thread.sleep(2_000);

        //get inhalation data
        for (int i = 0; i < 500; i++) {
            Response response2 = given().log().all()
                    .filter(new ConsoleReportFilter(extentTest))
                    .baseUri(PropertyUtils.readProperty("platformUrl"))
                    .header("X-API-Key", apiKey)
                    .pathParam("patientID", patientID)
                    .get("data/inhalation/{patientID}")
                    .then()
                    .log().all()
                    .assertThat()
                    .extract().response();

            if (response2.getStatusCode() == 429) {
                TevaAssert.assertEquals(extentTest, response2.getStatusCode(), 429, "Setting burst too low shall result in a 429.");
                encounteredError = true;
                break;
            }
        }

        TevaAssert.assertTrue(extentTest, encounteredError, "Encountered burst limit at least one time");
    }
}
