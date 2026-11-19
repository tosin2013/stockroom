package dev.stockroom;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

/**
 * Rejects non-integer numbers for integer fields (for example {@code {"stock": 1.5}})
 * instead of silently truncating them to the integer part, so such a request answers
 * {@code 400} as a Problem Detail (spec 010 FR-002).
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public JsonMapperBuilderCustomizer strictIntegerDeserialization() {
        return builder -> builder.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}
