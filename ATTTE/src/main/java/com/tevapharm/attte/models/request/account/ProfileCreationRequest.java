package com.tevapharm.attte.models.request.account;

import com.tevapharm.attte.models.database.Profile;
import com.tevapharm.attte.models.request.PlatformRequestBase;

public class ProfileCreationRequest extends PlatformRequestBase {

    public ProfileDto patient;

    public ProfileCreationRequest(Profile profile) {
        patient = new ProfileDto();
        patient.firstName = profile.getFirstName();
        patient.lastName = profile.getLastName();
        patient.dateOfBirth = profile.getDateOfBirth();
        patient.locale = profile.getLocale();

    }


}
