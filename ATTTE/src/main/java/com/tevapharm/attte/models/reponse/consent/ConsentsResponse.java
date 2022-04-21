package com.tevapharm.attte.models.reponse.consent;

import java.util.List;

public class ConsentsResponse {

    public List<Consent> consents;

    public static class Consent {
        public String version;
        public List<Locales> locales;

        public static class Locales {
            public String locale;
            public List<String> legalTypes;
        }
    }
}
