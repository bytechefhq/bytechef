/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.platform.ai.skill.security.AiSkillOwnershipResolver;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.service.UserService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringJUnitConfig(AiSkillFacadeIntTest.AiSkillFacadeIntTestConfiguration.class)
class AiSkillFacadeIntTest {

    private static final long ADMIN_USER_ID = 3L;
    private static final String BODY_REACHED = "body reached";
    private static final long OTHER_SKILL_ID = 20L;
    private static final long OTHER_USER_ID = 2L;
    private static final long OWNER_SKILL_ID = 10L;
    private static final long OWNER_USER_ID = 1L;

    @Autowired
    private AiSkillFacade aiSkillFacade;

    @Autowired
    private AiSkillFileStorage aiSkillFileStorage;

    @Autowired
    private AiSkillService aiSkillService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        AiSkill ownerSkill = aiSkill(OWNER_SKILL_ID, "owner");
        AiSkill otherSkill = aiSkill(OTHER_SKILL_ID, "other");

        when(aiSkillService.fetchAiSkill(OWNER_SKILL_ID)).thenReturn(Optional.of(ownerSkill));
        when(aiSkillService.fetchAiSkill(OTHER_SKILL_ID)).thenReturn(Optional.of(otherSkill));
        when(aiSkillService.getAiSkill(OWNER_SKILL_ID)).thenReturn(ownerSkill);
        when(aiSkillService.getAiSkills()).thenReturn(List.of(ownerSkill, otherSkill));
        when(aiSkillFileStorage.readAiSkillFileBytes(any())).thenReturn(new byte[] {
            1, 2, 3
        });

        when(userService.getUser(anyString())).thenThrow(new UserNotFoundException());

