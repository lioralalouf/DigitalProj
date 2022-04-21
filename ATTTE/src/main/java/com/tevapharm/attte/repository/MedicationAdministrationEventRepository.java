package com.tevapharm.attte.repository;


import com.tevapharm.attte.models.database.MedicationAdministrationEvent;
import com.tevapharm.attte.utils.PropertyUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;


public class MedicationAdministrationEventRepository extends BaseDynamoRepository {

    private static final String medicationEventsTable = PropertyUtils.readProperty("medicationEventsTable");

    public boolean removeByID(String id) {

        DynamoDbTable<MedicationAdministrationEvent> mappedTable = enhancedClient.table(MedicationAdministrationEventRepository.medicationEventsTable, TableSchema.fromBean(MedicationAdministrationEvent.class));

        Key key = Key.builder()
                .partitionValue(id)
                .build();

        mappedTable.deleteItem(key);

        return true;
    }


}
