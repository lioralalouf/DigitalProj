package com.tevapharm.automation.dhp.tests.dss.data;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.generators.InhalationGenerator;
import com.tevapharm.attte.generators.MedicalDeviceGenerator;
import com.tevapharm.attte.generators.MobileApplicationGenerator;
import com.tevapharm.attte.generators.ProfileGenerator;
import com.tevapharm.attte.models.DataProviders;
import com.tevapharm.attte.models.database.Inhalation;
import com.tevapharm.attte.models.database.MedicalDevice;
import com.tevapharm.attte.models.database.MobileApplication;
import com.tevapharm.attte.models.database.Profile;
import com.tevapharm.attte.models.reponse.data.InhalerDataResponse;
import com.tevapharm.attte.models.request.ConnectRequest;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.models.request.account.ProfileCreationRequest;
import com.tevapharm.attte.models.request.inhalation.UploadInhalationsRequest;
import com.tevapharm.attte.models.request.medicaldevice.RegisterMedicalDeviceRequest;
import com.tevapharm.attte.models.request.mobiledevice.RegisterMobileApplicationRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.IpAddressUtils;
import com.tevapharm.attte.utils.LambdaUtils;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.utils.TevaAssert;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class GetUserInhalationsByPartnerPositiveTest extends PartnerApiTestBase {

    private String stateToken;
    private Profile profile;
    private MedicalDevice medicalDevice;
    private MobileApplication mobileApplication;
    private String apiKey;
    private String partnerID;
    private static final IpAddressUtils ipa = new IpAddressUtils();

    @Test(priority = 1, testName = "Get inhalation peakflow data for Emergency drug 'Pro Air'", dataProvider = "getPeakFlowEmergency", dataProviderClass = DataProviders.class)
    @Traceability(FS = {"1633"})
    public void tc01_getPeakFlowProAir(int peakFlow) throws IOException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), peakFlow);

        Map<String, String> environment = LambdaUtils.getLambdaConfiguration(PropertyUtils.readProperty("getPatientInhalations"));
        String patientID = UUID.randomUUID().toString();

        extentTest.info("Set volumeTransferEnabled and peakFlowTransferEnabled As true");
        environment.put("volumeTransferEnabled", "true");
        environment.put("peakFlowTransferEnabled", "true");

        LambdaUtils.updateLambdaConfiguration(PropertyUtils.readProperty("getPatientInhalations"), environment);

        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        this.partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Generate new API key with valid read data scope");

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
        String apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);

        extentTest.info("Create new user account");
        profile = ProfileGenerator.getProfile();

        //create account
        Profile profile = ProfileGenerator.getProfile();
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


        extentTest.info("Create new mobile device to the user");
        mobileApplication = MobileApplicationGenerator.getATTTE();

        // Need Mobile App
        MobileApplication mobileApplication = MobileApplicationGenerator.getATTTE();
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


        extentTest.info("Create digihaler medical device to the user");
        // medicalDevice = MedicalDeviceGenerator.getProAir();

        MedicalDevice medicalDevice = MedicalDeviceGenerator.getProAir();

        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        long actualDeviceSerial = registerMedicalDeviceRequest.inhaler.serialNumber;
        registerMedicalDeviceRequest.inhaler.lastConnectionDate = "2015-12-01T00:00:00+04:00";
        registerMedicalDeviceRequest.inhaler.addedDate = "2015-12-01T00:00:00+04:00";
        String actualAddedDate = registerMedicalDeviceRequest.inhaler.addedDate;
        String actualLastConnection = registerMedicalDeviceRequest.inhaler.lastConnectionDate;

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

        extentTest.info("Upload inhalations by the user");
        List<Inhalation> inhalations = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Inhalation inhalation = InhalationGenerator.generateGoodInhalation(medicalDevice);
            Inhalation inhalation2 = InhalationGenerator.generateFairInhalation(medicalDevice);
            inhalations.add(inhalation);
            inhalations.add(inhalation2);
            inhalation.event.id = 1;
            inhalation.event.peakFlow = peakFlow;
            //inhalation.event.volume = 5;
            inhalation2.event.id = 2;
            inhalation2.event.peakFlow = peakFlow;
            //inhalation2.event.volume = 5;

        }

        extentTest.info("Upload inhalations by the user");
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

        extentTest.info("Provision user and partner to get state token and extract the decoded provision ID from it");
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

        TevaAssert.assertNotNull(extentTest, stateToken, "State token is present in response");

        String provisionID = getProvisionID(stateToken);

        extentTest.info("Insert the provision id in request body and connect the account of the user to the partner");
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

        extentTest.info("Get user account and verify the user's data transfer consent status to this partner is active in Http Response");
        Response resGetAccount = given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .when()
                .log().all()
                .get("/account")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath js2 = resGetAccount.jsonPath();
        List<LinkedHashMap<String, String>> allConsents = js2.getList("account.consents");
        for (LinkedHashMap<String, String> consent : allConsents) {
            if (consent.get("consentType").equalsIgnoreCase("dataTransferConsent")) {
                if (consent.get("partnerID").equalsIgnoreCase(partnerID)) {
                    TevaAssert.assertEquals(extentTest, consent.get("status"), "Active", "Data transfer consent status is active");
                }
            }
        }

        extentTest.info("Get user's inhaler details by partner and verify the correct serial number, drug and added date returns in response for inhaler");
        Response response2 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        InhalerDataResponse inhalerResponse = response2.getBody().as(InhalerDataResponse.class);
        long s = Long.parseLong(inhalerResponse.inhalers.get(0).serialNumber);
        TevaAssert.assertEquals(extentTest, actualDeviceSerial, s, "Response inhaler serial number is correct");
        TevaAssert.assertEquals(extentTest, actualAddedDate, inhalerResponse.inhalers.get(0).addedDate, "added date is correct");
        TevaAssert.assertEquals(extentTest, "AAA200", inhalerResponse.inhalers.get(0).drug, "Drug is correct");
        TevaAssert.assertEquals(extentTest, actualLastConnection, inhalerResponse.inhalers.get(0).lastConnectionDate, "lastConnectionDate is correct");
        TevaAssert.assertEquals(extentTest, 1, inhalerResponse.inhalers.get(0).deviceStatus, "deviceStatus is correct");


        extentTest.info("Get inhalations data by partner, Verify HTTP Response for returning all fields and correct peakflow for all inhalations");
        Response response4 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhalation/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        //InhalationResponse inhalationResponse = response4.getBody().as(InhalationResponse.class);

        JsonPath json = response4.jsonPath();

        List<LinkedHashMap<String, String>> allInhalations = json.getList("inhalations");

        System.out.println("these are all inhalations - " + allInhalations);

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("1")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("peakFlow"), peakFlow, "Http Response has the Correct peakflow value for inhalation number 1");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("volume"), provisionID);
            }

        }

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("2")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("peakFlow"), peakFlow, "Http Response has the Correct peakflow value for inhalation number 2");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("volume"), provisionID);
            }

        }

    }

    @Test(priority = 1, testName = "Get inhalation peakflow data for Maintenance drug 'Air Duo", dataProvider = "getPeakFlowMaintenance", dataProviderClass = DataProviders.class)
    @Traceability(URS = {"x.x.x"}, FS = {"1633"})
    public void tc02_getPeakFlowAirDuo(int peakFlow) throws IOException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), peakFlow);
        String patientID = UUID.randomUUID().toString();

        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        this.partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Generate new API key with valid read data scope");

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
        String apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);

        extentTest.info("Create new user account");
        profile = ProfileGenerator.getProfile();

        //create account
        Profile profile = ProfileGenerator.getProfile();
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


        extentTest.info("Create new mobile device to the user");
        mobileApplication = MobileApplicationGenerator.getATTTE();

        // Need Mobile App
        MobileApplication mobileApplication = MobileApplicationGenerator.getATTTE();
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


        extentTest.info("Create digihaler medical device to the user");
        // medicalDevice = MedicalDeviceGenerator.getProAir();

        MedicalDevice medicalDevice = MedicalDeviceGenerator.getAirDuo();

        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        long actualDeviceSerial = registerMedicalDeviceRequest.inhaler.serialNumber;
        registerMedicalDeviceRequest.inhaler.lastConnectionDate = "2015-12-01T00:00:00+04:00";
        registerMedicalDeviceRequest.inhaler.addedDate = "2015-12-01T00:00:00+04:00";
        String actualAddedDate = registerMedicalDeviceRequest.inhaler.addedDate;
        String actualLastConnection = registerMedicalDeviceRequest.inhaler.lastConnectionDate;

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

        extentTest.info("Upload inhalations by the user");
        List<Inhalation> inhalations = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Inhalation inhalation = InhalationGenerator.generateGoodInhalation(medicalDevice);
            Inhalation inhalation2 = InhalationGenerator.generateFairInhalation(medicalDevice);
            inhalations.add(inhalation);
            inhalations.add(inhalation2);
            inhalation.event.id = 1;
            inhalation.event.peakFlow = peakFlow;
            //inhalation.event.volume = 5;
            inhalation2.event.id = 2;
            inhalation2.event.peakFlow = peakFlow;
            //inhalation2.event.volume = 5;

        }

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

        extentTest.info("Provision user and partner to get state token and extract the decoded provision ID from it");
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

        TevaAssert.assertNotNull(extentTest, stateToken, "State token is present in response");

        String provisionID = getProvisionID(stateToken);

        extentTest.info("Insert the provision id in request body and connect the account of the user to the partner");
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

        extentTest.info("Get user account and verify the user's data transfer consent status to this partner is active in Http Response");
        Response resGetAccount = given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .when()
                .log().all()
                .get("/account")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath js2 = resGetAccount.jsonPath();
        List<LinkedHashMap<String, String>> allConsents = js2.getList("account.consents");
        for (LinkedHashMap<String, String> consent : allConsents) {
            if (consent.get("consentType").equalsIgnoreCase("dataTransferConsent")) {
                if (consent.get("partnerID").equalsIgnoreCase(partnerID)) {
                    TevaAssert.assertEquals(extentTest, consent.get("status"), "Active", "Data transfer consent status is active");
                }
            }
        }

        extentTest.info("Get user's inhaler details by partner and verify the correct serial number, drug and added date returns in response for inhaler");
        Response response2 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        InhalerDataResponse inhalerResponse = response2.getBody().as(InhalerDataResponse.class);
        long s = Long.parseLong(inhalerResponse.inhalers.get(0).serialNumber);
        TevaAssert.assertEquals(extentTest, actualDeviceSerial, s, "Response inhaler serial number is correct");
        TevaAssert.assertEquals(extentTest, actualAddedDate, inhalerResponse.inhalers.get(0).addedDate, "added date is correct");
        TevaAssert.assertEquals(extentTest, "FSL060", inhalerResponse.inhalers.get(0).drug, "Drug is correct");
        TevaAssert.assertEquals(extentTest, actualLastConnection, inhalerResponse.inhalers.get(0).lastConnectionDate, "lastConnectionDate is correct");
        TevaAssert.assertEquals(extentTest, 1, inhalerResponse.inhalers.get(0).deviceStatus, "deviceStatus is correct");


        extentTest.info("Get inhalations data by partner, Verify HTTP Response for returning all fields and correct peakflow for all inhalations");
        Response response4 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhalation/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath json = response4.jsonPath();

        List<LinkedHashMap<String, String>> allInhalations = json.getList("inhalations");

        System.out.println("these are all inhalations - " + allInhalations);

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("1")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("peakFlow"), peakFlow, "Http Response has the Correct peakflow value for inhalation number 1");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("volume"), provisionID);
            }

        }

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("2")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("peakFlow"), peakFlow, "Http Response has the Correct peakflow value for inhalation number 2");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("volume"), provisionID);
            }

        }

    }

    @Test(priority = 1, testName = "Get inhalation peakflow data for Maintenance drug 'Armon Air", dataProvider = "getPeakFlowMaintenance", dataProviderClass = DataProviders.class)
    @Traceability(URS = {"x.x.x"}, FS = {"1633"})
    public void tc03_getPeakFlowArmonAir(int peakFlow) throws IOException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), peakFlow);
        String patientID = UUID.randomUUID().toString();

        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        this.partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Generate new API key with valid read data scope");

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
        String apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);

        extentTest.info("Create new user account");
        profile = ProfileGenerator.getProfile();

        //create account
        Profile profile = ProfileGenerator.getProfile();
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


        extentTest.info("Create new mobile device to the user");
        mobileApplication = MobileApplicationGenerator.getATTTE();

        // Need Mobile App
        MobileApplication mobileApplication = MobileApplicationGenerator.getATTTE();
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


        extentTest.info("Create digihaler medical device to the user");
        MedicalDevice medicalDevice = MedicalDeviceGenerator.getArmonAir();

        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        long actualDeviceSerial = registerMedicalDeviceRequest.inhaler.serialNumber;
        registerMedicalDeviceRequest.inhaler.lastConnectionDate = "2015-12-01T00:00:00+04:00";
        registerMedicalDeviceRequest.inhaler.addedDate = "2015-12-01T00:00:00+04:00";
        String actualAddedDate = registerMedicalDeviceRequest.inhaler.addedDate;
        String actualLastConnection = registerMedicalDeviceRequest.inhaler.lastConnectionDate;

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

        extentTest.info("Upload inhalations by the user");
        List<Inhalation> inhalations = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Inhalation inhalation = InhalationGenerator.generateGoodInhalation(medicalDevice);
            Inhalation inhalation2 = InhalationGenerator.generateFairInhalation(medicalDevice);
            inhalations.add(inhalation);
            inhalations.add(inhalation2);
            inhalation.event.id = 1;
            inhalation.event.peakFlow = peakFlow;
            inhalation2.event.id = 2;
            inhalation2.event.peakFlow = peakFlow;
        }

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

        extentTest.info("Provision user and partner to get state token and extract the decoded provision ID from it");
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

        TevaAssert.assertNotNull(extentTest, stateToken, "State token is present in response");

        String provisionID = getProvisionID(stateToken);

        extentTest.info("Insert the provision id in request body and connect the account of the user to the partner");
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

        extentTest.info("Get user account and verify the user's data transfer consent status to this partner is active in Http Response");
        Response resGetAccount = given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .when()
                .log().all()
                .get("/account")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath js2 = resGetAccount.jsonPath();
        List<LinkedHashMap<String, String>> allConsents = js2.getList("account.consents");
        for (LinkedHashMap<String, String> consent : allConsents) {
            if (consent.get("consentType").equalsIgnoreCase("dataTransferConsent")) {
                if (consent.get("partnerID").equalsIgnoreCase(partnerID)) {
                    TevaAssert.assertEquals(extentTest, consent.get("status"), "Active", "Data transfer consent status is active");
                }
            }
        }

        extentTest.info("Get user's inhaler details by partner and verify the correct serial number, drug and added date returns in response for inhaler");
        Response response2 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        InhalerDataResponse inhalerResponse = response2.getBody().as(InhalerDataResponse.class);
        long s = Long.parseLong(inhalerResponse.inhalers.get(0).serialNumber);
        TevaAssert.assertEquals(extentTest, actualDeviceSerial, s, "Response inhaler serial number is correct");
        TevaAssert.assertEquals(extentTest, actualAddedDate, inhalerResponse.inhalers.get(0).addedDate, "added date is correct");
        TevaAssert.assertEquals(extentTest, "FPL060", inhalerResponse.inhalers.get(0).drug, "Drug is correct");
        TevaAssert.assertEquals(extentTest, actualLastConnection, inhalerResponse.inhalers.get(0).lastConnectionDate, "lastConnectionDate is correct");
        TevaAssert.assertEquals(extentTest, 1, inhalerResponse.inhalers.get(0).deviceStatus, "deviceStatus is correct");


        extentTest.info("Get inhalations data by partner, Verify HTTP Response for returning all fields and correct peakflow for all inhalations");
        Response response4 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhalation/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath json = response4.jsonPath();

        extentTest.info("Verify HTTP Response for returning all fields and correct peakflow of inhalation 1");
        List<LinkedHashMap<String, String>> allInhalations = json.getList("inhalations");

        System.out.println("these are all inhalations - " + allInhalations);

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("1")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("peakFlow"), peakFlow, "Http Response has the Correct peakflow value for inhalation number 1");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("volume"), provisionID);
            }

        }

        extentTest.info("Verify HTTP Response for returning all fields and correct peakflow of inhalation 2");
        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("2")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("peakFlow"), peakFlow, "Http Response has the Correct peakflow value for inhalation number 2");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("volume"), provisionID);
            }

        }

    }


    @Test(priority = 1, testName = "Get inhalation volume data for Emergency drug 'Pro Air'", dataProvider = "getVolumePositive", dataProviderClass = DataProviders.class)
    @Traceability(URS = {"x.x.x"}, FS = {"1633"})
    public void tc04_getVolume(double volume) throws IOException, ParseException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), volume);
        String patientID = UUID.randomUUID().toString();

        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        this.partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Generate new API key with valid read data scope");

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
        String apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);

        extentTest.info("Create new user account");
        profile = ProfileGenerator.getProfile();

        //create account
        Profile profile = ProfileGenerator.getProfile();
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


        extentTest.info("Create new mobile device to the user");
        mobileApplication = MobileApplicationGenerator.getATTTE();

        // Need Mobile App
        MobileApplication mobileApplication = MobileApplicationGenerator.getATTTE();
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


        extentTest.info("Create digihaler medical device to the user");
        MedicalDevice medicalDevice = MedicalDeviceGenerator.getProAir();

        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        long actualDeviceSerial = registerMedicalDeviceRequest.inhaler.serialNumber;
        registerMedicalDeviceRequest.inhaler.lastConnectionDate = "2015-12-01T00:00:00+04:00";
        registerMedicalDeviceRequest.inhaler.addedDate = "2015-12-01T00:00:00+04:00";
        String actualAddedDate = registerMedicalDeviceRequest.inhaler.addedDate;
        String actualLastConnection = registerMedicalDeviceRequest.inhaler.lastConnectionDate;

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


        extentTest.info("Upload inhalations by the user");
        List<Inhalation> inhalations = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Inhalation inhalation = InhalationGenerator.generateGoodInhalation(medicalDevice);
            Inhalation inhalation2 = InhalationGenerator.generateFairInhalation(medicalDevice);
            inhalations.add(inhalation);
            inhalations.add(inhalation2);
            inhalation.event.id = 1;
            inhalation.event.volume = volume;
            inhalation.event.peakFlow = 90;
            inhalation2.event.id = 2;
            inhalation2.event.volume = volume;
            inhalation2.event.peakFlow = 90;
        }

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

        extentTest.info("Provision user and partner to get state token and extract the decoded provision ID from it");
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

        TevaAssert.assertNotNull(extentTest, stateToken, "State token is present in response");


        String provisionID = getProvisionID(stateToken);

        extentTest.info("Insert the provision id in request body and connect the account of the user to the partner");
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

        extentTest.info("Get user account and verify the user's data transfer consent status to this partner is active in Http Response");
        Response resGetAccount = given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .when()
                .log().all()
                .get("/account")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath js2 = resGetAccount.jsonPath();
        List<LinkedHashMap<String, String>> allConsents = js2.getList("account.consents");
        for (LinkedHashMap<String, String> consent : allConsents) {
            if (consent.get("consentType").equalsIgnoreCase("dataTransferConsent")) {
                if (consent.get("partnerID").equalsIgnoreCase(partnerID)) {
                    TevaAssert.assertEquals(extentTest, consent.get("status"), "Active", "Data transfer consent status is active");
                }
            }
        }

        extentTest.info("Get user's inhaler details by partner and verify the correct serial number, drug and added date returns in response for inhaler");
        Response response2 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        InhalerDataResponse inhalerResponse = response2.getBody().as(InhalerDataResponse.class);
        long s = Long.parseLong(inhalerResponse.inhalers.get(0).serialNumber);
        TevaAssert.assertEquals(extentTest, actualDeviceSerial, s, "Response inhaler serial number is correct");
        TevaAssert.assertEquals(extentTest, actualAddedDate, inhalerResponse.inhalers.get(0).addedDate, "added date is correct");
        TevaAssert.assertEquals(extentTest, "AAA200", inhalerResponse.inhalers.get(0).drug, "Drug is correct");
        TevaAssert.assertEquals(extentTest, actualLastConnection, inhalerResponse.inhalers.get(0).lastConnectionDate, "lastConnectionDate is correct");
        TevaAssert.assertEquals(extentTest, 1, inhalerResponse.inhalers.get(0).deviceStatus, "deviceStatus is correct");


        extentTest.info("Get user's  details by partner and verify the correct serial number, drug and added date returns in response");
        Response response4 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhalation/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath json = response4.jsonPath();

        extentTest.info("Get inhalations data by partner, Verify HTTP Response for returning all fields and correct Volume for all inhalations");
        List<LinkedHashMap<String, String>> allInhalations = json.getList("inhalations");

        System.out.println("these are all inhalations - " + allInhalations);

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("1")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("volume"), (int) volume, "Http Response has the Correct volume value for inhalation number 1");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("peakFlow"), provisionID);
            }

        }

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("2")) {
                TevaAssert.assertEquals(extentTest, inhalation.get("volume"), (int) volume, "Http Response has the Correct volume value for inhalation number 2");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("peakFlow"), provisionID);
            }
        }

    }

    @Test(priority = 1, testName = "Get inhalation volume data or Maintenance drug 'Air Duo', Volume value is '0.3")
    @Traceability(URS = {"x.x.x"}, FS = {"1633"})
    public void tc05_getVolume() throws IOException, ParseException, InterruptedException, org.json.simple.parser.ParseException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        String patientID = UUID.randomUUID().toString();

        extentTest.info("Onboard a new partner");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        this.partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Generate new API key with valid read data scope");

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
        String apiKey = extractor.get("apiKey");
        registerApiKey(partnerID, apiKey);

        extentTest.info("Create new user account");
        profile = ProfileGenerator.getProfile();

        //create account
        Profile profile = ProfileGenerator.getProfile();
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


        extentTest.info("Create new mobile device to the user");
        mobileApplication = MobileApplicationGenerator.getATTTE();

        // Need Mobile App
        MobileApplication mobileApplication = MobileApplicationGenerator.getATTTE();
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


        extentTest.info("Create digihaler medical device to the user");
        MedicalDevice medicalDevice = MedicalDeviceGenerator.getAirDuo();

        RegisterMedicalDeviceRequest registerMedicalDeviceRequest = new RegisterMedicalDeviceRequest(medicalDevice);
        long actualDeviceSerial = registerMedicalDeviceRequest.inhaler.serialNumber;
        registerMedicalDeviceRequest.inhaler.lastConnectionDate = "2015-12-01T00:00:00+04:00";
        registerMedicalDeviceRequest.inhaler.addedDate = "2015-12-01T00:00:00+04:00";
        String actualAddedDate = registerMedicalDeviceRequest.inhaler.addedDate;
        String actualLastConnection = registerMedicalDeviceRequest.inhaler.lastConnectionDate;

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


        List<Inhalation> inhalations = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Inhalation inhalation = InhalationGenerator.generateGoodInhalation(medicalDevice);
            Inhalation inhalation2 = InhalationGenerator.generateFairInhalation(medicalDevice);
            inhalations.add(inhalation);
            inhalations.add(inhalation2);
            inhalation.event.id = 1;
            inhalation.event.volume = 0.3;
            inhalation.event.peakFlow = 90;
            inhalation2.event.id = 2;
            inhalation2.event.volume = 0.3;
            inhalation2.event.peakFlow = 90;
        }

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

        extentTest.info("Provision user and partner to get state token and extract the decoded provision ID from it");
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

        TevaAssert.assertNotNull(extentTest, stateToken, "State token is present in response");


        String provisionID = getProvisionID(stateToken);

        extentTest.info("Insert the provision id in request body and connect the account of the user to the partner");
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

        extentTest.info("Get user account and verify the user's data transfer consent status to this partner is active in Http Response");
        Response resGetAccount = given().log().all()
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("external-entity-id", profile.getExternalEntityID())
                .request()
                .when()
                .log().all()
                .get("/account")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath js2 = resGetAccount.jsonPath();
        List<LinkedHashMap<String, String>> allConsents = js2.getList("account.consents");
        for (LinkedHashMap<String, String> consent : allConsents) {
            if (consent.get("consentType").equalsIgnoreCase("dataTransferConsent")) {
                if (consent.get("partnerID").equalsIgnoreCase(partnerID)) {
                    TevaAssert.assertEquals(extentTest, consent.get("status"), "Active", "Data transfer consent status is active");
                }
            }
        }

        extentTest.info("Get user's inhaler details by partner and verify the correct serial number, drug and added date returns in response for inhaler");
        Response response2 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhaler/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        InhalerDataResponse inhalerResponse = response2.getBody().as(InhalerDataResponse.class);
        long s = Long.parseLong(inhalerResponse.inhalers.get(0).serialNumber);
        TevaAssert.assertEquals(extentTest, actualDeviceSerial, s, "Response inhaler serial number is correct");
        TevaAssert.assertEquals(extentTest, actualAddedDate, inhalerResponse.inhalers.get(0).addedDate, "added date is correct");
        TevaAssert.assertEquals(extentTest, "FSL060", inhalerResponse.inhalers.get(0).drug, "Drug is correct");
        TevaAssert.assertEquals(extentTest, actualLastConnection, inhalerResponse.inhalers.get(0).lastConnectionDate, "lastConnectionDate is correct");
        TevaAssert.assertEquals(extentTest, 1, inhalerResponse.inhalers.get(0).deviceStatus, "deviceStatus is correct");


        extentTest.info("Get inhalations data by partner, Verify HTTP Response for returning all fields and correct Volume for all inhalations");
        Response response4 = given().log().all()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", apiKey)
                .pathParam("patientID", patientID)
                .get("data/inhalation/{patientID}")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath json = response4.jsonPath();

        List<LinkedHashMap<String, String>> allInhalations = json.getList("inhalations");

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("1")) {
                TevaAssert.assertEquals(extentTest, String.valueOf(inhalation.get("volume")), "0.3", "Http Response has the Correct volume value for inhalation number 1");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("peakFlow"), provisionID);
            }

        }

        for (LinkedHashMap<String, String> inhalation : allInhalations) {
            String id = inhalation.get("id");
            if (inhalation.get("id").substring(id.indexOf(':') + 1).equalsIgnoreCase("2")) {
                TevaAssert.assertEquals(extentTest, String.valueOf(inhalation.get("volume")), "0.3", "Http Response has the Correct volume value for inhalation number 2");
                TevaAssert.assertNotNull(extentTest, inhalation.get("time"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("category"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("drug"), provisionID);
                TevaAssert.assertNotNull(extentTest, inhalation.get("peakFlow"), provisionID);
            }

        }

    }
}
