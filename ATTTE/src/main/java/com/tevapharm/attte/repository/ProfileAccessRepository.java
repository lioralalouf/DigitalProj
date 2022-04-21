package com.tevapharm.attte.repository;


import com.tevapharm.attte.models.database.ProfileAccess;
import com.tevapharm.attte.utils.PropertyUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;


public class ProfileAccessRepository extends BaseDynamoRepository {

    private static final String profileAccessTable = PropertyUtils.readProperty("profileAccessTable");

    public void updateLastAccessByExternalEntityID(ProfileAccess profileAccessModel) {

    }

    public Object getLastAccessByExternalEntityID(String externalEntityID) {

        return null;
    }

    public ProfileAccess findByExternalEntityID(String externalEntityID) {

        DynamoDbTable<ProfileAccess> mappedTable = enhancedClient.table(ProfileAccessRepository.profileAccessTable, TableSchema.fromBean(ProfileAccess.class));

        Key key = Key.builder()
                .partitionValue(externalEntityID)
                .build();

        return mappedTable.getItem(key);

    }
}
