/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.oauth2.authorizationserver.config;

import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantScope;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.sql.BaseDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Turns the test PostgreSQL database into a schema-per-tenant database: the {@link DataSource} sets {@code search_path}
 * from {@link TenantContext} on every connection, and the module changelogs run into {@code public} (context
 * {@code mono}, via the regular Liquibase bean) and into one schema per tenant (context {@code multitenant}).
 *
 * @author Ivica Cardic
 */
@TestConfiguration(proxyBeanMethods = false)
public class MultiTenantSchemaIntTestConfiguration {

    public static final String ACME_TENANT_ID = "acme";
    public static final String GLOBEX_TENANT_ID = "globex";
    public static final List<String> TENANT_IDS = List.of(ACME_TENANT_ID, GLOBEX_TENANT_ID);

    private static final String CHANGELOG = "classpath:config/liquibase/master.xml";
    private static final String MULTITENANT_CONTEXT = "multitenant";

    @Bean
    static BeanPostProcessor tenantSchemaDataSourceBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantSchemaDataSource)) {
                    return new TenantSchemaDataSource(dataSource);
                }

                return bean;
            }
        };
    }

    @Bean
    @DependsOn("liquibase")
    TenantSchemas tenantSchemas(DataSource dataSource, ResourceLoader resourceLoader) {
        return new TenantSchemas(dataSource, resourceLoader);
    }

    public static String getDatabaseSchema(String tenantId) {
        return TenantScope.call(tenantId, TenantContext::getCurrentDatabaseSchema);
    }

    private static final class TenantSchemaDataSource extends BaseDataSource {

        private TenantSchemaDataSource(DataSource dataSource) {
            super(dataSource);
        }
    }

    /**
     * Creates one schema per tenant and applies the {@code multitenant} changesets to it, the way the multi-tenant
     * edition provisions a new tenant.
     */
    public static final class TenantSchemas implements InitializingBean {

        private final DataSource dataSource;
        private final ResourceLoader resourceLoader;

        @SuppressFBWarnings("EI2")
        private TenantSchemas(DataSource dataSource, ResourceLoader resourceLoader) {
            this.dataSource = dataSource;
            this.resourceLoader = resourceLoader;
        }

        @Override
        @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
        public void afterPropertiesSet() throws LiquibaseException {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            for (String tenantId : TENANT_IDS) {
                String databaseSchema = getDatabaseSchema(tenantId);

                jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + databaseSchema);

                SpringLiquibase springLiquibase = new SpringLiquibase();

                springLiquibase.setChangeLog(CHANGELOG);
                springLiquibase.setContexts(MULTITENANT_CONTEXT);
                springLiquibase.setDataSource(dataSource);
                springLiquibase.setDefaultSchema(databaseSchema);
                springLiquibase.setResourceLoader(resourceLoader);

                runInTenant(tenantId, springLiquibase);
            }
        }

        private static void runInTenant(String tenantId, SpringLiquibase springLiquibase) throws LiquibaseException {
            String previousTenantId = TenantContext.getCurrentTenantId();

            TenantContext.setCurrentTenantId(tenantId);

            try {
                springLiquibase.afterPropertiesSet();
            } finally {
                TenantContext.setCurrentTenantId(previousTenantId);
            }
        }
    }
}
