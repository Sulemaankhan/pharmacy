package com.pharmacy.inventory.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic stockCommandsTopic(@Value("${app.kafka.topics.stock-commands}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic stockRepliesTopic(@Value("${app.kafka.topics.stock-replies}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
