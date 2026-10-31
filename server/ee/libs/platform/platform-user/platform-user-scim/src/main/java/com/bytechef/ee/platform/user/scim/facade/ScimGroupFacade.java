/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import com.bytechef.ee.platform.user.scim.audit.ScimGroupAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimGroupAuditMapper;
import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Transactional
public class ScimGroupFacade {

    private final AuthorityService authorityService;

    @SuppressFBWarnings("EI")
    public ScimGroupFacade(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Audited(event = ScimGroupAuditEvents.SCIM_GROUP_CREATED, mapper = ScimGroupAuditMapper.class)
    public Authority createGroup(ScimGroup scimGroup) {
        Authority authority = new Authority();

        authority.setName(scimGroup.displayName());

        return authorityService.create(authority);
    }

    @Audited(event = ScimGroupAuditEvents.SCIM_GROUP_DELETED, mapper = ScimGroupAuditMapper.class)
    public boolean deleteGroup(long id) {
        if (authorityService.fetchAuthority(id)
            .isEmpty()) {

            return false;
        }

        authorityService.delete(id);

        return true;
    }

    @Audited(event = ScimGroupAuditEvents.SCIM_GROUP_UPDATED, mapper = ScimGroupAuditMapper.class)
    public Optional<Authority> replaceGroup(long id, ScimGroup scimGroup) {
        Optional<Authority> authorityOptional = authorityService.fetchAuthority(id);

        if (authorityOptional.isEmpty()) {
            return Optional.empty();
        }

        Authority authority = authorityOptional.get();

        authority.setName(scimGroup.displayName());

        return Optional.of(authorityService.update(authority));
    }
}
