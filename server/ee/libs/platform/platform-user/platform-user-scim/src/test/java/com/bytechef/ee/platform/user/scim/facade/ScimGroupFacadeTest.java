/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 */
class ScimGroupFacadeTest {

    private static final long GROUP_ID = 7L;

    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final ScimGroupFacade scimGroupFacade = new ScimGroupFacade(authorityService);

    @Test
    void testCreateGroupCreatesAuthorityWithDisplayName() {
        Authority createdAuthority = createAuthority(GROUP_ID, "ROLE_ENGINEERING");

        when(authorityService.create(any(Authority.class))).thenReturn(createdAuthority);

        Authority authority = scimGroupFacade.createGroup(createScimGroup("ROLE_ENGINEERING"));

        ArgumentCaptor<Authority> authorityArgumentCaptor = ArgumentCaptor.forClass(Authority.class);

        verify(authorityService).create(authorityArgumentCaptor.capture());

        Authority capturedAuthority = authorityArgumentCaptor.getValue();

        assertThat(authority).isSameAs(createdAuthority);
        assertThat(capturedAuthority.getId()).isNull();
        assertThat(capturedAuthority.getName()).isEqualTo("ROLE_ENGINEERING");
    }

    @Test
    void testDeleteGroupDeletesExistingAuthority() {
        when(authorityService.fetchAuthority(GROUP_ID))
            .thenReturn(Optional.of(createAuthority(GROUP_ID, "ROLE_ENGINEERING")));

        assertThat(scimGroupFacade.deleteGroup(GROUP_ID)).isTrue();

        verify(authorityService).delete(GROUP_ID);
    }

    @Test
    void testDeleteGroupReturnsFalseWhenAuthorityDoesNotExist() {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.empty());

        assertThat(scimGroupFacade.deleteGroup(GROUP_ID)).isFalse();

        verify(authorityService, never()).delete(anyLong());
    }

    @Test
    void testReplaceGroupRenamesExistingAuthority() {
        Authority existingAuthority = createAuthority(GROUP_ID, "ROLE_OLD");
        Authority updatedAuthority = createAuthority(GROUP_ID, "ROLE_NEW");

        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.of(existingAuthority));
        when(authorityService.update(existingAuthority)).thenReturn(updatedAuthority);

        Optional<Authority> authorityOptional = scimGroupFacade.replaceGroup(GROUP_ID, createScimGroup("ROLE_NEW"));

        assertThat(authorityOptional).containsSame(updatedAuthority);
        assertThat(existingAuthority.getName()).isEqualTo("ROLE_NEW");
    }

    @Test
    void testReplaceGroupReturnsEmptyWhenAuthorityDoesNotExist() {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.empty());

        assertThat(scimGroupFacade.replaceGroup(GROUP_ID, createScimGroup("ROLE_NEW"))).isEmpty();

        verify(authorityService, never()).update(any(Authority.class));
    }

    private static Authority createAuthority(long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }

    private static ScimGroup createScimGroup(String displayName) {
        return new ScimGroup(List.of(ScimGroup.SCHEMA), null, displayName, List.of(), null);
    }
}
