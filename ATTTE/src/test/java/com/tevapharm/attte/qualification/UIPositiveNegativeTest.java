package com.tevapharm.attte.qualification;


import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.pageObjects.AuthorizationPage;
import com.tevapharm.attte.pageObjects.ConsentPage;
import com.tevapharm.attte.pageObjects.PartnerLoginPage;
import com.tevapharm.attte.pageObjects.VendorToolPage;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.UiBaseTest;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


import static java.lang.Thread.sleep;
import static io.restassured.RestAssured.given;


@Listeners(TestListeners.class)
public class UIPositiveNegativeTest extends UiBaseTest {
    private String accessToken = "";
    private String partnerID = "";
    private String apiKey = "";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(priority = 1, testName = "Prove that the tool can be used to show negative test scenarios for UIs")
    @Traceability(URS = {"1719","1720"})
    public void tc01_UI_negative_test() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        accessToken = RestAssuredOAuth.getToken();

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        String response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken).
                header("Content-Type", "application/json")
                .request()
                .body(partnerRequest).when()
                .log().all()
                .post("/configuration/partners")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response().asString();

        JsonPath js = new JsonPath(response);
        this.partnerID = js.getString("partnerID");

        Thread.sleep(20_000);
        //upload privacy notice for the partner
        File pnTxt = new File("./documents/privacyNotice.txt");
        Response responseFile = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .multiPart("file", pnTxt, "text/plain")
                .log().all()
                .post(PropertyUtils.readProperty("onboardPn01ConsentUrl"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, 200, responseFile.getStatusCode(), "Privacy Notice file uploaded successfully");

        //upload terms and conditions for the partner
        File termsTxt = new File("./documents/termsAndConditions.txt");
        Response responseFileB = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .multiPart("file", termsTxt, "text/plain")
                .log().all()
                .post(PropertyUtils.readProperty("onboardtou01ConsentUrl"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, 200, responseFileB.getStatusCode(), "Terms Of Use file uploaded successfully");

        //upload hipaa for the partner
        File hippaTxt = new File("./documents/hipaa.txt");
        Response responseFileC = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .multiPart("file", hippaTxt, "text/plain")
                .log().all()
                .post(PropertyUtils.readProperty("onboardHipaa01ConsentUrl"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, 200, responseFileC.getStatusCode(), "Hipaa consent file uploaded successfully");

        //upload marketing for the partner
        File marketingTxt = new File("./documents/marketing.txt");
        Response responseFileD = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .multiPart("file", marketingTxt, "text/plain")
                .log().all()
                .post(PropertyUtils.readProperty("onboardMarketing01ConsentUrl"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, 200, responseFileD.getStatusCode(), "Marketing Consent file uploaded successfully");
        //upload signature and conditions for the partner
        File signatureTxt = new File("./documents/signature.txt");
        Response responseFileE = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID)
                .multiPart("file", signatureTxt, "text/plain")
                .log().all()
                .post(PropertyUtils.readProperty("onboardSignature01ConsentUrl"))
                .then()
                .log().all()
                .extract().response();

        TevaAssert.assertEquals(extentTest, 200, responseFileE.getStatusCode(), "Signature Policy file uploaded successfully");

        GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
                GenerateApiRequest.class);

        Response response2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .basePath("configuration/partners")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .pathParam("partnerID", partnerID).body(apiKeyRequest)
                .when().post("/{partnerID}/key")
                .then()
                .log().all()
                .extract().response();

        JsonPath extractor = response2.jsonPath();
        this.apiKey = extractor.get("apiKey");

        VendorToolPage vendorToolPage = new VendorToolPage(driver);

        vendorToolPage = new VendorToolPage(driver);
        vendorToolPage.login("123456", apiKey);

        PartnerLoginPage partnerrLoginPage = new PartnerLoginPage(driver);
        boolean actual = partnerrLoginPage.checkTitle();
        TevaAssert.assertTrue(extentTest, actual, "Partner Login Page is displayed");

        partnerrLoginPage.login(PropertyUtils.readProperty("invalidUser"), PropertyUtils.readProperty("idHubPassword"));
        String actualError = partnerrLoginPage.getEmptyFieldErrorMsg();
        extentTest.info("fill in wrong user name with correct password");
        takeScreenshot(extentTest);
        String ExpectedError = PropertyUtils.readProperty("partnerLoginError3");
        TevaAssert.assertEquals(extentTest, actualError, ExpectedError, "error message for wrong user name field is correct");

    }

    @Test(priority = 2, testName = "Prove that the tool can be used to show positive test scenarios for UIs")
    @Traceability(URS = {"1719","1720"})
    public void tc02_positive_test() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        driver.navigate().refresh();

        String currentPage = driver.getCurrentUrl();

        PartnerLoginPage partnerrLoginPage = new PartnerLoginPage(driver);
        partnerrLoginPage.login2(PropertyUtils.readProperty("idHubUser"), PropertyUtils.readProperty("idHubPassword"));
        extentTest.info("Login with valid credentials");
        takeScreenshot(extentTest);
        partnerrLoginPage.clickLogin2();

        ConsentPage cp = new ConsentPage(driver);
        cp.getTitleText();
        cp.choosePerson(PropertyUtils.readProperty("guardianName"));
        extentTest.info("Next page is displayed - user selection");
        takeScreenshot(extentTest);

        TevaAssert.assertNotEquals(extentTest,currentPage, driver.getCurrentUrl(),"After clicking the login button the user should be redirected to a new page.");

        cp.clickContinue();

        AuthorizationPage ap = new AuthorizationPage(driver);
        sleep(2000);
        extentTest.info("Webpage screenshot before clicking the checkbox");
        takeScreenshot(extentTest);
        ap.clickCheckbox();
        extentTest.info("Webpage screenshot after clicking the checkbox");
        takeScreenshot(extentTest);

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        given()
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

        given()
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
    }
}



