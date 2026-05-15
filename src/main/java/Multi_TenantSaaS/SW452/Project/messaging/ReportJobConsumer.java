package Multi_TenantSaaS.SW452.Project.messaging;

import Multi_TenantSaaS.SW452.Project.domain.Job;
import Multi_TenantSaaS.SW452.Project.domain.JobStatus;
import Multi_TenantSaaS.SW452.Project.domain.ProcessedMessage;
import Multi_TenantSaaS.SW452.Project.multitenancy.TenantContext;
import Multi_TenantSaaS.SW452.Project.repository.JobRepository;
import Multi_TenantSaaS.SW452.Project.repository.ProcessedMessageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consumes report generation messages from RabbitMQ.
 *
 * Reliability strategy: IDEMPOTENCY KEY
 * Before processing, the consumer checks if the jobId already exists in the
 * processed_messages table. If it does, the message is skipped to prevent
 * duplicate processing. This is simpler and more portable than configuring
 * retry + DLQ, and sufficient for this use case.
 */
@Component
public class ReportJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportJobConsumer.class);

    private final JobRepository jobRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final Counter completedCounter;

    public ReportJobConsumer(JobRepository jobRepository,
                             ProcessedMessageRepository processedMessageRepository,
                             MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.processedMessageRepository = processedMessageRepository;

        // Counter — incremented on each successful job completion
        this.completedCounter = Counter.builder("workhub.jobs.completed")
                .description("Total report jobs completed")
                .register(meterRegistry);
    }

    @RabbitListener(queues = "${workhub.rabbitmq.queue}")
    @Transactional
    public void handleReportJob(ReportJobMessage message) {
        // Restore correlation ID from message payload into MDC for this consumer thread
        String correlationId = message.getCorrelationId();
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlationId", correlationId);
        }

        // Set tenant context from the message so DB writes are scoped to the correct schema
        String tenantId = message.getTenantId();

        try {
            log.info("Received report job message: jobId={}, projectId={}, tenantId={}",
                    message.getJobId(), message.getProjectId(), tenantId);

            // Idempotency check: skip if this job was already processed
            if (processedMessageRepository.existsById(message.getJobId())) {
                log.warn("Duplicate message detected, skipping: jobId={}", message.getJobId());
                return;
            }

            // Set tenant context for DB operations
            TenantContext.setTenantId(tenantId);

            // Find and update job to PROCESSING
            Job job = jobRepository.findById(message.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found: " + message.getJobId()));

            job.setStatus(JobStatus.PROCESSING);
            jobRepository.save(job);
            log.info("Job marked as PROCESSING: jobId={}", message.getJobId());

            // Simulate work (e.g., report generation)
            Thread.sleep(2000);

            // Mark job as COMPLETED
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            // Record processed message for idempotency
            processedMessageRepository.save(new ProcessedMessage(message.getJobId()));

            completedCounter.increment();
            log.info("Job completed successfully: jobId={}", message.getJobId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markJobFailed(message);
            log.error("Job interrupted: jobId={}, correlationId={}", message.getJobId(), correlationId, e);
        } catch (Exception e) {
            markJobFailed(message);
            log.error("Job failed: jobId={}, correlationId={}", message.getJobId(), correlationId, e);
        } finally {
            MDC.clear();
            TenantContext.clear();
        }
    }

    private void markJobFailed(ReportJobMessage message) {
        try {
            jobRepository.findById(message.getJobId()).ifPresent(job -> {
                job.setStatus(JobStatus.FAILED);
                job.setCompletedAt(Instant.now());
                jobRepository.save(job);
            });
        } catch (Exception e) {
            log.error("Failed to mark job as FAILED: jobId={}", message.getJobId(), e);
        }
    }
}
