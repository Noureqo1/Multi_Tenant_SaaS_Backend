package Multi_TenantSaaS.SW452.Project.controller;

import Multi_TenantSaaS.SW452.Project.dto.JobStatusResponse;
import Multi_TenantSaaS.SW452.Project.service.JobService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{jobId}")
    public JobStatusResponse getJobStatus(@PathVariable UUID jobId) {
        return jobService.getJobStatus(jobId);
    }
}
