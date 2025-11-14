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

package com.bytechef.ee.tenant.service;

import com.bytechef.ee.tenant.repository.TenantRepository;
import com.bytechef.ee.tenant.util.TenantUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.domain.Tenant;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import liquibase.integration.spring.MultiTenantSpringLiquibase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
public class MultiTenantService implements TenantService, ResourceLoaderAware {

    private static final Log log = LogFactory.getLog(MultiTenantService.class);

    private static final ReentrantLock LOCK = new ReentrantLock();

    private final TenantRepository tenantRepository;
    private final DataSource dataSource;
    private final LiquibaseProperties liquibaseProperties;
    private ResourceLoader resourceLoader;

    @SuppressFBWarnings("EI")
    public MultiTenantService(
        TenantRepository tenantRepository, DataSource dataSource, LiquibaseProperties liquibaseProperties) {

        this.tenantRepository = tenantRepository;
        this.dataSource = dataSource;
        this.liquibaseProperties = liquibaseProperties;
    }

    @Override
    public String createTenant() {
        try {
            LOCK.lock();

            String tenantId = tenantRepository.findMaxTenantId();

            DecimalFormat decimalFormat = new DecimalFormat("000000");

            tenantId = decimalFormat.format(Integer.parseInt(tenantId) + 1);

            tenantRepository.createTenant(tenantId);

            initTenant(tenantId, "multitenant");

            log.info("Tenant created: " + tenantId);

            return tenantId;
        } finally {
            LOCK.unlock();
        }
    }

    public void createTenant(String tenantId) {
        tenantRepository.createTenant(tenantId);
    }

    @Override
    public void deleteTenant(String tenantId) {
        tenantRepository.deleteTenant(tenantId);

        log.info("Tenant deleted: " + tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTenantIdByUserActivationKey(String key) {
        return tenantRepository.findTenantIdByUserActivationKey(key);
    }

    @Transactional(readOnly = true)
    public String getTenantIdByOrganizationName(String organizationName) {
        return tenantRepository.findTenantIdByOrganizationName(organizationName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTenantIdsByUserEmail(String email) {
        return tenantRepository.findTenantIdsByUserEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTenantIdsByUserLogin(String login) {
        return tenantRepository.findTenantIdsByUserLogin(login);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTenantIdByUserResetKey(String key) {
        return tenantRepository.findTenantIdByUserResetKey(key);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTenantIds() {
        return tenantRepository.findTenantIds();
    }

    @Transactional(readOnly = true)
    public List<Tenant> getTenants() {
        return tenantRepository.findTenants();
    }

    public void initTenant(String tenantId, String contexts) {
        loadChangelog(Collections.singletonList(tenantId), contexts);
    }

    @Override
    public boolean isMultiTenantEnabled() {
        return true;
    }

    @Override
    public void loadChangelog(List<String> tenantIds, String contexts) {
        MultiTenantSpringLiquibase multiTenantSpringLiquibase = new MultiTenantSpringLiquibase();

        multiTenantSpringLiquibase.setDataSource(dataSource);
        multiTenantSpringLiquibase.setResourceLoader(resourceLoader);

        List<String> schemas =
            tenantIds
                .stream()
                .map(TenantUtils::getDatabaseSchema)
                .collect(Collectors.toList());

        multiTenantSpringLiquibase.setSchemas(schemas);
        multiTenantSpringLiquibase.setChangeLog("classpath:config/liquibase/master.xml");

        if (contexts == null) {
            multiTenantSpringLiquibase.setContexts(String.join(",", liquibaseProperties.getContexts()));
        } else {
            multiTenantSpringLiquibase.setContexts(contexts);
        }

        multiTenantSpringLiquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        multiTenantSpringLiquibase.setDropFirst(liquibaseProperties.isDropFirst());
        multiTenantSpringLiquibase.setParameters(liquibaseProperties.getParameters());

        try {
            multiTenantSpringLiquibase.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tenantIdsByUserEmailExist(String email) {
        return !getTenantIdsByUserEmail(email).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tenantIdsByUserLoginExist(String email) {
        return !getTenantIdsByUserLogin(email).isEmpty();
    }
}
