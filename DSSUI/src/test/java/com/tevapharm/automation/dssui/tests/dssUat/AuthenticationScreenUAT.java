package com.tevapharm.automation.dssui.tests.dssUat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.models.request.GenerateApiRequest;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.testing.UiBaseTest;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.automation.dssui.pageObjects.ConsentPage;
import com.tevapharm.automation.dssui.pageObjects.PartnerLoginPage;
import com.tevapharm.automation.dssui.pageObjects.VendorToolPage;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.html5.WebStorage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;


public class AuthenticationScreenUAT extends UiBaseTest {
	String accessToken = "";
	String partnerID = "";
	String apiKey = "";
	String partnerNameTemp = "";
	private final ObjectMapper objectMapper = new ObjectMapper();



	@Test(priority = 1, description = "Generate api key to partner and getting the api key details, expecting the correct scopes to be displayed.")
	@Traceability(URS = "x.x.x", FS = "x.x.x")
	public void tc01_loginSuccesfully() throws IOException {
		accessToken = RestAssuredOAuth.getToken();

		PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
				PartnerRequest.class);
		partnerRequest.name = UUID.randomUUID().toString();

		RestAssured.baseURI = PropertyUtils.readProperty("adminUrl");
		String response = given()
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

		System.out.println(response);

		JsonPath js = new JsonPath(response);
		this.partnerID = js.getString("partnerID");
		this.partnerNameTemp = partnerRequest.name;
		
		GenerateApiRequest apiKeyRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newApiKey"),
				GenerateApiRequest.class);

		Response response2 = given()
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
		
		LocalStorage localStorage = ((WebStorage) driver).getLocalStorage();
		localStorage.removeItem("stateToken");
		
		VendorToolPage vendorToolPage = new VendorToolPage(driver);
		vendorToolPage.login("123456", apiKey);
		
		System.out.println(apiKey);
		
		PartnerLoginPage partnerLoginPage = new PartnerLoginPage(driver);
		boolean actual = partnerLoginPage.checkTitle();
		Assert.assertTrue(actual, "digihelerLoginPage is displayed");
		
		partnerLoginPage.login(PropertyUtils.readProperty("idHubUser"), PropertyUtils.readProperty("idHubPassword"));
		
		
		String consentTitle = PropertyUtils.readProperty("consentTitle");
		String partnerName = this.partnerNameTemp;
		ConsentPage cp = new ConsentPage(driver);
		String actualTitle  = cp.getTitleText();
		String expectedTitle = partnerName+" "+consentTitle;
		Assert.assertEquals(actualTitle, expectedTitle, "The correct title is displayed to the user");
	}
}
