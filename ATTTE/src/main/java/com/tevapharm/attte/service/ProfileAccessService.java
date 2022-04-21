package com.tevapharm.attte.service;


import com.tevapharm.attte.models.database.ProfileAccess;
import com.tevapharm.attte.repository.ProfileAccessRepository;

public class ProfileAccessService {

    private final ProfileAccessRepository profileAccessRepository = new ProfileAccessRepository();

    public void updateLastAccess(String externalEntityID, long accessTimestamp, Long foregroundAccess, Long backgroundAccess ) {

    }

    public ProfileAccess findByExternalEntityID(String externalEntityID) {
        return profileAccessRepository.findByExternalEntityID(externalEntityID);
    }
}
