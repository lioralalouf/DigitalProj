package com.tevapharm.attte.models.reponse.data;

import java.util.List;

public class InhalerDataResponse {
    public List<Inhalers> inhalers;
    public Patients patient;

    public static class Patients {
        public String patient;
        public String consentStartDate;
    }

    public static class Inhalers {
        public String serialNumber;
        public long Inhalers;
        public String lastConnectionDate;
        public int deviceStatus;
        public String addedDate;
        public String drug;
        public String brandName;
        public String strength;
    }
}
