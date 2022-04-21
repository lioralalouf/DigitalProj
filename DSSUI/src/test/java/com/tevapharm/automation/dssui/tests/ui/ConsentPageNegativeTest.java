package com.tevapharm.automation.dssui.tests.ui;

import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class ConsentPageNegativeTest extends UiBaseTest {
    private String partnerID = "";
    private final String partnerNameTemp = "";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(priority = 1, testName = "Image Upload", description = "Upload image and check its been uploaded successfully with the correct image path.")
    @Traceability(URS = {"x.x.x"}, FS = {"x.x.x"})
    public void tc01_testIconPathDeletedImage() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        File iconFile = new File("./icons/image3.jpg");
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

        PartnerLoginPage digihelerLoginPage = new PartnerLoginPage(driver);
        boolean actual = digihelerLoginPage.checkTitle();
        TevaAssert.assertTrue(extentTest, actual, "digihelerLoginPage is displayed");

        digihelerLoginPage.login(PropertyUtils.readProperty("idHubUser"), PropertyUtils.readProperty("idHubPassword"));

        ConsentPage cp = new ConsentPage(driver);
        String iconPath = cp.getIconPath();
        TevaAssert.assertEquals(extentTest, iconPath, iconUrl, "Icon path should be identical");
        Thread.sleep(8000);
        driver.navigate().to(PropertyUtils.readProperty("mockToolUrl"));
        String imageForDelete = iconUrl.substring(iconUrl.lastIndexOf("/") + 1);

        Response responseFile2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                //.basePath("/configuration/image")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                //.multiPart("file", iconUrl, "image/jpg")
                .log().all()
                .delete("/configuration/image/" + imageForDelete)
                .then()
                .log().all()
                .extract().response();

        vendorToolPage = new VendorToolPage(driver);
        vendorToolPage.login("123456", apiKey);
    }

    @Test(priority = 2, testName = "test2", description = "Upload image and check its been uploaded successfully with the correct image path.")
    @Traceability(URS = {"x.x.x"}, FS = {"x.x.x"})
    public void tc02_checkUnregisteredUsers() throws IOException {
        ConsentPage cp = new ConsentPage(driver);
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        TevaAssert.assertFalse(extentTest, cp.checkUserIsNotDisplayed("dependent1"), "");
        TevaAssert.assertFalse(extentTest, cp.checkUserIsNotDisplayed("dependent2"), "");

        //revoke apikey and delete partner
        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given()
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

        Response response2 = given()
                .filter(new ConsoleReportFilter(extentTest))
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

        TevaAssert.assertEquals(extentTest, response2.getStatusCode(), 200, "Request is expected to have HTTP Response Code `200`");
    }
}
