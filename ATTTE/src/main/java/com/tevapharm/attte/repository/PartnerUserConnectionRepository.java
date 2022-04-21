package com.tevapharm.attte.repository;


import com.tevapharm.attte.models.database.PartnerUserConnection;
import com.tevapharm.attte.utils.PropertyUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Iterator;

public class PartnerUserConnectionRepository extends BaseDynamoRepository {


    private static final String partnerUserConnectionTable = PropertyUtils.readProperty("partnerUserConnectionTable");

    public PartnerUserConnection findConsentByPatientPartner(String externalEntityID, String partnerID) {

        DynamoDbTable<PartnerUserConnection> mappedTable = enhancedClient.table(PartnerUserConnectionRepository.partnerUserConnectionTable, TableSchema.fromBean(PartnerUserConnection.class));

        Key key = Key.builder()
                .partitionValue(externalEntityID)
                .sortValue("consent#dataTransferConsent#" + partnerID)
                .build();

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(key);

        Iterator<PartnerUserConnection> results = mappedTable.query(queryConditional).items().iterator();


        while (results.hasNext()) {
            return results.next();
        }

        return null;
    }

    public void updatePatientPartnerConsent(PartnerUserConnection partnerUserConnection) {
        DynamoDbTable<PartnerUserConnection> table = enhancedClient.table(PartnerUserConnectionRepository.partnerUserConnectionTable, TableSchema.fromBean(PartnerUserConnection.class));
        table.putItem(partnerUserConnection);
    }
}
