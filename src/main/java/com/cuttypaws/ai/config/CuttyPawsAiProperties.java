package com.cuttypaws.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cuttypaws.ai")
public class CuttyPawsAiProperties {

    private boolean enabled = true;
    private Health health = new Health();
    private Search search = new Search();
    private Feed feed = new Feed();

    @Data
    public static class Health {
        private boolean disclaimerEnabled = true;
    }

    @Data
    public static class Search {
        private int defaultRadiusMiles = 25;
    }

    @Data
    public static class Feed {
        private int adInjectionInterval = 7;
        private int maxRecommendedProductsPerPage = 3;
    }
}