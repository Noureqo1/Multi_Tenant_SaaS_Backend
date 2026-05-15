package Multi_TenantSaaS.SW452.Project.service;

import Multi_TenantSaaS.SW452.Project.domain.Job;
import Multi_TenantSaaS.SW452.Project.dto.GenerateReportResponse;
import Multi_TenantSaaS.SW452.Project.dto.JobStatusResponse;
import Multi_TenantSaaS.SW452.Project.messaging.ReportJobMessage;
import Multi_TenantSaaS.SW452.Project.messaging.ReportJobProducer;
import Multi_TenantSaaS.SW452.Project.multitenancy.TenantContext;
import Multi_TenantSaaS.SW452.Project.repository.JobRepository;
import Multi_TenantSaaS.SW452.Project.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final ReportJobProducer reportJobProducer;

    public JobService(JobRepository jobRepository,
                      ProjectRepository projectRepository,
                      ReportJobProducer reportJobProducer) {
        this.jobRepository = jobRepository;
        this.projectRepository = projectRepository;
        this.reportJobProducer = reportJobProducer;
    }

    /**
     * Validates that the project belongs to the caller's tenant, creates a PENDING job,
     * publishes a message to RabbitMQ, and returns the jobId with PENDING status.
     */
    @Transactional
    public GenerateReportResponse createAndEnqueueReportJob(Long projectId) {
        log.info("Creating report job for projectId={}", projectId);

        // Validate project exists (scoped to current tenant via Hibernate multi-tenancy)
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found for id: " + projectId));

        // Get the current tenant context
        String tenantId = TenantContext.getTenantIdOrDefault();

        // Persist job with PENDING status
        Job job = new Job(tenantId, projectId);
        job = jobRepository.save(job);

        log.info("Job created: jobId={}, tenantId={}, projectId={}", job.getId(), tenantId, projectId);

        // Get correlation ID from MDC to propagate into the message
        String correlationId = MDC.get("correlationId");

        // Publish message to RabbitMQ
        ReportJobMessage message = new ReportJobMessage(
                job.getId(), tenantId, projectId, correlationId);
        reportJobProducer.publishReportJob(message);

        return new GenerateReportResponse(job.getId(), job.getStatus().name());
    }

    /**
     * Returns the current status of a job.
     */
    @Transactional(readOnly = true)
    public JobStatusResponse getJobStatus(UUID jobId) {
        log.info("Fetching job status for jobId={}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found for id: " + jobId));

        return new JobStatusResponse(
                job.getId(),
                job.getProjectId(),
                job.getStatus().name(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}
