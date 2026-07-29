/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.facade;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class SigningKeyFacadeImpl implements SigningKeyFacade {

    private final SigningKeyService signingKeyService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SigningKeyFacadeImpl(SigningKeyService signingKeyService, UserService userService) {
        this.signingKeyService = signingKeyService;
        this.userService = userService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public String create(SigningKey signingKey, PlatformType type) {
        User user = userService.getCurrentUser();

        signingKey.setType(type);
        signingKey.setUserId(user.getId());

        return signingKeyService.create(signingKey);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<SigningKey> getSigningKeys(PlatformType type, long environmentId) {
        return signingKeyService.getSigningKeys(type, environmentId);
    }
}
