package com.tevapharm.automation.dssui.requests;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class CreatePartnerTemp {

	String apiKey = "";
	private final static ObjectMapper objectMapper = new ObjectMapper();

	public static Response updateInvalidRate(String partnerID, int rate) throws IOException, DatabindException, IOException {
		String accessToken = RestAssuredOAuth.getToken();

		PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
				PartnerRequest.class);
		partnerRequest.name = UUID.randomUUID().toString();
		partnerRequest.throttle.rate = rate;

		RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
		Response response = given().basePath("configuration/partners")
				.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
				.pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
				.then().log().all().extract().response();
		return response;

	}

	public static Response updateInvalidBurst(String partnerID, int burst) throws IOException, DatabindException, IOException {
		String accessToken = RestAssuredOAuth.getToken();

		PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
				PartnerRequest.class);
		partnerRequest.name = UUID.randomUUID().toString();
		partnerRequest.throttle.burst = burst;

		RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
		Response response = given().basePath("configuration/partners")
				.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
				.pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
				.then().log().all().extract().response();
		return response;
	}

	public static Response updateInvalidLimit(String partnerID, int limit) throws IOException, DatabindException, IOException {
		String accessToken = RestAssuredOAuth.getToken();

		PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
				PartnerRequest.class);
		partnerRequest.name = UUID.randomUUID().toString();
		partnerRequest.quota.limit = limit;

		RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
		Response response = given().basePath("configuration/partners")
				.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
				.pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
				.then().log().all().extract().response();
		return response;
	}

	public static Response updateInvalidPeriod(String partnerID, String period) throws IOException, DatabindException, IOException {
		String accessToken = RestAssuredOAuth.getToken();

		PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
				PartnerRequest.class);
		partnerRequest.name = UUID.randomUUID().toString();
		partnerRequest.quota.period = period;

		RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
		Response response = given().basePath("configuration/partners")
				.header("Authorization", "Bearer " + accessToken).header("Content-Type", "application/json")
				.pathParam("partnerID", partnerID).request().body(partnerRequest).when().log().all().put("/{partnerID}")
				.then().log().all().extract().response();
		return response;
	}

	public void revokeAndDeletePartner(String partnerID) throws StreamReadException, DatabindException, IOException {
		String accessToken = RestAssuredOAuth.getToken();

				RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
				Response response2 = given()
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

		Response response3 = given()
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
		
		Assert.assertEquals(response3.getStatusCode(), 200, "Request is expected to have HTTP Response Code `200`");
	}
	
	public static String generateApiKey(String partnerID) throws StreamReadException, DatabindException, IOException {
		String accessToken = RestAssuredOAuth.getToken();

		GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
				GenerateApiRequest.class);
		
		Response response2 = given()
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

		JsonPath extractor = response2.jsonPath();
		String apiKey = extractor.get("apiKey");
		return apiKey;
	}
}
