package com.pharmacy.drugstore.config;

import com.pharmacy.drugstore.kafka.DomainEvent;
import com.pharmacy.drugstore.kafka.PaymentResult;
import com.pharmacy.drugstore.kafka.ShipmentResult;
import com.pharmacy.drugstore.kafka.StockResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentCommandsTopic(@Value("${app.kafka.topics.payment-commands}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic paymentRepliesTopic(@Value("${app.kafka.topics.payment-replies}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic paymentEventsTopic(@Value("${app.kafka.topics.payment-events}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic notificationCommandsTopic(@Value("${app.kafka.topics.notification-commands}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic shipmentCommandsTopic(@Value("${app.kafka.topics.shipment-commands}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic shipmentRepliesTopic(@Value("${app.kafka.topics.shipment-replies}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic shipmentEventsTopic(@Value("${app.kafka.topics.shipment-events}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic stockCommandsTopic(@Value("${app.kafka.topics.stock-commands}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic stockRepliesTopic(@Value("${app.kafka.topics.stock-replies}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic inventoryEventsTopic(@Value("${app.kafka.topics.inventory-events}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic orderEventsTopic(@Value("${app.kafka.topics.order-events}") String name) {
        return topic(name);
    }

    @Bean
    public NewTopic fulfilmentEventsTopic(@Value("${app.kafka.topics.fulfilment-events}") String name) {
        return topic(name);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildProducerProperties(null));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ConsumerFactory<String, DomainEvent> domainEventConsumerFactory(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        config.keySet().removeIf(key -> key.startsWith("spring.json."));
        JsonDeserializer<DomainEvent> deserializer = new JsonDeserializer<>(DomainEvent.class, false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> sagaEventListenerFactory(
            ConsumerFactory<String, DomainEvent> domainEventConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, DomainEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(domainEventConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(6).replicas(1).build();
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, PaymentResult> paymentResultConsumerFactory(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        config.keySet().removeIf(key -> key.startsWith("spring.json."));
        JsonDeserializer<PaymentResult> deserializer = new JsonDeserializer<>(PaymentResult.class, false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ReplyingKafkaTemplate<String, Object, PaymentResult> paymentReplyKafkaTemplate(
            ProducerFactory<String, Object> producerFactory,
            ConsumerFactory<String, PaymentResult> paymentResultConsumerFactory,
            @Value("${app.kafka.topics.payment-replies}") String repliesTopic) {
        ContainerProperties containerProperties = new ContainerProperties(repliesTopic);
        containerProperties.setGroupId("pharmacy-service-payment-replies");
        containerProperties.setMissingTopicsFatal(false);
        ConcurrentMessageListenerContainer<String, PaymentResult> replies =
                new ConcurrentMessageListenerContainer<>(paymentResultConsumerFactory, containerProperties);
        ReplyingKafkaTemplate<String, Object, PaymentResult> template =
                new ReplyingKafkaTemplate<>(producerFactory, replies);
        template.setDefaultReplyTimeout(Duration.ofSeconds(20));
        return template;
    }

    @Bean
    public ConsumerFactory<String, ShipmentResult> shipmentResultConsumerFactory(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        config.keySet().removeIf(key -> key.startsWith("spring.json."));
        JsonDeserializer<ShipmentResult> deserializer = new JsonDeserializer<>(ShipmentResult.class, false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ReplyingKafkaTemplate<String, Object, ShipmentResult> shipmentReplyKafkaTemplate(
            ProducerFactory<String, Object> producerFactory,
            ConsumerFactory<String, ShipmentResult> shipmentResultConsumerFactory,
            @Value("${app.kafka.topics.shipment-replies}") String repliesTopic) {
        ContainerProperties containerProperties = new ContainerProperties(repliesTopic);
        containerProperties.setGroupId("pharmacy-service-shipment-replies");
        containerProperties.setMissingTopicsFatal(false);
        ConcurrentMessageListenerContainer<String, ShipmentResult> replies =
                new ConcurrentMessageListenerContainer<>(shipmentResultConsumerFactory, containerProperties);
        ReplyingKafkaTemplate<String, Object, ShipmentResult> template =
                new ReplyingKafkaTemplate<>(producerFactory, replies);
        template.setDefaultReplyTimeout(Duration.ofSeconds(20));
        return template;
    }

    @Bean
    public ConsumerFactory<String, StockResult> stockResultConsumerFactory(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        config.keySet().removeIf(key -> key.startsWith("spring.json."));
        JsonDeserializer<StockResult> deserializer = new JsonDeserializer<>(StockResult.class, false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ReplyingKafkaTemplate<String, Object, StockResult> stockReplyKafkaTemplate(
            ProducerFactory<String, Object> producerFactory,
            ConsumerFactory<String, StockResult> stockResultConsumerFactory,
            @Value("${app.kafka.topics.stock-replies}") String repliesTopic) {
        ContainerProperties containerProperties = new ContainerProperties(repliesTopic);
        containerProperties.setGroupId("pharmacy-service-stock-replies");
        containerProperties.setMissingTopicsFatal(false);
        ConcurrentMessageListenerContainer<String, StockResult> replies =
                new ConcurrentMessageListenerContainer<>(stockResultConsumerFactory, containerProperties);
        ReplyingKafkaTemplate<String, Object, StockResult> template =
                new ReplyingKafkaTemplate<>(producerFactory, replies);
        template.setDefaultReplyTimeout(Duration.ofSeconds(20));
        return template;
    }
}
