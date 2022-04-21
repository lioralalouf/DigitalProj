package com.tevapharm.attte.authentication;

import com.tevapharm.attte.utils.PropertyUtils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.Base64;

import static io.restassured.RestAssured.given;

public class RestAssuredOAuth {


  //  public static String username = PropertyUtils.readProperty("serviceUser");
  //  public static String password = PropertyUtils.readProperty("servicePassword");

    public static String username = "18gg0bjhlk917hugnq493vnd30";
    public static String password = "m9g2g4f6290de4j9lkraptsol7q08888cgv3i4j1699gor08fts";
    //public static String username = System.getProperty("serviceUser");
    //public static String password = System.getProperty("servicePassword");


    public static String encode(String str1, String str2) {
        return new String(Base64.getEncoder().encode((str1 + ":" + str2).getBytes()));
        
    }
	
    public static Response getCode() {
    	RestAssured.baseURI = PropertyUtils.readProperty("onicaUrl");
        String authorization = encode(PropertyUtils.readProperty("serviceUser"), PropertyUtils.readProperty("servicePassword"));

        return
                given().log().all()
                        .header("authorization", "Basic " + authorization)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("{\"grant_type\":\"client_credentials\"}")
                        .post("oauth2/token")
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();
    }

    public static String parseForOAuth2Code(Response response) {
        return response.jsonPath().getString("access_token");
        
    }
 
    public static String iShouldGetCode() {
        Response response = getCode();
        String code = parseForOAuth2Code(response);
        //Assert.assertNotNull(code);
		return code;
    }



   // @Step("get token{0}")
	public static String getToken() {
		RestAssured.baseURI = PropertyUtils.readProperty("onicaUrl");
		Response response = given()
				.auth().preemptive().basic(PropertyUtils.readProperty("serviceUser"), PropertyUtils.readProperty("servicePassword"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("cognito-role", "Admin")
				.formParam("grant_type", "client_credentials")
				.when()
				.post("oauth2/token")
		  .then()
		  .extract()
		  .response();
	
		JsonPath js = response.jsonPath();
		String accessToken = js.getString("access_token");
		return accessToken;
	}

}

