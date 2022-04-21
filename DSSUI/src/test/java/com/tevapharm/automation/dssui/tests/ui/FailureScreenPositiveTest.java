package com.tevapharm.automation.dssui.tests.ui;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.UiBaseTest;
import com.tevapharm.attte.utils.FileUtils;
import com.tevapharm.attte.utils.LocalStorageUtils;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.automation.dssui.pageObjects.FailureScreenPage;
import com.tevapharm.automation.dssui.pageObjects.PartnerLoginPage;
import com.tevapharm.automation.dssui.pageObjects.VendorToolPage;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class FailureScreenPositiveTest extends UiBaseTest {
    private final String partnerNameTemp = "";
    private String image1Hash;


    @Test(priority = 1, testName = "Invalid state token should navigate the user to failure screen")
    @Traceability(URS = {"x.x.x"}, FS = {"x.x.x"})
    public void tc01_getFailureScreen() throws IOException, NoSuchAlgorithmException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        File iconFile = new File("./icons/image1.jpg");
        image1Hash = FileUtils.getFileHash(iconFile);

        Response responseFile = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .multiPart("file", iconFile, "image/jpg")
                .log().all()
                .post("/configuration/image")
                .then()
                .log().all()
                .extract().response();

        JsonPath extract = responseFile.jsonPath();
        String iconUrl = extract.get("url");

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.icon = iconUrl;

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
        //registerPartnerID(partnerID);

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
        vendorToolPage.login("123456", apiKey);

        System.out.println(apiKey);

        PartnerLoginPage partnererLoginPage = new PartnerLoginPage(driver);
        boolean actual = partnererLoginPage.checkTitle();
        TevaAssert.assertTrue(extentTest,actual, "digihelerLoginPage is displayed");

        partnererLoginPage.login(PropertyUtils.readProperty("idHubUser"), PropertyUtils.readProperty("idHubPassword"));

        Base64.Encoder encoder = Base64.getEncoder();
        String stateToken = encoder.encodeToString("{\"patientID\": \"FAKE\", \"provisionID\": \"FAKE\"}".getBytes());
        LocalStorageUtils localStorageUtils = new LocalStorageUtils(driver);
        localStorageUtils.setItemInLocalStorage("stateToken", stateToken);
        driver.navigate().refresh();

        FailureScreenPage fs = new FailureScreenPage(driver);
        TevaAssert.assertEquals(extentTest, fs.getError(), "Account not linked", "Navigated to error message succesfully");


        //REVOKE API KEY AND DELETE PARTNER FROM DB
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
    }
}
