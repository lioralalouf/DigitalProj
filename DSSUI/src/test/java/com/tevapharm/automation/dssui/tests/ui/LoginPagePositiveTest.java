package com.tevapharm.automation.dssui.tests.ui;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.UiBaseTest;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.automation.dssui.pageObjects.ConsentPage;
import com.tevapharm.automation.dssui.pageObjects.PartnerLoginPage;
import com.tevapharm.automation.dssui.pageObjects.VendorToolPage;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class LoginPagePositiveTest extends UiBaseTest {
    private String partnerNameTemp = "";

    @Test(testName = "Login with invalid credentials")
    @Traceability(URS = {"1648"}, FS = {"1663","1664"})
    public void tc01_loginInvalidCred() throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        
        extentTest.info("Onboard A new partner");

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
        this.partnerNameTemp = partnerRequest.name;
        
        extentTest.info("Generate A new api key to the onboarded partner");
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
        System.out.println(apiKey);

        //LocalStorage localStorage = ((WebStorage) driver).getLocalStorage();
        //localStorage.removeItem("stateToken");
        
        extentTest.info("Navigate to vendor mock tool and login to 'Authentication Page'");
        VendorToolPage vendorToolPage = new VendorToolPage(driver);
        vendorToolPage.login("123456", apiKey);
        takeScreenshot(extentTest);

        System.out.println(apiKey);

        PartnerLoginPage partnerLoginPage = new PartnerLoginPage(driver);
        boolean actual = partnerLoginPage.checkTitle();
        
        extentTest.info("On Authentication Page, Try Login with invalid credentials");

        partnerLoginPage.login(PropertyUtils.readProperty("idHubUser"), "");
        String actualError1 = partnerLoginPage.getEmptyFieldErrorMsg();
        String ExpectedError1 = PropertyUtils.readProperty("partnerLoginError1");
        TevaAssert.assertEquals(extentTest, actualError1, ExpectedError1, "error message for empty password field is correct");
        takeScreenshot(extentTest);

        partnerLoginPage.login("", PropertyUtils.readProperty("idHubPassword"));
        String actualError2 = partnerLoginPage.getEmptyFieldErrorMsg();
        String ExpectedError2 = PropertyUtils.readProperty("partnerLoginError2");
        TevaAssert.assertEquals(extentTest, actualError2, ExpectedError2, "error message for empty user name field is correct");
        takeScreenshot(extentTest);

        partnerLoginPage.login(PropertyUtils.readProperty("invalidUser"), PropertyUtils.readProperty("idHubPassword"));
        String actualError3 = partnerLoginPage.getEmptyFieldErrorMsg();
        String ExpectedError3 = PropertyUtils.readProperty("partnerLoginError3");
        TevaAssert.assertEquals(extentTest, actualError3, ExpectedError3, "error message for wrong user name field is correct");
        takeScreenshot(extentTest);

        partnerLoginPage.login(PropertyUtils.readProperty("idHubUser"), PropertyUtils.readProperty("invalidPaaword"));
        String actualError4 = partnerLoginPage.getEmptyFieldErrorMsg();
        String ExpectedError4 = PropertyUtils.readProperty("partnerLoginError3");
        TevaAssert.assertEquals(extentTest, actualError4, ExpectedError4, "error message for wrong password field is correct");
        takeScreenshot(extentTest);
    }

    @Test(testName = "Login with valid credentials")
    @Traceability(URS = {"1648"}, FS = {"1663","1664"})
    public void tc02_loginValidCred() throws IOException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        PartnerLoginPage partnerLoginPage = new PartnerLoginPage(driver);
        partnerLoginPage.login(PropertyUtils.readProperty("idHubUser"), PropertyUtils.readProperty("idHubPassword"));
        
        extentTest.info("Login again with valid credentials");
   
        String consentTitle = PropertyUtils.readProperty("consentTitle");
        String partnerName = this.partnerNameTemp;
        ConsentPage cp = new ConsentPage(driver);
        String actualTitle = cp.getTitleText();
        String expectedTitle = partnerName + " " + consentTitle;
        TevaAssert.assertEquals(extentTest, actualTitle, expectedTitle, "The correct title is displayed to the user");
        takeScreenshot(extentTest);

        //revoke apikey and delete partner
        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response3 = given()
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

        Response response4 = given()
                //.filter(new ConsoleReportFilter(extentTest))
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
