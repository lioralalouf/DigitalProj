package com.tevapharm.attte.service;


import com.tevapharm.attte.models.database.MedicalDevice;
import com.tevapharm.attte.repository.InhalerRepository;

public class InhalerService {

    private final InhalerRepository inhalerRepository = new InhalerRepository();

    public MedicalDevice findInhalerBySerialNumber(String externalEntityID, Long serialNumber) {
        return inhalerRepository.findInhalerBySerialNumber(externalEntityID, serialNumber);
    }

    public void updateInhaler(MedicalDevice medicalDevice) {
        inhalerRepository.updateInhaler(medicalDevice);
    }

}
