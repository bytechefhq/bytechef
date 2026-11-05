/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditMapper;
import com.bytechef.ee.platform.user.scim.facade.ScimUserPatchOperations.Attribute;
import com.bytechef.ee.platform.user.scim.facade.ScimUserPatchOperations.AttributeChange;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
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

    private static final String EMAILS = "emails";
    private static final String EXTERNAL_ID = "externalId";
    private static final String FAMILY_NAME = "name.familyName";
    private static final String GIVEN_NAME = "name.givenName";
    private static final String USER_NAME = "userName";

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

        String active = null;

        for (AttributeChange attributeChange : ScimUserPatchOperations.parse(scimPatchRequest)) {
            if (attributeChange.attribute() == Attribute.ACTIVE) {
                active = attributeChange.value();
            }
        }

        return "false".equals(active);
    }

    public static void validate(ScimUser scimUser) {
        String userName = ScimValues.checkRequired(USER_NAME, scimUser.userName());

        ScimValues.checkMaxLength(USER_NAME, toLogin(userName), UserConstants.LOGIN_MAX_LENGTH);

        String email = extractPrimaryEmail(scimUser);

        ScimValues.checkMaxLength(
            EMAILS, email == null ? null : email.toLowerCase(Locale.ROOT), UserConstants.EMAIL_MAX_LENGTH);
        ScimValues.checkMaxLength(EXTERNAL_ID, scimUser.externalId(), ScimValues.EXTERNAL_ID_MAX_LENGTH);

        ScimName scimName = scimUser.name();

        if (scimName != null) {
            ScimValues.checkMaxLength(GIVEN_NAME, scimName.givenName(), ScimValues.NAME_MAX_LENGTH);
            ScimValues.checkMaxLength(FAMILY_NAME, scimName.familyName(), ScimValues.NAME_MAX_LENGTH);
        }
    }

    public static void validate(ScimPatchRequest scimPatchRequest) {
        ScimUserPatchOperations.parse(scimPatchRequest);
    }

    @Audited(event = ScimUserAuditEvents.SCIM_USER_CREATED, mapper = ScimUserAuditMapper.class)
    public User createUser(ScimUser scimUser) {
        validate(scimUser);

        AdminUserDTO adminUserDTO = new AdminUserDTO();

        applyScimUser(adminUserDTO, scimUser);

        User user = userService.create(adminUserDTO);

        if (!scimUser.active()) {
            user.setActivated(false);

            userService.save(user);
        }

        return user;
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
        List<AttributeChange> attributeChanges = ScimUserPatchOperations.parse(scimPatchRequest);

        Optional<User> userOptional = userService.fetchUser(id);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        for (AttributeChange attributeChange : attributeChanges) {
            applyAttributeChange(user, attributeChange);
        }

        userService.save(user);

        return Optional.of(user);
    }

    private Optional<User> applyReplace(long id, ScimUser scimUser) {
        validate(scimUser);

        Optional<User> userOptional = userService.fetchUser(id);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();
        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setId(id);

        applyScimUser(adminUserDTO, scimUser);

        String langKey = user.getLangKey();

        adminUserDTO.setImageUrl(user.getImageUrl());
        adminUserDTO.setLangKey(langKey == null ? UserConstants.DEFAULT_LANGUAGE : langKey);

        return userService.update(adminUserDTO);
    }

    private static void applyAttributeChange(User user, AttributeChange attributeChange) {
        String value = attributeChange.value();

        switch (attributeChange.attribute()) {
            case ACTIVE -> user.setActivated(Boolean.parseBoolean(value));
            case EXTERNAL_ID -> user.setExternalId(value);
            case FAMILY_NAME -> user.setLastName(value);
            case GIVEN_NAME -> user.setFirstName(value);
            case USER_NAME -> user.setLogin(value);
            default -> throw new IllegalStateException("Unexpected attribute: " + attributeChange.attribute());
        }
    }

    private static void applyScimUser(AdminUserDTO adminUserDTO, ScimUser scimUser) {
        adminUserDTO.setLogin(scimUser.userName());
        adminUserDTO.setEmail(extractPrimaryEmail(scimUser));
        adminUserDTO.setExternalId(scimUser.externalId());
        adminUserDTO.setActivated(scimUser.active());

        ScimName scimName = scimUser.name();

        if (scimName != null) {
            adminUserDTO.setFirstName(scimName.givenName());
            adminUserDTO.setLastName(scimName.familyName());
        }
    }

    private static String toLogin(String userName) {
        return userName.toLowerCase(Locale.ROOT);
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
