package com.tevapharm.attte.service;


import com.tevapharm.attte.models.database.Job;
import com.tevapharm.attte.repository.JobsRepository;

import java.util.List;

public class JobsService {

    private final JobsRepository jobsRepository = new JobsRepository();

    public void removeJobByFunction(String functionName) {
        jobsRepository.removeByFunction(functionName);
    }

    public List<Job> findJobByFunction(String functionName) {
        return jobsRepository.findJobsByFunction(functionName);
    }
}
