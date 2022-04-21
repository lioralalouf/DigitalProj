package com.tevapharm.attte.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import org.testng.annotations.BeforeMethod;


public class ApiBaseTest {
    protected String accessToken = "";
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeMethod
    public void setup() {
        accessToken = RestAssuredOAuth.getToken();
    }


}
