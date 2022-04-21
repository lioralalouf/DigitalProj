package com.tevapharm.automation.dhp.tests.configuration.image;

import com.aventstack.extentreports.ExtentTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.annotations.Traceability;
import com.tevapharm.attte.extentReports.ExtentManager;
import com.tevapharm.attte.extentReports.TestListeners;
import com.tevapharm.attte.models.request.PartnerRequest;
import com.tevapharm.attte.reporter.ConsoleReportFilter;
import com.tevapharm.attte.testing.PartnerApiTestBase;
import com.tevapharm.attte.utils.FileUtils;
import com.tevapharm.attte.utils.PropertyUtils;
import com.tevapharm.attte.utils.TevaAssert;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Listeners(TestListeners.class)
public class ImageConfiguration extends PartnerApiTestBase {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String partnerID;

    @Test(priority = 1, testName = "Verify icon image has been uploaded successfully to the partner")
    @Traceability(URS = {"1700"})
    public void tc01_uploadNewImage() throws IOException, NoSuchAlgorithmException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());

        extentTest.info("Onboard a new image to S3 Bucket and save the image hash");
        File iconFile = new File("./icons/image1.jpg");
        String image1Hash = FileUtils.getFileHash(iconFile);

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
        extentTest.info("Extract the image URL from the HTTP Response");
        String iconUrl = extract.get("url");

        extentTest.info("Onboard a new partner and set the URL to logo field");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.icon = iconUrl;

        this.partnerID = createPartner(extentTest, partnerRequest);

        extentTest.info("Get the partner details, Verify response image hash and path are identical to the original image hash and path");
        Response response2 =
                given()
                        .filter(new ConsoleReportFilter(extentTest))
                        .baseUri(PropertyUtils.readProperty("adminUrl"))
                        .basePath("configuration/partners")
                        .header("Authorization", "Bearer " + accessToken)
                        .pathParam("partnerID", partnerID)
                        .when()
                        .log().all()
                        .when()
                        .get("/{partnerID}")
                        .then()
                        .log().all()
                        .assertThat().statusCode(200)
                        .extract().response();

        JsonPath extractor = response2.jsonPath();
        String ResponsePartnerID = extractor.get("partnerID");
        String partnerIconPath = extractor.get("icon");
        String imageDownloadHash = FileUtils.getRemoteFileHash(partnerIconPath);

        TevaAssert.assertEquals(extentTest, partnerID, ResponsePartnerID, "Partner ID should be identical");
        TevaAssert.assertEquals(extentTest, partnerIconPath, iconUrl, "Icon path should be identical");
        TevaAssert.assertEquals(extentTest, image1Hash, imageDownloadHash, "Icon hash should be identical");
    }

    @Test(priority = 2, testName = "Verify icon image has been updated successfully to the partner")
    @Traceability(URS = {"1700"})
    public void tc02_UpdateImage() throws IOException, NoSuchAlgorithmException, InterruptedException {
        ExtentTest extentTest = ExtentManager.getTest(this.getClass());
        //second upload
        extentTest.info("Onboard a new image to S3 Bucket and save the image hash");
        File iconFile2 = new File("./icons/image2.jpg");
        String image2Hash = FileUtils.getFileHash(iconFile2);

        Response responseFile2 = given()
                .filter(new ConsoleReportFilter(extentTest))
                .baseUri(PropertyUtils.readProperty("adminUrl"))
                .header("Authorization", "Bearer " + accessToken)
                .multiPart("file", iconFile2, "image/jpg")
                .log().all()
                .post("/configuration/image")
                .then()
                .log().all()
                .extract().response();

        extentTest.info("Extract the image URL from the HTTP Response");
        JsonPath extract = responseFile2.jsonPath();
        String iconUrl2 = extract.get("url");

        extentTest.info("Update the partner details and set the new URL to logo field");
        PartnerRequest partnerRequest = objectMapper.readValue(PropertyUtils.readRequest("partner", "newPartner"),
                PartnerRequest.class);
        partnerRequest.name = UUID.randomUUID().toString();
        partnerRequest.icon = iconUrl2;

        updatePartner(extentTest, partnerRequest, partnerID);

        extentTest.info("Get the partner details, Verify response image hash and path are identical to the 'Updated' image hash and path");
        Response response2 =
                given()
                        .filter(new ConsoleReportFilter(extentTest))
                        .baseUri(PropertyUtils.readProperty("adminUrl"))
                        .basePath("configuration/partners")
                        .header("Authorization", "Bearer " + accessToken)
                        .pathParam("partnerID", partnerID)
                        .when()
                        .log().all()
                        .when()
                        .get("/{partnerID}")
                        .then()
                        .log().all()
                        .assertThat().statusCode(200)
                        .extract().response();

        JsonPath extractor2 = response2.jsonPath();
        String ResponsePartnerID2 = extractor2.get("partnerID");
        String partnerIconPath2 = extractor2.get("icon");
        String imageDownloadHash2 = FileUtils.getRemoteFileHash(partnerIconPath2);

        TevaAssert.assertEquals(extentTest, partnerID, ResponsePartnerID2, "Partner ID should be identical");
        TevaAssert.assertEquals(extentTest, partnerIconPath2, iconUrl2, "Icon path should be identical");
        TevaAssert.assertEquals(extentTest, image2Hash, imageDownloadHash2, "Icon hash should be identical");
    }
}
