package com.pharmacy.drugstore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter(objectMapper);
        return RestClient.builder()
                .configureMessageConverters(converters -> converters
                        .registerDefaults()
                        .withJsonConverter(json));
    }
}
