package com.tevapharm.attte.repository;

import com.tevapharm.attte.utils.PropertyUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

public class BaseCloudWatchRepository {

    protected  Region region;
    protected CloudWatchLogsClient cloudWatchLogsClient;

    public BaseCloudWatchRepository() {

        if (PropertyUtils.readProperty("awsRegion").equals("us-east-1")) {
            region = Region.US_EAST_1;
        }

        cloudWatchLogsClient = CloudWatchLogsClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create() )
                .build();

    }
}
