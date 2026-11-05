/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.user.scim.config.ScimIntTestConfiguration;
import com.bytechef.ee.platform.user.scim.exception.ScimBadRequestException;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ScimIntTestConfiguration.class)
@Transactional
class ScimUserFacadeIntTest {

    private static final long MISSING_USER_ID = Long.MAX_VALUE;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ScimUserFacade scimUserFacade;

    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(cacheName))
                .clear();
        }
    }

    @Test
    void testIsDeactivationForScimUser() {
        assertThat(ScimUserFacade.isDeactivation(createUser(true), createScimUser("john.doe", false))).isTrue();
        assertThat(ScimUserFacade.isDeactivation(createUser(true), createScimUser("john.doe", true))).isFalse();
        assertThat(ScimUserFacade.isDeactivation(createUser(false), createScimUser("john.doe", false))).isFalse();
        assertThat(ScimUserFacade.isDeactivation(createUser(false), createScimUser("john.doe", true))).isFalse();
    }

    @Test
    void testIsDeactivationForPatchRecognizesEveryFormOfActiveFalse() {
        List<ScimPatchRequest> scimPatchRequests = List.of(
            createPatchRequest(new ScimPatchOperation("replace", "active", "false")),
            createPatchRequest(new ScimPatchOperation("Replace", "active", Boolean.FALSE)),
            createPatchRequest(new ScimPatchOperation("replace", null, Map.of("active", false))),
            createPatchRequest(new ScimPatchOperation("Add", "active", "false")),
            createPatchRequest(new ScimPatchOperation("add", null, Map.of("active", false))));

        for (ScimPatchRequest scimPatchRequest : scimPatchRequests) {
            assertThat(ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest)).isTrue();
            assertThat(ScimUserFacade.isDeactivation(createUser(false), scimPatchRequest)).isFalse();
        }
    }

    @Test
    void testIsDeactivationForPatchUsesTheLastActiveOperation() {
        ScimPatchRequest deactivateThenActivate = createPatchRequest(
            new ScimPatchOperation("replace", "active", "false"), new ScimPatchOperation("replace", "active", "true"));
        ScimPatchRequest activateThenDeactivate = createPatchRequest(
            new ScimPatchOperation("replace", "active", "true"), new ScimPatchOperation("replace", "active", "false"));
        ScimPatchRequest withoutActive = createPatchRequest(
            new ScimPatchOperation("replace", "name.givenName", "John"),
            new ScimPatchOperation("replace", null, Map.of("displayName", "John Doe")));

        assertThat(ScimUserFacade.isDeactivation(createUser(true), deactivateThenActivate)).isFalse();
        assertThat(ScimUserFacade.isDeactivation(createUser(true), activateThenDeactivate)).isTrue();
        assertThat(ScimUserFacade.isDeactivation(createUser(true), withoutActive)).isFalse();
    }

    @Test
    void testIsDeactivationForPatchRejectsRemoveOfActive() {
        ScimPatchRequest scimPatchRequest = createPatchRequest(new ScimPatchOperation("remove", "active", null));

        assertScimBadRequest(
            () -> ScimUserFacade.isDeactivation(createUser(true), scimPatchRequest),
            ScimBadRequestException.MUTABILITY);
    }

    @Test
    void testCreateUserStoresTheScimAttributes() {
        ScimUser scimUser = new ScimUser(
            List.of(ScimUser.SCHEMA), null, "ext-1", "John.Doe", new ScimName("John Doe", "Doe", "John"), "John Doe",
            List.of(
                new ScimEmail("john.home@example.com", "home", false),
                new ScimEmail("john.work@example.com", "work", true)),
            true, null, null);

        User createdUser = scimUserFacade.createUser(scimUser);

        User storedUser = fetchStoredUser(createdUser.getId());

        assertThat(storedUser.getLogin()).isEqualTo("john.doe");
        assertThat(storedUser.getEmail()).isEqualTo("john.work@example.com");
        assertThat(storedUser.getFirstName()).isEqualTo("John");
        assertThat(storedUser.getLastName()).isEqualTo("Doe");
        assertThat(storedUser.getExternalId()).isEqualTo("ext-1");
        assertThat(storedUser.getLangKey()).isEqualTo(UserConstants.DEFAULT_LANGUAGE);
        assertThat(storedUser.isActivated()).isTrue();
    }

    @Test
    void testCreateUserStoresTheExternalIdSoItCanBeFilteredByExactly() {
        User createdUser = scimUserFacade.createUser(createScimUser("john.doe", "00u-john", true));

        scimUserFacade.createUser(createScimUser("jane.doe", "00u-jane", true));

        assertThat(userService.getUsersByExternalId("00u-john"))
            .extracting(User::getId)
            .containsExactly(createdUser.getId());
        assertThat(userService.getUsersByExternalId("00U-JOHN")).isEmpty();
    }

    @Test
    void testCreateUserStoresTheUserNameLowercasedSoTheLowercasedLoginFindsIt() {
        User createdUser = scimUserFacade.createUser(
            new ScimUser(null, null, null, "John.Doe@X.com", null, null, null, true, null, null));

        assertThat(userService.fetchUserByLogin("john.doe@x.com"))
            .map(User::getId)
            .contains(createdUser.getId());
    }

    @Test
    void testCreateUserWithActiveFalseStoresADisabledUserThatIsStillListed() {
        long userCount = userService.countUsers();

        User createdUser = scimUserFacade.createUser(createScimUser("john.doe", false));

        User storedUser = fetchStoredUser(createdUser.getId());

        assertThat(storedUser.isActivated()).isFalse();
        assertThat(storedUser.getActivationKey()).isNull();

        Page<User> userPage = userService.getAllManagedUsers(PageRequest.of(0, 100));

        assertThat(userPage.getContent())
            .extracting(User::getId)
            .contains(createdUser.getId());
        assertThat(userService.countUsers()).isEqualTo(userCount + 1);
    }

    @Test
    void testCreateUserWithActiveTrueStoresAnActivatedUser() {
        User createdUser = scimUserFacade.createUser(createScimUser("john.doe", true));

        assertThat(fetchStoredUser(createdUser.getId()).isActivated()).isTrue();
    }

    @Test
    void testCreateUserUsesTheFirstEmailWhenNoneIsPrimary() {
        ScimUser scimUser = new ScimUser(
            null, null, null, "john.doe", null, null,
            List.of(new ScimEmail("first@example.com", "work", false),
                new ScimEmail("second@example.com", "home", false)),
            true, null, null);

        User createdUser = scimUserFacade.createUser(scimUser);

        assertThat(fetchStoredUser(createdUser.getId()).getEmail()).isEqualTo("first@example.com");
    }

    @Test
    void testCreateUserFallsBackToTheUserNameWhenEmailsAreMissing() {
        User firstUser = scimUserFacade.createUser(
            new ScimUser(null, null, null, "john@example.com", null, null, null, true, null, null));
        User secondUser = scimUserFacade.createUser(
            new ScimUser(null, null, null, "jane@example.com", null, null, List.of(), true, null, null));

        assertThat(fetchStoredUser(firstUser.getId()).getEmail()).isEqualTo("john@example.com");
        assertThat(fetchStoredUser(secondUser.getId()).getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void testCreateUserWithoutNameLeavesTheNamesUnset() {
        User createdUser = scimUserFacade.createUser(createScimUser("john.doe", true));

        User storedUser = fetchStoredUser(createdUser.getId());

        assertThat(storedUser.getFirstName()).isNull();
        assertThat(storedUser.getLastName()).isNull();
    }

    @Test
    void testCreateUserStoresAnExternalIdOf255CharactersCountedInCodePoints() {
        String externalId = "😀".repeat(255);

        User createdUser = scimUserFacade.createUser(createScimUser("john.doe", externalId, true));

        assertThat(fetchStoredUser(createdUser.getId()).getExternalId()).isEqualTo(externalId);
    }

    @ParameterizedTest
    @MethodSource("unstorableScimUsers")
    void testCreateUserRejectsValuesTheColumnsCannotStoreWithInvalidValue(ScimUser scimUser) {
        long userCount = userService.countUsers();

        assertScimBadRequest(() -> scimUserFacade.createUser(scimUser), ScimBadRequestException.INVALID_VALUE);

        assertThat(userService.countUsers()).isEqualTo(userCount);
    }

    @Test
    void testReplaceUserStoresTheScimAttributes() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        ScimUser scimUser = new ScimUser(
            null, null, "ext-2", "New.Login", new ScimName(null, "Smith", "Jane"), null,
            List.of(new ScimEmail("jane@example.com", "work", true)), true, null, null);

        Optional<User> replacedUser = scimUserFacade.replaceUser(existingUser.getId(), scimUser);

        assertThat(replacedUser)
            .map(User::getId)
            .contains(existingUser.getId());

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getLogin()).isEqualTo("new.login");
        assertThat(storedUser.getEmail()).isEqualTo("jane@example.com");
        assertThat(storedUser.getFirstName()).isEqualTo("Jane");
        assertThat(storedUser.getLastName()).isEqualTo("Smith");
        assertThat(storedUser.getExternalId()).isEqualTo("ext-2");
        assertThat(storedUser.isActivated()).isTrue();
    }

    @Test
    void testReplaceUserKeepsTheLangKeyAndImageUrlTheScimResourceDoesNotManage() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.replaceUser(existingUser.getId(), createScimUser("john.doe", true));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getImageUrl()).isEqualTo("https://example.com/john.png");
        assertThat(storedUser.getLangKey()).isEqualTo("de");
    }

    @Test
    void testReplaceUserDefaultsAMissingLangKey() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        existingUser.setLangKey(null);

        userService.save(existingUser);

        scimUserFacade.replaceUser(existingUser.getId(), createScimUser("john.doe", true));

        assertThat(fetchStoredUser(existingUser.getId()).getLangKey()).isEqualTo(UserConstants.DEFAULT_LANGUAGE);
    }

    @Test
    void testReplaceUserReturnsEmptyWhenTheUserDoesNotExist() {
        long userCount = userService.countUsers();

        assertThat(scimUserFacade.replaceUser(MISSING_USER_ID, createScimUser("john.doe", true))).isEmpty();
        assertThat(scimUserFacade.deactivateUser(MISSING_USER_ID, createScimUser("john.doe", false))).isEmpty();
        assertThat(userService.countUsers()).isEqualTo(userCount);
    }

    @Test
    void testReplaceUserRejectsAnExternalIdLongerThan255CharactersAndKeepsTheStoredUser() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        ScimUser scimUser = createScimUser("jane.doe", "x".repeat(256), true);

        assertScimBadRequest(
            () -> scimUserFacade.replaceUser(existingUser.getId(), scimUser), ScimBadRequestException.INVALID_VALUE);
        assertScimBadRequest(
            () -> scimUserFacade.deactivateUser(existingUser.getId(), scimUser),
            ScimBadRequestException.INVALID_VALUE);

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getLogin()).isEqualTo("john.doe");
        assertThat(storedUser.getExternalId()).isNull();
    }

    @Test
    void testDeactivateUserWithScimUserDisablesTheUserAndKeepsTheLangKeyAndImageUrl() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        Optional<User> deactivatedUser = scimUserFacade.deactivateUser(
            existingUser.getId(), createScimUser("john.doe", false));

        assertThat(deactivatedUser)
            .map(User::isActivated)
            .contains(false);

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.isActivated()).isFalse();
        assertThat(storedUser.getImageUrl()).isEqualTo("https://example.com/john.png");
        assertThat(storedUser.getLangKey()).isEqualTo("de");
    }

    @Test
    void testPatchUserAppliesReplaceOperationsCaseInsensitively() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        Optional<User> patchedUser = scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(
                new ScimPatchOperation("replace", "userName", "jane.smith"),
                new ScimPatchOperation("Replace", "name.givenName", "Jane"),
                new ScimPatchOperation("REPLACE", "name.familyName", "Smith")));

        assertThat(patchedUser).isPresent();

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getLogin()).isEqualTo("jane.smith");
        assertThat(storedUser.getFirstName()).isEqualTo("Jane");
        assertThat(storedUser.getLastName()).isEqualTo("Smith");
        assertThat(storedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(storedUser.isActivated()).isTrue();
    }

    @Test
    void testPatchUserReplacesTheExternalIdSoTheNewValueCanBeFilteredBy() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(
            existingUser.getId(), createPatchRequest(new ScimPatchOperation("replace", "externalId", "old-id")));
        scimUserFacade.patchUser(
            existingUser.getId(), createPatchRequest(new ScimPatchOperation("replace", "externalId", "new-id")));

        assertThat(userService.getUsersByExternalId("old-id")).isEmpty();
        assertThat(userService.getUsersByExternalId("new-id"))
            .extracting(User::getId)
            .containsExactly(existingUser.getId());
        assertThat(fetchStoredUser(existingUser.getId()).getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void testPatchUserAppliesEveryAttributeOfAPathlessReplaceValue() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(
                new ScimPatchOperation(
                    "replace", null, Map.of("externalId", "new-external-id", "name.familyName", "Smith"))));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getExternalId()).isEqualTo("new-external-id");
        assertThat(storedUser.getLastName()).isEqualTo("Smith");
        assertThat(storedUser.isActivated()).isTrue();
    }

    @Test
    void testPatchUserStoresTheUserNameLowercasedSoTheLowercasedLoginFindsIt() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(new ScimPatchOperation("replace", "userName", "Jane.Smith@Example.com")));

        assertThat(userService.fetchUserByLogin("jane.smith@example.com"))
            .map(User::getId)
            .contains(existingUser.getId());
    }

    @Test
    void testPatchUserEvictsTheCachedLoginOfTheRenamedUser() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        assertThat(userService.fetchUserByLogin("john.doe")).isPresent();
        assertThat(userService.fetchUserByLogin("jane.doe")).isEmpty();

        scimUserFacade.patchUser(
            existingUser.getId(), createPatchRequest(new ScimPatchOperation("replace", "userName", "jane.doe")));

        assertThat(userService.fetchUserByLogin("john.doe")).isEmpty();
        assertThat(userService.fetchUserByLogin("jane.doe"))
            .map(User::getId)
            .contains(existingUser.getId());
    }

    @Test
    void testPatchUserIgnoresAttributesTheUserResourceDoesNotMap() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(new ScimPatchOperation("replace", "externalId", "ext-1")));

        scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(
                new ScimPatchOperation("replace", "displayName", "Ignored"),
                new ScimPatchOperation("add", "title", "Ignored"),
                new ScimPatchOperation("Add", "emails[type eq \"work\"].value", "ignored@example.com"),
                new ScimPatchOperation("remove", "phoneNumbers", null),
                new ScimPatchOperation("replace", null, Map.of("displayName", "Ignored")),
                new ScimPatchOperation("add", null, Map.of("preferredLanguage", "fr"))));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getLogin()).isEqualTo("john.doe");
        assertThat(storedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(storedUser.getExternalId()).isEqualTo("ext-1");
        assertThat(storedUser.getFirstName()).isEqualTo("John");
        assertThat(storedUser.getLastName()).isEqualTo("Doe");
        assertThat(storedUser.getLangKey()).isEqualTo("de");
        assertThat(storedUser.isActivated()).isTrue();
    }

    @Test
    void testPatchUserAppliesAddOperationsWithAPathCaseInsensitively() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(
                new ScimPatchOperation("Add", "externalId", "new-external-id"),
                new ScimPatchOperation("add", "userName", "Jane.Smith"),
                new ScimPatchOperation("ADD", "name.givenName", "Jane"),
                new ScimPatchOperation("Add", "name.familyName", "Smith")));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getExternalId()).isEqualTo("new-external-id");
        assertThat(storedUser.getLogin()).isEqualTo("jane.smith");
        assertThat(storedUser.getFirstName()).isEqualTo("Jane");
        assertThat(storedUser.getLastName()).isEqualTo("Smith");
        assertThat(storedUser.isActivated()).isTrue();
    }

    @Test
    void testPatchUserAppliesEveryAttributeOfAPathlessAddValue() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        Map<String, Object> value = new LinkedHashMap<>();

        value.put("externalId", "new-external-id");
        value.put("name.familyName", "Smith");
        value.put("name", Map.of("givenName", "Jane"));
        value.put("active", "False");

        scimUserFacade.patchUser(existingUser.getId(), createPatchRequest(new ScimPatchOperation("Add", null, value)));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getExternalId()).isEqualTo("new-external-id");
        assertThat(storedUser.getFirstName()).isEqualTo("Jane");
        assertThat(storedUser.getLastName()).isEqualTo("Smith");
        assertThat(storedUser.isActivated()).isFalse();
    }

    @Test
    void testPatchUserAcceptsCaseInsensitiveAndSchemaQualifiedPaths() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(
                new ScimPatchOperation("replace", "urn:ietf:params:scim:schemas:core:2.0:User:externalId", "ext-9"),
                new ScimPatchOperation("replace", "NAME.GIVENNAME", "Jane"),
                new ScimPatchOperation("add", "name", Map.of("familyName", "Smith"))));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getExternalId()).isEqualTo("ext-9");
        assertThat(storedUser.getFirstName()).isEqualTo("Jane");
        assertThat(storedUser.getLastName()).isEqualTo("Smith");
    }

    @Test
    void testPatchUserRemoveUnassignsOptionalAttributes() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(
            existingUser.getId(), createPatchRequest(new ScimPatchOperation("replace", "externalId", "ext-1")));

        scimUserFacade.patchUser(
            existingUser.getId(),
            createPatchRequest(
                new ScimPatchOperation("Remove", "externalId", null),
                new ScimPatchOperation("remove", "name.givenName", null)));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getExternalId()).isNull();
        assertThat(storedUser.getFirstName()).isNull();
        assertThat(storedUser.getLastName()).isEqualTo("Doe");
        assertThat(storedUser.getLogin()).isEqualTo("john.doe");
        assertThat(storedUser.isActivated()).isTrue();
        assertThat(userService.getUsersByExternalId("ext-1")).isEmpty();
    }

    @Test
    void testPatchUserRemoveOfNameUnassignsTheGivenAndFamilyName() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        scimUserFacade.patchUser(existingUser.getId(),
            createPatchRequest(new ScimPatchOperation("remove", "name", null)));

        User storedUser = fetchStoredUser(existingUser.getId());

        assertThat(storedUser.getFirstName()).isNull();
        assertThat(storedUser.getLastName()).isNull();
    }

    @Test
    void testPatchUserStoresAnExternalIdOf255Characters() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        String externalId = "x".repeat(255);

        scimUserFacade.patchUser(
            existingUser.getId(), createPatchRequest(new ScimPatchOperation("add", "externalId", externalId)));

        assertThat(fetchStoredUser(existingUser.getId()).getExternalId()).isEqualTo(externalId);
    }

    @Test
    void testPatchUserRejectsRemoveOfUserNameOrActiveWithMutability() {
        assertPatchRejected(ScimBadRequestException.MUTABILITY, new ScimPatchOperation("remove", "userName", null));
        assertPatchRejected(ScimBadRequestException.MUTABILITY, new ScimPatchOperation("Remove", "active", null));
    }

    @Test
    void testPatchUserRejectsRemoveWithoutPathWithNoTarget() {
        assertPatchRejected(
            ScimBadRequestException.NO_TARGET, new ScimPatchOperation("remove", null, Map.of("externalId", "x")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "move", "copy", "test", ""
    })
    void testPatchUserRejectsUnknownOperationsWithInvalidSyntax(String operation) {
        assertPatchRejected(
            ScimBadRequestException.INVALID_SYNTAX, new ScimPatchOperation(operation, "externalId", "x"));
    }

    @Test
    void testPatchUserRejectsMalformedRequestsWithInvalidSyntax() {
        assertPatchRejected(ScimBadRequestException.INVALID_SYNTAX, new ScimPatchOperation(null, "externalId", "x"));
        assertPatchRejected(ScimBadRequestException.INVALID_SYNTAX, new ScimPatchOperation("add", null, "x"));
        assertPatchRejected(ScimBadRequestException.INVALID_SYNTAX);

        User existingUser = createStoredUser("jane.doe", "jane@example.com");

        assertScimBadRequest(
            () -> scimUserFacade.patchUser(existingUser.getId(), new ScimPatchRequest(null, null)),
            ScimBadRequestException.INVALID_SYNTAX);
    }

    @Test
    void testPatchUserRejectsAnExternalIdLongerThan255CharactersWithInvalidValue() {
        String externalId = "x".repeat(256);

        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("Add", "externalId", externalId));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("replace", "externalId", externalId));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("add", null, Map.of("externalId", externalId)));
    }

    @Test
    void testPatchUserRejectsValuesLongerThanTheirColumnsWithInvalidValue() {
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "userName", "u".repeat(51)));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "name.givenName", "g".repeat(51)));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("replace", "name.familyName", "f".repeat(51)));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("replace", "name", Map.of("givenName", "g".repeat(51))));
    }

    @Test
    void testPatchUserRejectsAMissingUserNameAndANonBooleanActiveWithInvalidValue() {
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("replace", "userName", " "));
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "userName", null));
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("replace", "active", "yes"));
        assertPatchRejected(ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "active", null));
    }

    @Test
    void testPatchUserRejectsMultipleValuesForASingleValuedAttributeWithInvalidValue() {
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "externalId", List.of("a", "b")));
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE, new ScimPatchOperation("add", "externalId", Map.of("a", "b")));
    }

    @Test
    void testPatchUserValidatesEveryOperationBeforeApplyingAny() {
        assertPatchRejected(
            ScimBadRequestException.INVALID_VALUE,
            new ScimPatchOperation("replace", "name.givenName", "Jane"),
            new ScimPatchOperation("add", "externalId", "x".repeat(256)));
    }

    @Test
    void testPatchUserReturnsEmptyWhenTheUserDoesNotExist() {
        long userCount = userService.countUsers();

        assertThat(
            scimUserFacade.patchUser(
                MISSING_USER_ID, createPatchRequest(new ScimPatchOperation("replace", "userName", "x"))))
                    .isEmpty();
        assertThat(userService.countUsers()).isEqualTo(userCount);
    }

    @Test
    void testDeactivateUserWithPatchDisablesTheStoredUser() {
        User firstUser = createStoredUser("john.doe", "john@example.com");
        User secondUser = createStoredUser("jane.doe", "jane@example.com");

        scimUserFacade.deactivateUser(
            firstUser.getId(), createPatchRequest(new ScimPatchOperation("replace", "active", "False")));
        scimUserFacade.deactivateUser(
            secondUser.getId(), createPatchRequest(new ScimPatchOperation("replace", null, Map.of("active", false))));

        assertThat(fetchStoredUser(firstUser.getId()).isActivated()).isFalse();
        assertThat(fetchStoredUser(secondUser.getId()).isActivated()).isFalse();
    }

    @Test
    void testPatchUserReactivatesADisabledUser() {
        User createdUser = scimUserFacade.createUser(createScimUser("john.doe", false));

        scimUserFacade.patchUser(
            createdUser.getId(), createPatchRequest(new ScimPatchOperation("replace", "active", true)));

        assertThat(fetchStoredUser(createdUser.getId()).isActivated()).isTrue();
    }

    @Test
    void testDeleteUserDeletesTheStoredUser() {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        assertThat(scimUserFacade.deleteUser(existingUser.getId())).isTrue();
        assertThat(userService.fetchUser(existingUser.getId())).isEmpty();
    }

    @Test
    void testDeleteUserReturnsFalseWhenTheUserDoesNotExist() {
        long userCount = userService.countUsers();

        assertThat(scimUserFacade.deleteUser(MISSING_USER_ID)).isFalse();
        assertThat(userService.countUsers()).isEqualTo(userCount);
    }

    private void assertPatchRejected(String scimType, ScimPatchOperation... scimPatchOperations) {
        User existingUser = createStoredUser("john.doe", "john@example.com");

        long userId = existingUser.getId();
        ScimPatchRequest scimPatchRequest = createPatchRequest(scimPatchOperations);

        assertScimBadRequest(() -> scimUserFacade.patchUser(userId, scimPatchRequest), scimType);
        assertScimBadRequest(() -> scimUserFacade.deactivateUser(userId, scimPatchRequest), scimType);
        assertScimBadRequest(() -> ScimUserFacade.validate(scimPatchRequest), scimType);

        User storedUser = fetchStoredUser(userId);

        assertThat(storedUser.getExternalId()).isNull();
        assertThat(storedUser.getFirstName()).isEqualTo("John");
        assertThat(storedUser.getLastName()).isEqualTo("Doe");
        assertThat(storedUser.getLogin()).isEqualTo("john.doe");
        assertThat(storedUser.isActivated()).isTrue();

        userService.delete(storedUser.getLogin());
    }

    private static void assertScimBadRequest(ThrowingCallable throwingCallable, String scimType) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(ScimBadRequestException.class)
            .hasFieldOrPropertyWithValue("scimType", scimType);
    }

    private User createStoredUser(String login, String email) {
        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setEmail(email);
        adminUserDTO.setFirstName("John");
        adminUserDTO.setImageUrl("https://example.com/john.png");
        adminUserDTO.setLangKey("de");
        adminUserDTO.setLastName("Doe");
        adminUserDTO.setLogin(login);

        return userService.create(adminUserDTO);
    }

    private User fetchStoredUser(long id) {
        return userService.fetchUser(id)
            .orElseThrow();
    }

    private static Stream<ScimUser> unstorableScimUsers() {
        List<ScimEmail> emails = List.of(new ScimEmail("john@example.com", "work", true));

        return Stream.of(
            new ScimUser(null, null, "x".repeat(256), "john.doe", null, null, emails, true, null, null),
            new ScimUser(null, null, null, "u".repeat(51), null, null, emails, true, null, null),
            new ScimUser(
                null, null, null, "john.doe", null, null,
                List.of(new ScimEmail("e".repeat(180) + "@example.com", "work", true)), true, null, null),
            new ScimUser(
                null, null, null, "john.doe", new ScimName(null, null, "g".repeat(51)), null, emails, true, null,
                null),
            new ScimUser(
                null, null, null, "john.doe", new ScimName(null, "f".repeat(51), null), null, emails, true, null,
                null),
            new ScimUser(null, null, null, null, null, null, emails, true, null, null));
    }

    private static ScimPatchRequest createPatchRequest(ScimPatchOperation... scimPatchOperations) {
        return new ScimPatchRequest(
            List.of("urn:ietf:params:scim:api:messages:2.0:PatchOp"), Arrays.asList(scimPatchOperations));
    }

    private static ScimUser createScimUser(String userName, boolean active) {
        return createScimUser(userName, null, active);
    }

    private static ScimUser createScimUser(String userName, String externalId, boolean active) {
        return new ScimUser(
            List.of(ScimUser.SCHEMA), null, externalId, userName, null, null,
            List.of(new ScimEmail(userName.toLowerCase(Locale.ROOT) + "@example.com", "work", true)), active, null,
            null);
    }

    private static User createUser(boolean activated) {
        User user = new User();

        user.setActivated(activated);
        user.setLogin("john.doe");

        return user;
    }
}
