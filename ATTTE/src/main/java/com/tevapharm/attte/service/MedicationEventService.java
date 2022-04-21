package com.tevapharm.attte.service;

import com.tevapharm.attte.repository.MedicationAdministrationEventRepository;

public class MedicationEventService {

    private final MedicationAdministrationEventRepository medicationAdministrationEventRepository = new MedicationAdministrationEventRepository();

    public void removeByID(String id) {
        medicationAdministrationEventRepository.removeByID(id);
    }
}
