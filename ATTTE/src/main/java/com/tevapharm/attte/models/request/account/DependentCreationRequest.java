package com.tevapharm.attte.models.request.account;

import com.tevapharm.attte.models.database.Profile;
import com.tevapharm.attte.models.request.PlatformRequestBase;

public class DependentCreationRequest extends PlatformRequestBase {

    public DependentDto dependent;

    public DependentCreationRequest(Profile profile) {
        dependent = new DependentDto();
        dependent.firstName = profile.getFirstName();
        dependent.lastName = profile.getLastName();
        dependent.dateOfBirth = profile.getDateOfBirth();
        dependent.locale = profile.getLocale();
        dependent.dependentID = profile.getExternalEntityID();
        dependent.ageOfMajority = profile.getAgeOfMajority();

    }
}
