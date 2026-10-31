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

package com.bytechef.automation.search.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacadeImpl;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.automation.configuration.service.WorkspaceServiceImpl;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.search.SearchResult;
import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.callback.UserCallback;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs {@link AutomationSearchFacadeImpl} with the real {@code UserService}, {@code WorkspaceFacade},
 * {@code WorkspaceService} and Community Edition {@code PermissionService} over Postgres. The search providers are
 * recording test beans: they stand in for the per-asset providers so the test can observe which workspace ids the
 * facade hands them.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationSearchFacadeIntTest.AutomationSearchFacadeIntTestConfiguration.class)
@Transactional
class AutomationSearchFacadeIntTest {

    private static final String LOGIN = "search-user";

    @Autowired
    private AutomationSearchFacade automationSearchFacade;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private RecordingSearchAssetProvider connectionSearchAssetProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecordingSearchAssetProvider workflowSearchAssetProvider;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @BeforeEach
    void beforeEach() {
        connectionSearchAssetProvider.reset();
        workflowSearchAssetProvider.reset();

        workspaceRepository.deleteAll();

        Authority userAuthority = authorityRepository.findByName(AuthorityConstants.USER)
            .orElseThrow();

        User user = new User();

        user.setActivated(true);
        user.setAuthorityIds(List.of(userAuthority.getId()));
        user.setEmail(LOGIN + "@example.com");
        user.setLangKey("en");
        user.setLogin(LOGIN);
        user.setPassword("x".repeat(60));

        userRepository.save(user);

        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    LOGIN, null, List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testProvidersAreScopedToCallerWorkspaces() {
        Workspace firstWorkspace = workspaceRepository.save(new Workspace("first-search-workspace"));
        Workspace secondWorkspace = workspaceRepository.save(new Workspace("second-search-workspace"));

        workflowSearchAssetProvider.setResults(List.of(new TestSearchResult(1L, SearchAssetType.WORKFLOW)));

        List<SearchResult<?>> results = automationSearchFacade.search("q", 10);

        assertThat(workflowSearchAssetProvider.getWorkspaceIds())
            .containsExactlyInAnyOrder(firstWorkspace.getId(), secondWorkspace.getId());
        assertThat(results).containsExactly(new TestSearchResult(1L, SearchAssetType.WORKFLOW));
    }

    @Test
    void testCallerWithoutWorkspacesGetsNoResults() {
        workflowSearchAssetProvider.setResults(List.of(new TestSearchResult(1L, SearchAssetType.WORKFLOW)));

        List<SearchResult<?>> results = automationSearchFacade.search("q", 10);

        assertThat(results).isEmpty();
        assertThat(connectionSearchAssetProvider.getWorkspaceIds()).isNull();
        assertThat(workflowSearchAssetProvider.getWorkspaceIds()).isNull();
    }

    @Test
    void testEveryProviderIsQueried() {
        Workspace workspace = workspaceRepository.save(new Workspace("search-workspace"));

        connectionSearchAssetProvider.setResults(List.of(new TestSearchResult(2L, SearchAssetType.CONNECTION)));
        workflowSearchAssetProvider.setResults(List.of(new TestSearchResult(3L, SearchAssetType.WORKFLOW)));

        List<SearchResult<?>> results = automationSearchFacade.search("q", 10);

        assertThat(connectionSearchAssetProvider.getWorkspaceIds()).containsExactly(workspace.getId());
        assertThat(workflowSearchAssetProvider.getWorkspaceIds()).containsExactly(workspace.getId());
        assertThat(results).containsExactlyInAnyOrder(
            new TestSearchResult(2L, SearchAssetType.CONNECTION), new TestSearchResult(3L, SearchAssetType.WORKFLOW));
    }

    static class RecordingSearchAssetProvider implements SearchAssetProvider {

        private final SearchAssetType searchAssetType;
        private final AtomicReference<List<TestSearchResult>> results = new AtomicReference<>(List.of());
        private final AtomicReference<Set<Long>> workspaceIds = new AtomicReference<>();

        RecordingSearchAssetProvider(SearchAssetType searchAssetType) {
            this.searchAssetType = searchAssetType;
        }

        @Override
        public SearchAssetType getAssetType() {
            return searchAssetType;
        }

        @Override
        public List<? extends SearchResult> search(String query, int limit, Set<Long> workspaceIds) {
            this.workspaceIds.set(workspaceIds);

            return results.get();
        }

        Set<Long> getWorkspaceIds() {
            return workspaceIds.get();
        }

        void reset() {
            results.set(List.of());
            workspaceIds.set(null);
        }

        void setResults(List<TestSearchResult> results) {
            this.results.set(results);
        }
    }

    record TestSearchResult(Long id, SearchAssetType type) implements SearchResult<Long> {

        @Override
        public String name() {
            return "name-" + id;
        }

        @Override
        public String description() {
            return null;
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({
        AutomationSearchFacadeImpl.class, CacheConfiguration.class, LiquibaseConfiguration.class,
        PermissionServiceImpl.class, PostgreSQLContainerConfiguration.class, UserCallback.class, UserServiceImpl.class,
        WorkspaceFacadeImpl.class, WorkspaceServiceImpl.class
    })
    static class AutomationSearchFacadeIntTestConfiguration {

        @Bean
        RecordingSearchAssetProvider connectionSearchAssetProvider() {
            return new RecordingSearchAssetProvider(SearchAssetType.CONNECTION);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        RecordingSearchAssetProvider workflowSearchAssetProvider() {
            return new RecordingSearchAssetProvider(SearchAssetType.WORKFLOW);
        }

        @Configuration
        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class AutomationSearchFacadeIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
