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

package com.bytechef.platform.user.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.user.facade.OrganizationDeletionFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that lets a tenant administrator request deletion of the whole organization.
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/account/organization")
@ConditionalOnCoordinator
public class OrganizationDeletionController {

    private final OrganizationDeletionFacade organizationDeletionFacade;

    public OrganizationDeletionController(OrganizationDeletionFacade organizationDeletionFacade) {
        this.organizationDeletionFacade = organizationDeletionFacade;
    }

    @PostMapping("/delete")
    public OrganizationDeletionResponse requestDeletion(
        @RequestBody OrganizationDeletionRequest organizationDeletionRequest, HttpServletRequest request,
        HttpServletResponse response) {

        Instant purgeAfter = organizationDeletionFacade.requestDeletion(
            organizationDeletionRequest.confirmation(), organizationDeletionRequest.password());

        new SecurityContextLogoutHandler().logout(request, response, null);
        new CookieClearingLogoutHandler(AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY)
            .logout(request, response, null);

        return new OrganizationDeletionResponse(purgeAfter);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    void handleAccessDenied() {
    }

    public record OrganizationDeletionRequest(String confirmation, String password) {
    }

    public record OrganizationDeletionResponse(Instant purgeAfter) {
    }
}
