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
import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ScimIntTestConfiguration.class)
@Transactional
class ScimGroupFacadeIntTest {

    private static final long MISSING_GROUP_ID = Long.MAX_VALUE;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private ScimGroupFacade scimGroupFacade;

    @Test
    void testCreateGroupStoresAnAuthorityNamedAfterTheDisplayName() {
        Authority authority = scimGroupFacade.createGroup(createScimGroup("ROLE_ENGINEERING"));

        Optional<Authority> storedAuthority = authorityService.fetchAuthority(authority.getId());

        assertThat(storedAuthority)
            .map(Authority::getName)
            .contains("ROLE_ENGINEERING");
    }

    @Test
    void testCreateGroupStoresADisplayNameOf50Characters() {
        String displayName = "G".repeat(50);

        Authority authority = scimGroupFacade.createGroup(createScimGroup(displayName));

        assertThat(authorityService.fetchAuthority(authority.getId()))
            .map(Authority::getName)
            .contains(displayName);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "", " "
    })
    void testCreateGroupRejectsAMissingDisplayNameWithoutStoringAnything(String displayName) {
        List<Authority> authorities = authorityService.getAuthorities();

        assertThatThrownBy(() -> scimGroupFacade.createGroup(createScimGroup(displayName)))
            .isInstanceOf(ScimBadRequestException.class)
            .hasFieldOrPropertyWithValue("scimType", ScimBadRequestException.INVALID_VALUE)
            .hasMessage("displayName is required");

        assertThat(authorityService.getAuthorities()).hasSameSizeAs(authorities);
    }

    @Test
    void testCreateGroupRejectsADisplayNameLongerThan50CharactersWithoutStoringAnything() {
        List<Authority> authorities = authorityService.getAuthorities();

        assertThatThrownBy(() -> scimGroupFacade.createGroup(createScimGroup("G".repeat(51))))
            .isInstanceOf(ScimBadRequestException.class)
            .hasFieldOrPropertyWithValue("scimType", ScimBadRequestException.INVALID_VALUE)
            .hasMessage("displayName must not be longer than 50 characters");

        assertThat(authorityService.getAuthorities()).hasSameSizeAs(authorities);
    }

    @Test
    void testReplaceGroupRenamesTheStoredAuthority() {
        Authority authority = scimGroupFacade.createGroup(createScimGroup("ROLE_OLD"));

        Optional<Authority> replacedAuthority = scimGroupFacade.replaceGroup(
            authority.getId(), createScimGroup("ROLE_NEW"));

        assertThat(replacedAuthority)
            .map(Authority::getName)
            .contains("ROLE_NEW");
        assertThat(authorityService.fetchAuthority(authority.getId()))
            .map(Authority::getName)
            .contains("ROLE_NEW");
    }

    @Test
    void testReplaceGroupReturnsEmptyWhenTheAuthorityDoesNotExist() {
        assertThat(scimGroupFacade.replaceGroup(MISSING_GROUP_ID, createScimGroup("ROLE_NEW"))).isEmpty();
        assertThat(authorityService.fetchAuthority(MISSING_GROUP_ID)).isEmpty();
    }

    @Test
    void testReplaceGroupRejectsADisplayNameLongerThan50CharactersAndKeepsTheStoredName() {
        Authority authority = scimGroupFacade.createGroup(createScimGroup("ROLE_OLD"));

        assertThatThrownBy(() -> scimGroupFacade.replaceGroup(authority.getId(), createScimGroup("G".repeat(51))))
            .isInstanceOf(ScimBadRequestException.class)
            .hasFieldOrPropertyWithValue("scimType", ScimBadRequestException.INVALID_VALUE);

        assertThat(authorityService.fetchAuthority(authority.getId()))
            .map(Authority::getName)
            .contains("ROLE_OLD");
    }

    @Test
    void testDeleteGroupDeletesTheStoredAuthority() {
        Authority authority = scimGroupFacade.createGroup(createScimGroup("ROLE_ENGINEERING"));

        assertThat(scimGroupFacade.deleteGroup(authority.getId())).isTrue();
        assertThat(authorityService.fetchAuthority(authority.getId())).isEmpty();
    }

    @Test
    void testDeleteGroupReturnsFalseWhenTheAuthorityDoesNotExist() {
        List<Authority> authorities = authorityService.getAuthorities();

        assertThat(scimGroupFacade.deleteGroup(MISSING_GROUP_ID)).isFalse();
        assertThat(authorityService.getAuthorities()).hasSameSizeAs(authorities);
    }

    private static ScimGroup createScimGroup(String displayName) {
        return new ScimGroup(List.of(ScimGroup.SCHEMA), null, displayName, List.of(), null);
    }
}
