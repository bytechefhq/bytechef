/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditMapper;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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
public class ScimUserFacade {

    private static final String ACTIVE = "active";
    private static final String REPLACE = "replace";

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ScimUserFacade(UserService userService) {
        this.userService = userService;
    }

    public static boolean isDeactivation(User user, ScimUser scimUser) {
        return user.isActivated() && !scimUser.active();
    }

    public static boolean isDeactivation(User user, ScimPatchRequest scimPatchRequest) {
        if (!user.isActivated()) {
            return false;
        }

        Boolean active = null;

        for (ScimPatchOperation scimPatchOperation : scimPatchRequest.operations()) {
            if (!REPLACE.equalsIgnoreCase(scimPatchOperation.operation())) {
                continue;
            }

            if (ACTIVE.equals(scimPatchOperation.path())) {
                active = Boolean.parseBoolean(String.valueOf(scimPatchOperation.value()));
            } else if (scimPatchOperation.path() == null && scimPatchOperation.value() instanceof Map<?, ?> valueMap
                && valueMap.containsKey(ACTIVE)) {

                active = Boolean.parseBoolean(String.valueOf(valueMap.get(ACTIVE)));
            }
        }

        return Boolean.FALSE.equals(active);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_CREATED, mapper = ScimUserAuditMapper.class)
    public User createUser(ScimUser scimUser) {
        AdminUserDTO adminUserDTO = new AdminUserDTO();

        applyScimUser(adminUserDTO, scimUser);

        return userService.create(adminUserDTO);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_DEACTIVATED, mapper = ScimUserAuditMapper.class)
    public Optional<User> deactivateUser(long id, ScimPatchRequest scimPatchRequest) {
        return applyPatch(id, scimPatchRequest);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_DEACTIVATED, mapper = ScimUserAuditMapper.class)
    public Optional<User> deactivateUser(long id, ScimUser scimUser) {
        return applyReplace(id, scimUser);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_DELETED, mapper = ScimUserAuditMapper.class)
    public boolean deleteUser(long id) {
        Optional<User> userOptional = userService.fetchUser(id);

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        userService.delete(user.getLogin());

        return true;
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_PATCHED, mapper = ScimUserAuditMapper.class)
    public Optional<User> patchUser(long id, ScimPatchRequest scimPatchRequest) {
        return applyPatch(id, scimPatchRequest);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_UPDATED, mapper = ScimUserAuditMapper.class)
    public Optional<User> replaceUser(long id, ScimUser scimUser) {
        return applyReplace(id, scimUser);
    }

    private Optional<User> applyPatch(long id, ScimPatchRequest scimPatchRequest) {
        Optional<User> userOptional = userService.fetchUser(id);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        for (ScimPatchOperation scimPatchOperation : scimPatchRequest.operations()) {
            if (REPLACE.equalsIgnoreCase(scimPatchOperation.operation())) {
                applyReplaceOperation(user, scimPatchOperation);
            }
        }

        userService.save(user);

        return Optional.of(user);
    }

    private Optional<User> applyReplace(long id, ScimUser scimUser) {
        if (userService.fetchUser(id)
            .isEmpty()) {

            return Optional.empty();
        }

        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setId(id);

        applyScimUser(adminUserDTO, scimUser);

        return userService.update(adminUserDTO);
    }

    private static void applyReplaceOperation(User user, ScimPatchOperation scimPatchOperation) {
        String path = scimPatchOperation.path();
        Object value = scimPatchOperation.value();

        if (path != null) {
            switch (path) {
                case ACTIVE -> user.setActivated(Boolean.parseBoolean(String.valueOf(value)));
                case "userName" -> user.setLogin(String.valueOf(value));
                case "name.givenName" -> user.setFirstName(String.valueOf(value));
                case "name.familyName" -> user.setLastName(String.valueOf(value));
                default -> {
                }
            }
        } else if (value instanceof Map<?, ?> valueMap && valueMap.containsKey(ACTIVE)) {
            user.setActivated(Boolean.parseBoolean(String.valueOf(valueMap.get(ACTIVE))));
        }
    }

    private static void applyScimUser(AdminUserDTO adminUserDTO, ScimUser scimUser) {
        adminUserDTO.setLogin(scimUser.userName());
        adminUserDTO.setEmail(extractPrimaryEmail(scimUser));
        adminUserDTO.setActivated(scimUser.active());

        ScimName scimName = scimUser.name();

        if (scimName != null) {
            adminUserDTO.setFirstName(scimName.givenName());
            adminUserDTO.setLastName(scimName.familyName());
        }
    }

    private static String extractPrimaryEmail(ScimUser scimUser) {
        List<ScimEmail> scimEmails = scimUser.emails();

        if (scimEmails == null || scimEmails.isEmpty()) {
            return scimUser.userName();
        }

        for (ScimEmail scimEmail : scimEmails) {
            if (scimEmail.primary()) {
                return scimEmail.value();
            }
        }

        ScimEmail scimEmail = scimEmails.getFirst();

        return scimEmail.value();
    }
}