        for (User user : List.of(
            user(OWNER_USER_ID, "owner"), user(OTHER_USER_ID, "other"), user(ADMIN_USER_ID, "admin"))) {

            when(userService.fetchUserByLogin(user.getLogin())).thenReturn(Optional.of(user));

            doReturn(user).when(userService)
                .getUser(user.getLogin());
        }
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        reset(aiSkillFileStorage, aiSkillService, userService);
    }

    @Test
    void testOwnerIsAllowed() {
        authenticate(platformUser("owner", "ROLE_USER"));

        assertThat(aiSkillFacade.getAiSkill(OWNER_SKILL_ID)
            .getId()).isEqualTo(OWNER_SKILL_ID);
    }

    @Test
    void testNonOwnerIsDenied() {
        authenticate(platformUser("other", "ROLE_USER"));

        assertThatThrownBy(() -> aiSkillFacade.getAiSkill(OWNER_SKILL_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testTenantAdminIsAllowed() {
        authenticate(platformUser("admin", "ROLE_ADMIN"));

        assertThat(aiSkillFacade.getAiSkill(OWNER_SKILL_ID)
            .getId()).isEqualTo(OWNER_SKILL_ID);
    }

    @Test
    void testConnectedUserIsDeniedEvenWithTheOwnersLoginAndAnAdminAuthority() {
        authenticate(connectedUser("owner", "ROLE_ADMIN"));

        assertThatThrownBy(() -> aiSkillFacade.getAiSkill(OWNER_SKILL_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUnauthenticatedCallerIsDenied() {
        authenticate(anonymous());

        assertThatThrownBy(() -> aiSkillFacade.getAiSkill(OWNER_SKILL_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions("skill", null, "instructions"))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testNonAdminListsOnlyOwnSkills() {
        authenticate(platformUser("owner", "ROLE_USER"));

        assertThat(aiSkillFacade.getAiSkills())
            .extracting(AiSkill::getId)
            .containsExactly(OWNER_SKILL_ID);
    }

    @Test
    void testTenantAdminListsAllSkills() {
        authenticate(platformUser("admin", "ROLE_ADMIN"));

        assertThat(aiSkillFacade.getAiSkills())
            .extracting(AiSkill::getId)
            .containsExactly(OWNER_SKILL_ID, OTHER_SKILL_ID);
    }

    @Test
    void testConnectedUserIsRefusedSkillListing() {
        authenticate(connectedUser("owner", "ROLE_ADMIN"));

        assertThatThrownBy(() -> aiSkillFacade.getAiSkills()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testOwnerMayDownloadOwnSkill() {
        authenticate(platformUser("owner", "ROLE_USER"));

        assertThat(aiSkillFacade.getAiSkillDownload(OWNER_SKILL_ID)).containsExactly(1, 2, 3);
    }

    @Test
    void testNonOwnerMayNotDownloadAnotherUsersSkill() {
        authenticate(platformUser("other", "ROLE_USER"));

        assertThatThrownBy(() -> aiSkillFacade.getAiSkillDownload(OWNER_SKILL_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testEveryFacadeMethodDeniesAnUnauthenticatedCaller() {
        authenticate(anonymous());

        for (Method method : getFacadeMethods()) {
            if (isPostFiltered(method)) {
                assertThat((List<?>) invoke(method))
                    .as(method.toGenericString())
                    .isEmpty();
            } else {
                assertThatThrownBy(() -> invoke(method))
                    .as(method.toGenericString())
                    .isInstanceOf(AccessDeniedException.class);
            }
        }
    }

    @Test
    void testEveryResourceMethodDeniesANonOwner() {
        authenticate(platformUser("other", "ROLE_USER"));

        assertResourceMethodsDenied();
    }

    @Test
    void testEveryResourceMethodDeniesAConnectedUser() {
        authenticate(connectedUser("owner", "ROLE_ADMIN"));

        assertResourceMethodsDenied();
    }

    @Test
    void testEveryResourceMethodAdmitsTheOwner() {
        authenticate(platformUser("owner", "ROLE_USER"));

        assertResourceMethodsReachTheBody();
    }

    @Test
    void testEveryResourceMethodAdmitsATenantAdmin() {
        authenticate(platformUser("admin", "ROLE_ADMIN"));

        assertResourceMethodsReachTheBody();
    }

    @Test
    void testEveryResourceMethodHasACase() {
        List<String> resourceMethodNames = getFacadeMethods().stream()
            .filter(method -> method.getParameterCount() > 0 && method.getParameterTypes()[0] == long.class)
            .map(Method::getName)
            .toList();

        assertThat(resourceMethodNames).containsExactlyInAnyOrderElementsOf(getResourceMethodCalls().keySet());
    }

    @Test
    void testNonOwnerListsOnlyOwnSkills() {
        authenticate(platformUser("other", "ROLE_USER"));

        assertThat(aiSkillFacade.getAiSkills())
            .extracting(AiSkill::getId)
            .containsExactly(OTHER_SKILL_ID);
    }

    private void assertResourceMethodsDenied() {
        for (Map.Entry<String, ThrowingCallable> entry : getResourceMethodCalls().entrySet()) {
            assertThatThrownBy(entry.getValue())
                .as(entry.getKey())
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    private void assertResourceMethodsReachTheBody() {
        when(aiSkillService.getAiSkill(OWNER_SKILL_ID)).thenThrow(new IllegalStateException(BODY_REACHED));
        when(aiSkillService.updateAiSkill(eq(OWNER_SKILL_ID), anyString(), any()))
            .thenThrow(new IllegalStateException(BODY_REACHED));
        when(aiSkillService.updateAiSkillTags(eq(OWNER_SKILL_ID), any()))
            .thenThrow(new IllegalStateException(BODY_REACHED));

        for (Map.Entry<String, ThrowingCallable> entry : getResourceMethodCalls().entrySet()) {
            assertThatThrownBy(entry.getValue())
                .as(entry.getKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }
    }

    private Map<String, ThrowingCallable> getResourceMethodCalls() {
        Map<String, ThrowingCallable> resourceMethodCalls = new LinkedHashMap<>();

        resourceMethodCalls.put(
            "createAdditionalFilesInSkill",
            () -> aiSkillFacade.createAdditionalFilesInSkill(OWNER_SKILL_ID, Map.of("notes.md", "notes")));
        resourceMethodCalls.put("deleteAiSkill", () -> aiSkillFacade.deleteAiSkill(OWNER_SKILL_ID));
        resourceMethodCalls.put("getAiSkill", () -> aiSkillFacade.getAiSkill(OWNER_SKILL_ID));
        resourceMethodCalls.put("getAiSkillDownload", () -> aiSkillFacade.getAiSkillDownload(OWNER_SKILL_ID));
        resourceMethodCalls.put(
            "getAiSkillFileContent", () -> aiSkillFacade.getAiSkillFileContent(OWNER_SKILL_ID, "SKILL.md"));
        resourceMethodCalls.put("getAiSkillFilePaths", () -> aiSkillFacade.getAiSkillFilePaths(OWNER_SKILL_ID));
        resourceMethodCalls.put(
            "getAiSkillWithDownload", () -> aiSkillFacade.getAiSkillWithDownload(OWNER_SKILL_ID));
        resourceMethodCalls.put(
            "removeFileInSkill", () -> aiSkillFacade.removeFileInSkill(OWNER_SKILL_ID, "notes.md"));
        resourceMethodCalls.put("updateAiSkill", () -> aiSkillFacade.updateAiSkill(OWNER_SKILL_ID, "skill", null));
        resourceMethodCalls.put(
            "updateAiSkillContent", () -> aiSkillFacade.updateAiSkillContent(OWNER_SKILL_ID, "notes.md", "notes"));
        resourceMethodCalls.put(
            "updateAiSkillTags", () -> aiSkillFacade.updateAiSkillTags(OWNER_SKILL_ID, List.of()));

        return resourceMethodCalls;
    }

    private List<Method> getFacadeMethods() {
        return Arrays.stream(AiSkillFacade.class.getMethods())
            .sorted((method1, method2) -> method1.toGenericString()
                .compareTo(method2.toGenericString()))
            .toList();
    }

    private Object invoke(Method method) {
        Object[] arguments = Stream.of(method.getParameterTypes())
            .map(AiSkillFacadeIntTest::getArgument)
            .toArray();

        try {
            return method.invoke(aiSkillFacade, arguments);
        } catch (IllegalAccessException illegalAccessException) {
            throw new IllegalStateException(illegalAccessException);
        } catch (InvocationTargetException invocationTargetException) {
            if (invocationTargetException.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException(invocationTargetException.getCause());
        }
    }

    private boolean isPostFiltered(Method method) {
        try {
            Method targetMethod = AopUtils.getTargetClass(aiSkillFacade)
                .getMethod(method.getName(), method.getParameterTypes());

            return targetMethod.isAnnotationPresent(PostFilter.class);
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new IllegalStateException(noSuchMethodException);
        }
    }

    private static Object getArgument(Class<?> parameterType) {
        if (parameterType == long.class) {
            return OWNER_SKILL_ID;
        }

        if (parameterType == String.class) {
            return "SKILL.md";
        }

        if (parameterType == byte[].class) {
            return new byte[0];
        }

        if (parameterType == List.class) {
            return new ArrayList<>();
        }

        if (parameterType == Map.class) {
            return Map.of();
        }

        throw new IllegalArgumentException("Unsupported parameter type " + parameterType);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static AiSkill aiSkill(long id, String createdBy) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setCreatedBy(createdBy);
        aiSkill.setId(id);
        aiSkill.setSkillFile(new FileEntry("skill.zip", "memory://skill.zip"));

        return aiSkill;
    }

    private static Authentication anonymous() {
        return new AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    private static Authentication connectedUser(String login, String authority) {
        return new ConnectedUserAuthenticationToken(login, AuthorityUtils.createAuthorityList(authority));
    }

    private static Authentication platformUser(String login, String authority) {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(authority);

        return UsernamePasswordAuthenticationToken.authenticated(
            new org.springframework.security.core.userdetails.User(login, "", authorities), null, authorities);
    }

    private static User user(long id, String login) {
        User user = new User();

        user.setActivated(true);
        user.setId(id);
        user.setLogin(login);

        return user;
    }

    @Configuration
    @ComponentScan(
        basePackageClasses = AiSkillFacade.class, useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.AiSkillFacadeImpl"))
    @EnableMethodSecurity
    static class AiSkillFacadeIntTestConfiguration {

        @Bean
        static AiSkillFileStorage aiSkillFileStorage() {
            return mock(AiSkillFileStorage.class);
        }

        @Bean
        static AiSkillService aiSkillService() {
            return mock(AiSkillService.class);
        }

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            AiSkillService aiSkillService, UserService userService) {

            PermissionServiceImpl permissionService = new PermissionServiceImpl(
                new CurrentUserResolver(userService), mock(PermissionScopeRegistry.class),
                mock(ProjectRepository.class), mock(WorkspaceScopeCacheService.class),
                mock(WorkspaceUserRepository.class),
                List.of(new AiSkillOwnershipResolver(aiSkillService, userService)), List.of(),
                new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class));

            return new AutomationMethodSecurityExpressionHandler(permissionService);
        }

        @Bean
        static TagService tagService() {
            return mock(TagService.class);
        }

        @Bean
        static UserService userService() {
            return mock(UserService.class);
        }
    }

    private static final class ConnectedUserAuthenticationToken extends UsernamePasswordAuthenticationToken
        implements ConnectedUserAuthentication {

        private ConnectedUserAuthenticationToken(String login, List<GrantedAuthority> authorities) {
            super(new org.springframework.security.core.userdetails.User(login, "", authorities), null, authorities);
        }

        @Override
        public long connectedUserId() {
            return 1L;
        }

        @Override
        public String externalUserId() {
            return getName();
        }

        @Override
        public long environmentId() {
            return 0L;
        }
    }
}
