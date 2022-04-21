package com.tevapharm.automation.dhp.tests.dss.data;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.generators.MedicalDeviceGenerator;
import com.tevapharm.attte.generators.MobileApplicationGenerator;
import com.tevapharm.attte.generators.ProfileGenerator;
import com.tevapharm.attte.models.database.MedicalDevice;
import com.tevapharm.attte.models.database.MobileApplication;
import com.tevapharm.attte.models.database.Profile;
import com.tevapharm.attte.models.reponse.data.InhalerDataResponse;
import com.tevapharm.attte.models.request.ConnectRequest;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.models.request.account.DependentCreationRequest;
import com.tevapharm.attte.models.request.account.DependentUpdateRequest;
import com.tevapharm.attte.models.request.account.ProfileCreationRequest;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class GetDependentDataE2ETest extends PartnerApiTestBase {
    private Profile profile;
    long min = 1000000000L;
    long max = 9999999999L ;   
    Random random = new Random();

    @Test(priority = 1, testName = "E2E test for pulling dependent's data by partner")
    @Traceability(FS = {"1635", "1656", "1698", "1704"})
    public void tc01_getDependentDataE2E() throws IOException, ParseException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        String accessToken = RestAssuredOAuth.getToken();
        String patientID = UUID.randomUUID().toString();

        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        String partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Generate a new API key for onboarded partner");
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
        String apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);

        Profile guardian = ProfileGenerator.getProfile();
        ProfileCreationRequest profileCreationRequest = new ProfileCreationRequest(guardian);

        extentTest.info("Create a guardian profile");
        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("email", guardian.getEmail())
                .header("external-entity-id", guardian.getExternalEntityID())
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

        Profile dependent1 = ProfileGenerator.getDependent();
        DependentCreationRequest dependentCreationRequest = new DependentCreationRequest(dependent1);

        extentTest.info("Create dependent number 1 profile");
        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .request()
                .body(dependentCreationRequest)
                .when()
                .log().all()
                .post("/account/dependent")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();
            
        extentTest.info("Create dependent number 2 profile");
        Profile dependent2 = ProfileGenerator.getDependent();
        DependentCreationRequest dependentCreationRequest2 = new DependentCreationRequest(dependent2);
        
        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .request()
                .body(dependentCreationRequest2)
                .when()
                .log().all()
                .post("/account/dependent")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();
 
        extentTest.info("Create mobile device for guardian");
        MobileApplication mobileApplication = MobileApplicationGenerator.getATTTE();
        RegisterMobileApplicationRequest registerMobileApplicationRequest = new RegisterMobileApplicationRequest(mobileApplication);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
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

        String guardianUUID = mobileApplication.getUUID();

        extentTest.info("Create mobile device for dependent number 1");
        MobileApplication mobileApplication2 = MobileApplicationGenerator.getATTTE();
        RegisterMobileApplicationRequest  registerMobileApplicationRequest2 = new RegisterMobileApplicationRequest(mobileApplication2);

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .header("patient-external-entity-id", dependent1.getExternalEntityID())
                .request()
                .body(registerMobileApplicationRequest2)
                .when()
                .log().all()
                .post("/application/mobile")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        String dependentUUID = mobileApplication2.getUUID();
        
        
        extentTest.info("Create mobile device for dependent number 2");
        MobileApplication mobileApplication3 = MobileApplicationGenerator.getATTTE();
        registerMobileApplicationRequest = new RegisterMobileApplicationRequest(mobileApplication3);
        
        given()
        .baseUri(PropertyUtils.readProperty("platformUrl"))
        .filter(new ConsoleReportFilter(extentTest))
        .header("Authorization", "Bearer " + accessToken)
        .header("Content-Type", "application/json")
        .header("external-entity-id", guardian.getExternalEntityID())
        .header("patient-external-entity-id", dependent2.getExternalEntityID())
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

        String dependentUUID2 = mobileApplication3.getUUID();

        extentTest.info("Create medical device for the guardian");
        MedicalDevice medicalDevice = MedicalDeviceGenerator.getProAir();
        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        long randomNum =  random.nextLong() % (max - min) + max;
        registerMedicalDeviceRequest.inhaler.serialNumber = randomNum;

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .header("device-uuid", guardianUUID)
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
    
        extentTest.info("Create medical device for dependent number 1, Create unique serial number for this device");
        randomNum =  random.nextLong() % (max - min) + max;
        medicalDevice = MedicalDeviceGenerator.getProAir();
        registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        registerMedicalDeviceRequest.inhaler.serialNumber = randomNum;
        long dependentSerielNumber = registerMedicalDeviceRequest.inhaler.serialNumber;

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .header("patient-external-entity-id", dependent1.getExternalEntityID())
                .header("device-uuid", dependentUUID)
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
        
        extentTest.info("Create medical device for dependent number 2, Create unique serial number for this device");
        randomNum =  random.nextLong() % (max - min) + max;
        medicalDevice = MedicalDeviceGenerator.getProAir();
        registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        registerMedicalDeviceRequest.inhaler.serialNumber = randomNum;
        long dependent2SerielNumber = registerMedicalDeviceRequest.inhaler.serialNumber;

        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .header("patient-external-entity-id", dependent2.getExternalEntityID())
                .header("device-uuid", dependentUUID2)
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

        extentTest.info("Provision guardian and partner to get state token and extract the decoded provision ID from it");
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
        String stateToken = extractor2.get("stateToken");

        String provisionID = getProvisionID(stateToken);

        extentTest.info("Provision dependent number 1 and partner to get state token and extract the decoded provision ID from it");
        Response response4 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", "111")
                .post("/data/provision/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath extractor3 = response4.jsonPath();
        stateToken = extractor3.get("stateToken");
        String provisionID2 = getProvisionID(stateToken);

        extentTest.info("Insert the provision id in request body and connect the account of the guardian to the partner");
        ConnectRequest connectRequest = objectMapper.readValue(PropertyUtils.readRequest("data", "connect"),
                ConnectRequest.class);
        connectRequest.connection.provisionID = provisionID;

        given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("Content-Type", "application/json")
                .header("External-Entity-Id", guardian.getExternalEntityID())
                .header("Authorization", "Bearer " + accessToken)
                .body(connectRequest)
                .when().log().all()
                .post("/account/connect")
                .then()
                .extract().response();

        extentTest.info("Insert the provision id in request body and connect the account of dependent number 1 to the partner");
        connectRequest = objectMapper.readValue(PropertyUtils.readRequest("data", "connect"),
                ConnectRequest.class);
        connectRequest.connection.provisionID = provisionID2;

        given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("Content-Type", "application/json")
                .header("External-Entity-Id", guardian.getExternalEntityID())
                .header("patient-external-entity-id", dependent1.getExternalEntityID())
                .header("Authorization", "Bearer " + accessToken)
                .body(connectRequest)
                .when().log().all()
                .post("/account/connect")
                .then()
                .extract().response();

        extentTest.info("Get dependent 1 inhaler details by partner and verify the correct serial number, drug and added date returns in response");
        Response response7 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", "111")
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        InhalerDataResponse inhalerResponse = response7.getBody().as(InhalerDataResponse.class);
        long s = Long.parseLong(inhalerResponse.inhalers.get(0).serialNumber);
        TevaAssert.assertEquals(extentTest, dependentSerielNumber, s, "Response inhaler serial number is correct");
        TevaAssert.assertNotNull(extentTest, inhalerResponse.inhalers.get(0).addedDate, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse.inhalers.get(0).lastConnectionDate, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse.inhalers.get(0).deviceStatus, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse.inhalers.get(0).addedDate, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse.inhalers.get(0).drug, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse.inhalers.get(0).brandName, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse.inhalers.get(0).strength, "");

        extentTest.info("Provision dependent number 2 and partner to get state token and extract the decoded provision ID from it");
        Response response8 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", "222")
                .post("/data/provision/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath extractor4 = response8.jsonPath();
        stateToken = extractor4.get("stateToken");
        String provisionID3 = getProvisionID(stateToken);
        
        extentTest.info("Insert the provision id in request body and connect the account of dependent number 2 to the partner");
        connectRequest = objectMapper.readValue(PropertyUtils.readRequest("data", "connect"),
                ConnectRequest.class);
        connectRequest.connection.provisionID = provisionID3;

        given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("Content-Type", "application/json")
                .header("External-Entity-Id", guardian.getExternalEntityID())
                .header("patient-external-entity-id", dependent2.getExternalEntityID())
                .header("Authorization", "Bearer " + accessToken)
                .body(connectRequest)
                .when().log().all()
                .post("/account/connect")
                .then()
                .extract().response();
   
        extentTest.info("Get dependent 2 inhaler details by partner and verify the correct serial number, drug and added date returns in response");
        Response response9 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", "222")
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        InhalerDataResponse inhalerResponse2 = response9.getBody().as(InhalerDataResponse.class);
        long s2 = Long.parseLong(inhalerResponse2.inhalers.get(0).serialNumber);
        TevaAssert.assertEquals(extentTest, dependent2SerielNumber, s2, "Response inhaler serial number is correct");
        TevaAssert.assertNotNull(extentTest, inhalerResponse2.inhalers.get(0).addedDate, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse2.inhalers.get(0).lastConnectionDate, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse2.inhalers.get(0).deviceStatus, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse2.inhalers.get(0).addedDate, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse2.inhalers.get(0).drug, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse2.inhalers.get(0).brandName, "");
        TevaAssert.assertNotNull(extentTest, inhalerResponse2.inhalers.get(0).strength, "");

        extentTest.info("Get user account and verify dependent 2 exists under guardian before setting age of majority");
        Response resGetAccount = given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .request()
                .when()
                .log().all()
                .get("/account")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();
        
        JsonPath js = resGetAccount.jsonPath();
        
        List<LinkedHashMap<String, String>> allDependents = js.getList("account.dependents");
        for (LinkedHashMap<String, String> dependent : allDependents) {
            if (dependent.get("externalEntityID").equalsIgnoreCase(dependent2.getExternalEntityID())) {
            	TevaAssert.assertEquals(extentTest, dependent.get("externalEntityID"), dependent.get("externalEntityID"), "dependent number 2 exists");

			}
		}
        
        extentTest.info("Set age of majority for dependent2 for today");
        LocalDate today = LocalDate.now(ZoneId.of("GMT"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String y = today.format(formatter);
        dependent2.setAgeOfMajority(y);

        DependentUpdateRequest dependentUpdateRequest = new DependentUpdateRequest(dependent2);
        dependentUpdateRequest.patient.ageOfMajority = y;
        
        given()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .header("patient-external-entity-id", dependent2.getExternalEntityID())
                .request()
                .body(dependentUpdateRequest)
                .when()
                .log().all()
                .put("/account/profile")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        Thread.sleep(10_000);
        
        extentTest.info("Get user account and verify dependent has been removed from the account after setting age of majority");
        Response resGetAccount2 = given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", guardian.getExternalEntityID())
                .request()
                .when()
                .log().all()
                .get("/account")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();
        
        JsonPath js2 = resGetAccount2.jsonPath();
        
        List<LinkedHashMap<String, String>> allDependets2 = js2.getList("account.dependents");
        for (LinkedHashMap<String, String> dependent : allDependets2) {
            if (!dependent.get("externalEntityID").equalsIgnoreCase(dependent2.getExternalEntityID())) {
            	TevaAssert.assertFalse(extentTest, dependent.get("externalEntityID").equalsIgnoreCase(dependent2.getExternalEntityID()), "dependent number 2 doesnt exist in account after reset age of majority");
			}
		}


        extentTest.info("Get dependent number 2 data by partner, Expecting error because dependent doesnt exist");
        given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", "222")
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(400);

	}


}
