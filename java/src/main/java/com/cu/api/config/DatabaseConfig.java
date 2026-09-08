package com.cu.api.config;

import java.nio.file.Path;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Loads {@code data/seed.sql} into the in-memory database before anything reads it. */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    public DataSourceInitializer seedDatabase(DataSource dataSource) {
        Path seed = SeedLocator.locate();
        log.info("Seeding the in-memory database from {}", seed);

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new FileSystemResource(seed));
        populator.setSqlScriptEncoding("UTF-8");

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
