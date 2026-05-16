package Multi_TenantSaaS.SW452.Project.messaging.outbox;

import Multi_TenantSaaS.SW452.Project.messaging.ReportJobMessage;
import Multi_TenantSaaS.SW452.Project.messaging.ReportJobProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxMessageRepository outboxMessageRepository;
    private final ReportJobProducer reportJobProducer;
    private final ObjectMapper objectMapper;

    public OutboxPublisherService(OutboxMessageRepository outboxMessageRepository,
                                  ReportJobProducer reportJobProducer,
                                  ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.reportJobProducer = reportJobProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingMessages() {
        List<OutboxMessage> pendingMessages =
                outboxMessageRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Outbox publisher found {} pending message(s)", pendingMessages.size());

        for (OutboxMessage outboxMessage : pendingMessages) {
            try {
                ReportJobMessage reportJobMessage =
                        objectMapper.readValue(outboxMessage.getPayload(), ReportJobMessage.class);

                reportJobProducer.publishReportJob(reportJobMessage);

                outboxMessage.markPublished();

                log.info("Outbox message published successfully: outboxId={}, aggregateId={}",
                        outboxMessage.getId(), outboxMessage.getAggregateId());

            } catch (Exception ex) {
                if (outboxMessage.getRetryCount() >= MAX_RETRY_COUNT) {
                    outboxMessage.markFailed();

                    log.error("Outbox message permanently failed after max retries: outboxId={}",
                            outboxMessage.getId(), ex);
                } else {
                    outboxMessage.retryLater();

                    log.warn("Outbox message publish failed and will retry later: outboxId={}, retryCount={}",
                            outboxMessage.getId(), outboxMessage.getRetryCount(), ex);
                }
            }
        }
    }
}