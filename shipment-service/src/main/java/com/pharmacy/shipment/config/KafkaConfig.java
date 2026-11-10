package com.pharmacy.shipment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic shipmentCommandsTopic(@Value("${app.kafka.topics.shipment-commands}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic shipmentRepliesTopic(@Value("${app.kafka.topics.shipment-replies}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic shipmentUpdatesTopic(@Value("${app.kafka.topics.shipment-updates}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
