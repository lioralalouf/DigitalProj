package com.tevapharm.attte.service;


import com.tevapharm.attte.models.database.PartnerKey;
import com.tevapharm.attte.models.database.PartnerUserConnection;
import com.tevapharm.attte.repository.PartnerRepository;
import com.tevapharm.attte.repository.PartnerUserConnectionRepository;
import com.tevapharm.attte.utils.DateUtils;

import java.util.List;

public class PartnerService {

    private final PartnerRepository partnerRepository = new PartnerRepository();
    private final PartnerUserConnectionRepository partnerUserConnectionRepository = new PartnerUserConnectionRepository();

    public void expireApiKey(String partnerID) {
        List<PartnerKey> partnerKeys =partnerRepository.findApiKeyByPartnerID(partnerID);

        for (PartnerKey partnerKey: partnerKeys) {
            partnerKey.setGrantAccessDate(DateUtils.getDate());
            partnerRepository.persistPartnerKey(partnerKey);
        }
    }

    public PartnerUserConnection findConsentByPatientPartner() {

        PartnerUserConnection partnerUserConnection = partnerUserConnectionRepository.findConsentByPatientPartner("ds","asd");

        return null;
    }

    public void updatePatientPartnerConsent(PartnerUserConnection partnerUserConnection) {
        partnerUserConnectionRepository.updatePatientPartnerConsent(partnerUserConnection);
    }
}
