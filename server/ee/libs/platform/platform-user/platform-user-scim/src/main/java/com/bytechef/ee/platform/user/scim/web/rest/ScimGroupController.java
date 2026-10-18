/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.ScimConstants;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser.ScimFilter;
import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.ee.platform.user.scim.model.ScimListResponse;
import com.bytechef.ee.platform.user.scim.model.ScimMeta;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only SCIM 2.0 Groups endpoint exposing ByteChef's authorities. Groups are system roles here, not
 * identity-provider groups, so an identity provider may read them but not create, rename, delete or re-assign them:
 * write operations answer 501 Not Implemented.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(path = "/api/scim/v2/Groups", produces = "application/scim+json")
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
@PreAuthorize("hasAuthority(\"" + ScimConstants.SCIM_AUTHORITY + "\")")
class ScimGroupController extends AbstractScimController {

    private static final String READ_ONLY_DETAIL = "Groups are read-only: they map to ByteChef roles";

    private final AuthorityService authorityService;

    @SuppressFBWarnings("EI")
    ScimGroupController(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @GetMapping
    ResponseEntity<ScimListResponse<ScimGroup>> listGroups(
        @RequestParam(defaultValue = "1") int startIndex, @RequestParam(required = false) String filter) {

        List<Authority> authorities = authorityService.getAuthorities();

        if (filter != null && !filter.isBlank()) {
            ScimFilter scimFilter = ScimFilterParser.parse(filter);

            if (scimFilter == null || !"displayName".equals(scimFilter.attributeName())) {
                throw ScimException.invalidFilter("Only 'displayName eq \"value\"' filters are supported");
            }

            authorities = authorities.stream()
                .filter(authority -> scimFilter.value()
                    .equalsIgnoreCase(authority.getName()))
                .toList();
        }

        List<ScimGroup> groups = authorities.stream()
            .map(this::toScimGroup)
            .toList();

        return ResponseEntity.ok(ScimListResponse.of(groups, Math.max(1, startIndex)));
    }

    @GetMapping("/{id}")
    ResponseEntity<ScimGroup> getGroup(@PathVariable long id) {
        return authorityService.fetchAuthority(id)
            .map(authority -> ResponseEntity.ok(toScimGroup(authority)))
            .orElseThrow(() -> ScimException.notFound("Group " + id + " not found"));
    }

    @PostMapping
    ResponseEntity<ScimGroup> createGroup() {
        throw ScimException.notImplemented(READ_ONLY_DETAIL);
    }

    @PutMapping("/{id}")
    ResponseEntity<ScimGroup> replaceGroup(@PathVariable long id) {
        throw ScimException.notImplemented(READ_ONLY_DETAIL);
    }

    @PatchMapping("/{id}")
    ResponseEntity<ScimGroup> patchGroup(@PathVariable long id) {
        throw ScimException.notImplemented(READ_ONLY_DETAIL);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteGroup(@PathVariable long id) {
        throw ScimException.notImplemented(READ_ONLY_DETAIL);
    }

    private ScimGroup toScimGroup(Authority authority) {
        ScimMeta meta = new ScimMeta("Group", null, null, "/api/scim/v2/Groups/" + authority.getId());

        return new ScimGroup(
            List.of(ScimGroup.SCHEMA), String.valueOf(authority.getId()), authority.getName(), List.of(), meta);
    }
}
