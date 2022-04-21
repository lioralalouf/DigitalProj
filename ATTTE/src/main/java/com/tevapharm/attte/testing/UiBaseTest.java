package com.tevapharm.attte.testing;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tevapharm.attte.authentication.RestAssuredOAuth;
import com.tevapharm.attte.utils.PropertyUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class UiBaseTest {
    protected String accessToken = "";
    protected String partnerID = "";
    protected String apiKey = "";
    protected final ObjectMapper objectMapper = new ObjectMapper();

    public WebDriver driver;
    private String hash;
    private String filename;

    @BeforeClass
    public void setup(ITestContext testContext) {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get(PropertyUtils.readProperty("url"));
        testContext.setAttribute("WebDriver", this.driver);

        accessToken = RestAssuredOAuth.getToken();
    }

    public void takeScreenshot(ExtentTest extentTest) throws IOException {
        String screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);

        extentTest.pass("<b><font color=green>" + "Screenshot \uD83D\uDCF7 (" + driver.getCurrentUrl() + "):" + "</font></b>",
                MediaEntityBuilder.createScreenCaptureFromBase64String(screenshot).build());
    }

    @BeforeMethod(alwaysRun = true)
    public void BeforeMethod(Method method) throws IOException {

        Class clazz = method.getDeclaringClass();
        String path = clazz.getPackage().getName().replaceAll("\\.", "/");

        String currentDirectory = System.getProperty("user.dir");
        this.filename = currentDirectory + "/src/test/java/" + path + "/" + clazz.getSimpleName() + ".java";
        ProcessBuilder processBuilder = new ProcessBuilder();
        StringBuilder output = new StringBuilder();

        processBuilder.command("git", "hash-object", filename);

        Process process = null;
        process = processBuilder.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line);
            output.append(System.getProperty("line.separator"));
        }

        System.out.println("Filename: " + filename);
        System.out.println("Hash: " + output);

        this.hash = output.toString();

    }


   // @AfterClass
    public void tearDown() {
        driver.quit();
    }


}
