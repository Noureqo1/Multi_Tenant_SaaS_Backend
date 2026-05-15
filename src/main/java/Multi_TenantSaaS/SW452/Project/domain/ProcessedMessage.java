package Multi_TenantSaaS.SW452.Project.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency table for message processing.
 * Before processing a job message, the consumer checks if the jobId already exists here.
 * If it does, the message is skipped to prevent duplicate processing.
 */
@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {

    @Id
    private UUID id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedMessage() {}

    public ProcessedMessage(UUID id) {
        this.id = id;
        this.processedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
