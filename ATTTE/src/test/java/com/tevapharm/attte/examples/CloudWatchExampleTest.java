package com.tevapharm.attte.examples;

import com.tevapharm.attte.repository.DataApiAuditCWRepository;
import com.tevapharm.attte.utils.DateUtils;
import org.testng.annotations.Test;

import java.io.IOException;


public class CloudWatchExampleTest {

    private DataApiAuditCWRepository dataApiAuditCWRepository = new DataApiAuditCWRepository();

    @Test(priority = 1)
    public void tc01_cloudwatch_communication_test() throws IOException {


        long tenMinutesAgo = DateUtils.utcTimeMinutesAgo(10);
        long timeNow = DateUtils.utcTimeMinutesAgo(0);

        dataApiAuditCWRepository.findLogsByTimeRange(tenMinutesAgo, timeNow);

    }
}
