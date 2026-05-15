package Multi_TenantSaaS.SW452.Project.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportJobProducer {

    private static final Logger log = LoggerFactory.getLogger(ReportJobProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final Counter publishedCounter;

    @Value("${workhub.rabbitmq.exchange}")
    private String exchange;

    @Value("${workhub.rabbitmq.routing-key}")
    private String routingKey;

    public ReportJobProducer(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;

        // Counter — incremented each time a report job message is published
        this.publishedCounter = Counter.builder("workhub.jobs.published")
                .description("Total report jobs enqueued")
                .register(meterRegistry);
    }

    public void publishReportJob(ReportJobMessage message) {
        log.info("Publishing report job message: jobId={}, projectId={}, tenantId={}",
                message.getJobId(), message.getProjectId(), message.getTenantId());

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        publishedCounter.increment();

        log.info("Report job message published successfully: jobId={}", message.getJobId());
    }
}
