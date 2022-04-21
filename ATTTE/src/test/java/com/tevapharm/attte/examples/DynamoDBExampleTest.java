package com.tevapharm.attte.examples;

import com.tevapharm.attte.models.database.PartnerKey;
import com.tevapharm.attte.repository.PartnerRepository;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;


public class DynamoDBExampleTest {

    private PartnerRepository partnerRepository = new PartnerRepository();


    @Test(priority = 1)
    public void tc01_database_communication_test() throws IOException {

        List<PartnerKey> partnerKeys =partnerRepository.findApiKeyByPartnerID("377dccb9-1e94-439b-8834-948c63c3b879");

        PartnerKey partnerKey = partnerKeys.get(0);
        Assert.assertNotNull(partnerKey);
        partnerKey.setGrantAccessDate("2000-01-23");
        partnerRepository.persistPartnerKey(partnerKey);

        List<PartnerKey> updateKeys =partnerRepository.findApiKeyByPartnerID("377dccb9-1e94-439b-8834-948c63c3b879");
        PartnerKey updatedPartnerKey = partnerKeys.get(0);
        Assert.assertEquals("2000-01-23",updatedPartnerKey.getGrantAccessDate());

    }

}
