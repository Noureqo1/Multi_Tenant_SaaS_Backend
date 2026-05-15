package Multi_TenantSaaS.SW452.Project.messaging;

import java.io.Serializable;
import java.util.UUID;

/**
 * Message payload for report generation jobs.
 * The correlationId field propagates the request's correlation ID
 * into the async consumer thread for end-to-end tracing.
 */
public class ReportJobMessage implements Serializable {

    private UUID jobId;
    private String tenantId;
    private Long projectId;
    private String correlationId;

    public ReportJobMessage() {}

    public ReportJobMessage(UUID jobId, String tenantId, Long projectId, String correlationId) {
        this.jobId = jobId;
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.correlationId = correlationId;
    }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    @Override
    public String toString() {
        return "ReportJobMessage{" +
                "jobId=" + jobId +
                ", tenantId='" + tenantId + '\'' +
                ", projectId=" + projectId +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
