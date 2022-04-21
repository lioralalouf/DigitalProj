package com.tevapharm.attte.repository;

import com.tevapharm.attte.utils.PropertyUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


public class BaseDynamoRepository {

    protected  Region region;
    protected DynamoDbClient ddb;
    protected DynamoDbEnhancedClient enhancedClient;

    public BaseDynamoRepository() {

        if (PropertyUtils.readProperty("awsRegion").equals("us-east-1")) {
            region = Region.US_EAST_1;
        }

        /*
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                Utils.readProperty("awsKey"),
                Utils.readProperty("awsSecret"));

         */

        ddb = DynamoDbClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create() )
                .build();

        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
    }
}
