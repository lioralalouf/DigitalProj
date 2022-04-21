package com.tevapharm.attte.qualification;

import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.DataProviders;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
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
public class ApiParametersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(priority = 1, testName = "Create partner with dynamic parameter to prove the tool can run the same test with different parameters"
            , dataProvider = "getPeriodTrue", dataProviderClass = DataProviders.class)
    @Traceability(URS = {"1724"})
    public void tc01_dynamic_parameters_test(String period) throws IOException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass(), period);

        String accessToken = RestAssuredOAuth.getToken();

        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.quota.period = period;
        extentTest.pass("create partner with parameter - " + period);

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("cognito-role", "Admin")
                .request()
                .body(partnerRequest)
                .when()
                .log().all()
                .post("/configuration/partners")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath extractor = response.jsonPath();
        String partnerID = extractor.get("partnerID");

        //delete partner from DB
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

        Thread.sleep(20_000);
    }
}
