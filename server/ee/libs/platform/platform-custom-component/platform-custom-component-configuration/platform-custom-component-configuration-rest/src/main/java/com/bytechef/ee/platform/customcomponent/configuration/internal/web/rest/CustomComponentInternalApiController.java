/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.internal.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Session/cookie-reachable counterpart to {@code CustomComponentApiController}. The admin console's upload dialog posts
 * cookie+XSRF only (no {@code Authorization} header), but {@code /api/platform/v1/custom-components/deploy} is matched
 * unconditionally by {@code PlatformApiKeySecurityConfigurer}, whose converter throws {@code BadCredentialsException}
 * when no {@code Authorization} header is present -- a hard 401 before the controller ever runs. This endpoint is
 * mounted under {@code /api/platform/internal/**} instead, which {@code PlatformApiKeySecurityConfigurer} does not
 * match, so the request falls through to normal session-based Spring Security authentication (the same base
 * {@code /api/**} filter chain every other authenticated browser call uses). The bearer-token surface at
 * {@code /api/platform/v1/custom-components/deploy} is left untouched for CLI/curl callers.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.platform.customcomponent.configuration.internal.web.rest.CustomComponentInternalApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class CustomComponentInternalApiController implements CustomComponentInternalApi {

    private final CustomComponentFacade customComponentFacade;

    @SuppressFBWarnings("EI")
    public CustomComponentInternalApiController(CustomComponentFacade customComponentFacade) {
        this.customComponentFacade = customComponentFacade;
    }

    /**
     * Authorization note: the {@code ROLE_ADMIN} guard lives on {@code CustomComponentFacadeImpl#save}, exactly
     * mirroring {@code CustomComponentApiController}, so it protects every caller of the facade -- session-based
     * admin-console callers included -- regardless of which REST entry point they reach it through.
     */
    @Override
    public ResponseEntity<Void> deployCustomComponentInternal(MultipartFile componentFile) {
        try {
            customComponentFacade.save(
                componentFile.getBytes(), Language.of(Objects.requireNonNull(componentFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.noContent()
            .build();
    }
}
