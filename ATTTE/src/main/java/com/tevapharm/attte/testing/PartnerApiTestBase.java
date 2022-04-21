package com.tevapharm.attte.testing;

import com.aventstack.extentreports.ExtentTest;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.annotations.AfterClass;

import java.io.IOException;
import java.util.*;

import static io.restassured.RestAssured.given;

public class PartnerApiTestBase extends ApiBaseTest {

    protected List<String> partnerIDs = new ArrayList<>();
    private Map<String, List<String>> apiKeys = new HashMap<>();

    public void registerPartnerID(String partnerID) {
        this.partnerIDs.add(partnerID);
        apiKeys.put(partnerID, new ArrayList<>());
    }

    public void registerApiKey(String partnerID, String apiKey) {

        if (!partnerIDs.contains(partnerID)) {
            this.partnerIDs.add(partnerID);
        }

        if (!apiKeys.containsKey(partnerID)) {
            apiKeys.put(partnerID, new ArrayList<>());
        }

        apiKeys.get(partnerID).add(apiKey);
    }

    public String createPartner(ExtentTest extentTest, PartnerRequest partnerRequest) throws InterruptedException {
        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        Response response;
        do {
            response = given()
                    .filter(new ConsoleReportFilter(extentTest))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .request()
                    .body(partnerRequest)
                    .when()
                    .log().all()
                    .post("/configuration/partners")
                    .then()
                    .log().all()
                    .assertThat()
                    .extract().response();

            if (response.getStatusCode() != 200) {
                Thread.sleep(20_000);
            }

        } while (response.getStatusCode() != 200);


        JsonPath js = new JsonPath(response.asString());
        String partnerID = js.getString("partnerID");
        registerPartnerID(partnerID);

        return partnerID;
    }

    public void updatePartner(ExtentTest extentTest, PartnerRequest partnerRequest, String partnerID) throws InterruptedException {

        Response response;
        do {
            response = given()
                    .filter(new ConsoleReportFilter(extentTest))
                    .baseUri(PropertyUtils.readProperty("adminUrl"))
                    .basePath("configuration/partners")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .pathParam("partnerID", partnerID)
                    .request()
                    .body(partnerRequest)
                    .when().log().all()
                    .put("/{partnerID}")
                    .then().log().all()
                    .extract().response();

            if (response.getStatusCode() != 200) {
                Thread.sleep(20_000);
            }

        } while(response.getStatusCode() != 200);
    }

    public String generateApiKey(ExtentTest extentTest, String partnerID) throws IOException {

        extentTest.info("Generate a new Api key for onboarded partner");
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
                .assertThat()
                .statusCode(200)
                .extract().response();

        JsonPath extractor = response.jsonPath();
        String apiKey = extractor.get("apiKey");

        apiKeys.get(partnerID).add(apiKey);

        return apiKey;
    }


    public String createPartner(ExtentTest extentTest) throws IOException, InterruptedException {

        Thread.sleep(1000);
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();

        RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
        String response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .request()
                .body(partnerRequest)
                .when()
                .log().all()
                .post("/configuration/partners")
                .then()
                .log().all()
                .assertThat()
                .statusCode(200)
                .extract().response().asString();

        JsonPath js = new JsonPath(response);
        String partnerID = js.getString("partnerID");
        registerPartnerID(partnerID);

        return partnerID;
    }

    @AfterClass
    public void cleanUp() throws InterruptedException {

        if (partnerIDs.size() > 0) {

            for (String partnerID : partnerIDs) {
                Thread.sleep(1000);

                for (String apiKey : apiKeys.get(partnerID)) {

                    Thread.sleep(1000);

                    if (apiKey != null) {
                        Response response;
                        do {
                            response = given()
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

                            if (response.getStatusCode() == 400) {
                                Thread.sleep(2000);
                            }

                        } while (response.getStatusCode() == 400);

                    }
                }

                Response response;

                do {
                    response = given()
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

                    if (response.getStatusCode() == 400) {
                        Thread.sleep(20_000);
                    }

                } while (response.getStatusCode() == 400);
            }

            partnerIDs.clear();
        }
    }

    public Response generateApiKeyByPartner(ExtentTest extentTest, String key) throws IOException {

        Response response = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("Content-Type", "application/json")
                .header("X-API-Key", key)
                .when()
                .post("/data/api/key")
                .then()
                .log().all()
                .extract().response();

        //JsonPath extractor = response.jsonPath();
        // String apiKey = extractor.get("apiKey");
        return response;
    }

    public Response getApiKeyByPartner(ExtentTest extentTest, String key) {
        Response response2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("platformUrl"))
                .header("X-API-Key", key)
                .when()
                .get("/data/api/key")
                .then()
                .log()
                .all()
                .extract()
                .response();
        return response2;
    }

    public String getProvisionID(String stateToken) throws ParseException {
        Base64.Decoder dec = Base64.getDecoder();
        String decodedToken = new String(dec.decode(stateToken));
        JSONParser parser = new JSONParser();
        JSONObject jsonToken = (JSONObject) parser.parse(decodedToken);
        return jsonToken.get("provisionID").toString();
    }

    public String getPartnerID(String stateToken) throws ParseException {
        Base64.Decoder dec = Base64.getDecoder();
        String decodedToken = new String(dec.decode(stateToken));
        JSONParser parser = new JSONParser();
        JSONObject jsonToken = (JSONObject) parser.parse(decodedToken);
        return jsonToken.get("partnerID").toString();
    }
}
