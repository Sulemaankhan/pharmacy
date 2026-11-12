package com.pharmacy.drugstore.reliability;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaReliabilityConfig {

    @Bean
    public NewTopic sagaDeadLetterTopic(@Value("${app.kafka.topics.saga-dlt}") String name) {
        return TopicBuilder.name(name).partitions(6).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.saga-dlt}") String dlt) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, ex) -> new TopicPartition(dlt, record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxElapsedTime(12_000L);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                org.springframework.dao.DataIntegrityViolationException.class,
                org.hibernate.exception.DataException.class);
        handler.setCommitRecovered(true);
        return handler;
    }
}
