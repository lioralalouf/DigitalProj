package com.tevapharm.attte.repository;


import com.tevapharm.attte.models.database.PushNotificationHistory;
import com.tevapharm.attte.utils.PropertyUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Iterator;

public class PushNotificationHistoryRepository extends BaseDynamoRepository {

    private static final String pushNotificationHistoryTable = PropertyUtils.readProperty("pushNotificationHistoryTable");

    public PushNotificationHistory findNotificationByExternalEntityID(String externalEntityID, String notificationType) {

        DynamoDbTable<PushNotificationHistory> mappedTable = enhancedClient.table(PushNotificationHistoryRepository.pushNotificationHistoryTable, TableSchema.fromBean(PushNotificationHistory.class));

        Key key = Key.builder()
                .partitionValue(externalEntityID)
                .build();

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(key);

        Iterator<PushNotificationHistory> results = mappedTable.query(queryConditional).items().iterator();
        PushNotificationHistory pushNotificationHistory = null;

        while (results.hasNext()) {
            pushNotificationHistory = results.next();

            if (pushNotificationHistory.getSkey().contains(notificationType)) {
                return pushNotificationHistory;
            }
        }

        return pushNotificationHistory;
    }

    public void persist(PushNotificationHistory pushNotificationHistory) {
        DynamoDbTable<PushNotificationHistory> mappedTable = enhancedClient.table(PushNotificationHistoryRepository.pushNotificationHistoryTable, TableSchema.fromBean(PushNotificationHistory.class));

        mappedTable.putItem(pushNotificationHistory);
    }

}
