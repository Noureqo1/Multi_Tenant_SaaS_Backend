package Multi_TenantSaaS.SW452.Project.service;

import Multi_TenantSaaS.SW452.Project.domain.Job;
import Multi_TenantSaaS.SW452.Project.dto.GenerateReportResponse;
import Multi_TenantSaaS.SW452.Project.dto.JobStatusResponse;
import Multi_TenantSaaS.SW452.Project.messaging.ReportJobMessage;
import Multi_TenantSaaS.SW452.Project.messaging.outbox.OutboxMessage;
import Multi_TenantSaaS.SW452.Project.messaging.outbox.OutboxMessageRepository;
import Multi_TenantSaaS.SW452.Project.multitenancy.TenantContext;
import Multi_TenantSaaS.SW452.Project.repository.JobRepository;
import Multi_TenantSaaS.SW452.Project.repository.ProjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public JobService(JobRepository jobRepository,
                      ProjectRepository projectRepository,
                      OutboxMessageRepository outboxMessageRepository,
                      ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.projectRepository = projectRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates that the project belongs to the caller's tenant, creates a PENDING job,
     * stores the report generation event in the outbox table, and returns the jobId.
     *
     * Outbox Pattern:
     * The Job and OutboxMessage are saved in the same transaction. This prevents losing
     * the message if RabbitMQ is temporarily unavailable during the request.
     */
    @Transactional
    public GenerateReportResponse createAndEnqueueReportJob(Long projectId) {
        log.info("Creating report job for projectId={}", projectId);

        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found for id: " + projectId));

        String tenantId = TenantContext.getTenantIdOrDefault();

        Job job = new Job(tenantId, projectId);
        job = jobRepository.save(job);

        log.info("Job created: jobId={}, tenantId={}, projectId={}", job.getId(), tenantId, projectId);

        String correlationId = MDC.get("correlationId");

        ReportJobMessage message = new ReportJobMessage(
                job.getId(), tenantId, projectId, correlationId);

        String payload = serializeMessage(message);

        OutboxMessage outboxMessage = new OutboxMessage(
                tenantId,
                "Job",
                job.getId().toString(),
                "REPORT_GENERATION_REQUESTED",
                payload
        );

        outboxMessageRepository.save(outboxMessage);

        log.info("Outbox message created for report job: jobId={}, tenantId={}",
                job.getId(), tenantId);

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

    private String serializeMessage(ReportJobMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize report job message", ex);
        }
    }
}