/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser;
import com.bytechef.ee.platform.user.scim.filter.ScimFilterParser.ScimFilter;
import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.ee.platform.user.scim.model.ScimListResponse;
import com.bytechef.ee.platform.user.scim.model.ScimMeta;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCIM 2.0 Group resource endpoints (RFC 7644 Section 3).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(path = "/api/scim/v2/Groups", produces = "application/scim+json")
class ScimGroupController {

    private final AuthorityService authorityService;

    @SuppressFBWarnings("EI")
    ScimGroupController(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @GetMapping
    ResponseEntity<ScimListResponse<ScimGroup>> listGroups(
        @RequestParam(defaultValue = "1") int startIndex,
        @RequestParam(defaultValue = "100") int count,
        @RequestParam(required = false) String filter) {

        if (filter != null) {
            ScimFilter scimFilter = ScimFilterParser.parse(filter);

            if (scimFilter != null && "displayName".equalsIgnoreCase(scimFilter.attributeName())) {
                List<Authority> authorities = authorityService.getAuthorities();

                List<ScimGroup> matchedGroups = authorities
                    .stream()
                    .filter(authority -> scimFilter.value()
                        .equalsIgnoreCase(authority.getName()))
                    .map(this::toScimGroup)
                    .toList();

                return ResponseEntity.ok(ScimListResponse.of(matchedGroups, startIndex));
            }
        }

        List<Authority> authorities = authorityService.getAuthorities();

        List<ScimGroup> groups = authorities
            .stream()
            .map(this::toScimGroup)
            .toList();

        return ResponseEntity.ok(ScimListResponse.of(groups, startIndex));
    }

    @GetMapping("/{id}")
    ResponseEntity<Object> getGroup(@PathVariable long id) {
        Optional<Authority> authority = authorityService.fetchAuthority(id);

        if (authority.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(notFoundError("Group", String.valueOf(id)));
        }

        return ResponseEntity.ok(toScimGroup(authority.get()));
    }

    @PostMapping
    ResponseEntity<ScimGroup> createGroup(@RequestBody ScimGroup scimGroup) {
        Authority authority = new Authority();

        authority.setName(scimGroup.displayName());

        Authority createdAuthority = authorityService.create(authority);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toScimGroup(createdAuthority));
    }

    @PutMapping("/{id}")
    ResponseEntity<Object> replaceGroup(@PathVariable long id, @RequestBody ScimGroup scimGroup) {
        Optional<Authority> existingAuthority = authorityService.fetchAuthority(id);

        if (existingAuthority.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(notFoundError("Group", String.valueOf(id)));
        }

        Authority authority = existingAuthority.get();

        authority.setName(scimGroup.displayName());

        Authority updatedAuthority = authorityService.update(authority);

        return ResponseEntity.ok(toScimGroup(updatedAuthority));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @PatchMapping("/{id}")
    ResponseEntity<Object> patchGroup(@PathVariable long id, @RequestBody ScimPatchRequest patchRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(
                Map.of(
                    "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:Error"),
                    "detail", "PATCH operation is not supported for Group resources",
                    "status", "501"));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteGroup(@PathVariable long id) {
        Optional<Authority> authority = authorityService.fetchAuthority(id);

        if (authority.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        authorityService.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    private Map<String, Object> notFoundError(String resourceType, String id) {
        return Map.of(
            "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:Error"),
            "detail", resourceType + " " + id + " not found",
            "status", "404");
    }

    private ScimGroup toScimGroup(Authority authority) {
        ScimMeta meta = new ScimMeta(
            "Group", null, null, "/api/scim/v2/Groups/" + authority.getId());

        return new ScimGroup(
            List.of(ScimGroup.SCHEMA), String.valueOf(authority.getId()), authority.getName(), List.of(), meta);
    }
}
