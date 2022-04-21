package com.tevapharm.attte.qualification;



import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.utils.TevaAssert;
import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;


@Listeners(TestListeners.class)
public class ApiNegativeMethodsTest {
    private String accessToken = "";
    private String partnerID = "";
    private String partnerNameTemp = "";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(testName = "Create a new partner in order for next test fail.")
    public void tc01_CreatePartner() throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        this.accessToken = RestAssuredOAuth.getToken();

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.contact.email = "beforeUpdate@gmail.com";

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response =
                given()
                        .filter(new ConsoleReportFilter(extentTest))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .request()
                        .body(partnerRequest)
                        .when().log().all()
                        .post("/configuration/partners")
                        .then().log().all()
                        .extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 200, "Onboard a new partner; request is expected to have HTTP Response Code `200`");

        JsonPath js = new JsonPath(response.asString());
        this.partnerID = js.getString("partnerID");
        this.partnerNameTemp = partnerRequest.name;
        Thread.sleep(2000);
    }

    @Test(testName = "Attempt to create a partner with a duplicate name in order to force the test to fail.")
    public void tc02_createDuplicatedPartner() throws IOException, InterruptedException {

        this.accessToken = RestAssuredOAuth.getToken();
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);

        partnerRequest.name = this.partnerNameTemp;

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response =
                given().log().all()
                        .filter(new ConsoleReportFilter(extentTest))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .request()
                        .body(partnerRequest)
                        .when().log().all()
                        .post("/configuration/partners")
                        .then().log().all()
                        .extract().response();

        TevaAssert.assertEquals(extentTest, response.getStatusCode(), 200, "Onboard a new partner; request is expected to have HTTP Response Code `200`");

        if (partnerID != null) {
            Response response8 = given()
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
        }

        Thread.sleep(2000);
    }
}
 
