package ru.sirius.hl.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.sirius.hl.dto.MovieDto;
import ru.sirius.hl.service.CacheService;

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
