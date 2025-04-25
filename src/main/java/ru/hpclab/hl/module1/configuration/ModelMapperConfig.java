package ru.hpclab.hl.module1.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.hpclab.hl.module1.dto.MovieDto;
import ru.hpclab.hl.module1.service.CacheService;

@Configuration
public class ModelMapperConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheService<MovieDto> movieCache() {
        return new CacheService<>("MovieDto");
    }
}
