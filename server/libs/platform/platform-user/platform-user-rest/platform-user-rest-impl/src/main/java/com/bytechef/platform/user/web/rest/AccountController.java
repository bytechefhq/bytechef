/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tenant.TenantContext;
import com.bytechef.platform.tenant.service.TenantService;
import com.bytechef.platform.tenant.util.TenantUtils;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.dto.PasswordChangeDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.InvalidPasswordException;
import com.bytechef.platform.user.exception.LoginAlreadyUsedException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.MailService;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.TempEmailService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.web.rest.exception.AccountErrorType;
import com.bytechef.platform.user.web.rest.exception.AccountResourceException;
import com.bytechef.platform.user.web.rest.vm.KeyAndPasswordVM;
import com.bytechef.platform.user.web.rest.vm.ManagedUserVM;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the current user's account.
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api")
@ConditionalOnCoordinator
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final ApplicationProperties applicationProperties;
    private final AuthorityService authorityService;
    private final MailService mailService;
    private final PersistentTokenService persistentTokenService;
    private final TenantService tenantService;
    private final UserService userService;
    private final TempEmailService tempEmailService;

    @SuppressFBWarnings("EI")
    public AccountController(
        ApplicationProperties applicationProperties, AuthorityService authorityService, MailService mailService,
        PersistentTokenService persistentTokenService, TenantService tenantService, UserService userService,
        TempEmailService tempEmailService) {

        this.applicationProperties = applicationProperties;
        this.authorityService = authorityService;
        this.persistentTokenService = persistentTokenService;
        this.tenantService = tenantService;
        this.userService = userService;
        this.mailService = mailService;
        this.tempEmailService = tempEmailService;
    }

    /**
     * {@code POST  /register} : register the user.
     *
     * @param managedUserVM the managed user View Model.
     * @throws InvalidPasswordException  {@code 400 (Bad Request)} if the password is incorrect.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (tempEmailService.isEmailDomainTemp(managedUserVM.getEmail())) {
            throw new EmailAlreadyUsedException();
        }

        ApplicationProperties.SignUp signUp = applicationProperties.getSignUp();

        if (!signUp.isEnabled() && (tenantService.isMultiTenantEnabled() || userService.countActiveUsers() > 0)) {
            throw new AccountResourceException("Sign-up is disabled", AccountErrorType.SIGN_UP_DISABLED);
        }

        if (tenantService.isMultiTenantEnabled()) {
            if (tenantService.tenantIdsByUserLoginExist(managedUserVM.getLogin())) {
                throw new LoginAlreadyUsedException();
            }

            if (tenantService.tenantIdsByUserEmailExist(managedUserVM.getEmail())) {
                throw new EmailAlreadyUsedException();
            }
        } else {
            if (userService.countActiveUsers() > 0) {
                throw new AccountResourceException(
                    "Organization already exists", AccountErrorType.ORGANIZATION_ALREADY_EXISTS);
            }
        }

        if (isPasswordLengthInvalid(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }

        User user = userService.registerUser(managedUserVM, managedUserVM.getPassword());

        if (signUp.isActivationRequired()) {
            mailService.sendActivationEmail(user);
        } else {
            activateAccount(user.getActivationKey());
        }
    }

    /**
     * {@code GET  /activate} : activate the registered user.
     *
     * @param key the activation key.
     * @throws AccountResourceException {@code 500 (Internal Server Error)} if the user couldn't be activated.
     */
    @GetMapping("/activate")
    public void activateAccount(@RequestParam(value = "key") String key) {
        Optional<User> userOptional = userService.activateRegistration(key);

        if (userOptional.isEmpty()) {
            throw new AccountResourceException(
                "No user was found for this activation key", AccountErrorType.USER_NOT_FOUND);
        }

        if (tenantService.isMultiTenantEnabled()) {
            String tenantId = tenantService.createTenant();

            User user = userOptional.get();

            user.setId(null);

            TenantUtils.runWithTenantId(tenantId, () -> userService.saveUser(user));
        }
    }

    /**
     * {@code GET  /authenticate} : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request.
     * @return the login if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");

        return request.getRemoteUser();
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user.
     * @throws AccountResourceException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/account")
    public AdminUserDTO getAccount() {
        List<Authority> authorities = authorityService.getAuthorities();

        return userService
            .fetchCurrentUser()
            .map(user -> new AdminUserDTO(user, authorities))
            .orElseThrow(() -> new AccountResourceException(
                "User could not be found", AccountErrorType.USER_NOT_FOUND));
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws AccountResourceException  {@code 500 (Internal Server Error)} if the user login wasn't found.
     */
    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException(
                "Current user login not found", AccountErrorType.USER_NOT_FOUND));

        Optional<User> existingUser = userService.fetchUserByEmail(userDTO.getEmail());

        if (existingUser.isPresent() && !isEqualsIgnoreCase(existingUser, userLogin)) {
            throw new EmailAlreadyUsedException();
        }

        if (tenantService.isMultiTenantEnabled()) {
            List<String> tenantIds = tenantService.getTenantIdsByUserEmail(userDTO.getEmail());

            if (!tenantIds.contains(TenantContext.getCurrentTenantId())) {
                throw new EmailAlreadyUsedException();
            }
        }

        Optional<User> user;

        user = userService.fetchUserByLogin(userLogin);

        if (user.isEmpty()) {
            throw new AccountResourceException("User could not be found", AccountErrorType.USER_NOT_FOUND);
        }

        userService.updateUser(
            userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(), userDTO.getLangKey(),
            userDTO.getImageUrl());
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }

        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code GET  /account/sessions} : get the current open sessions.
     *
     * @return the current open sessions.
     * @throws AccountResourceException {@code 500 (Internal Server Error)} if the current open sessions couldn't be
     *                                  retrieved.
     */
    @GetMapping("/account/sessions")
    public List<PersistentToken> getCurrentSessions() {
        User user = userService
            .fetchUserByLogin(
                SecurityUtils.getCurrentUserLogin()
                    .orElseThrow(() -> new AccountResourceException(
                        "Current user login not found", AccountErrorType.USER_NOT_FOUND)))
            .orElseThrow(() -> new AccountResourceException(
                "User could not be found", AccountErrorType.USER_NOT_FOUND));

        return persistentTokenService.getUserPersistentTokens(user.getId());
    }

    /**
     * {@code DELETE /account/sessions?series={series}} : invalidate an existing session.
     *
     * - You can only delete your own sessions, not any other user's session - If you delete one of your existing
     * sessions, and that you are currently logged in on that session, you will still be able to use that session, until
     * you quit your browser: it does not work in real time (there is no API for that), it only removes the "remember
     * me" cookie - This is also true if you invalidate your current session: you will still be able to use it until you
     * close your browser or that the session times out. But automatic login (the "remember me" cookie) will not work
     * anymore. There is an API to invalidate the current session, but there is no API to check which session uses which
     * cookie.
     *
     * @param series the series of an existing session.
     * @throws IllegalArgumentException if the series couldn't be URL decoded.
     */
    @DeleteMapping("/account/sessions/{series}")
    public void invalidateSession(@PathVariable("series") String series) {
        String decodedSeries = URLDecoder.decode(series, StandardCharsets.UTF_8);

        SecurityUtils.getCurrentUserLogin()
            .flatMap(userService::fetchUserByLogin)
            .ifPresent(
                u -> persistentTokenService.getUserPersistentTokens(u.getId())
                    .stream()
                    .filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), decodedSeries))
                    .findAny()
                    .ifPresent(t -> persistentTokenService.delete(decodedSeries)));
    }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param email the mail of the user.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String email) {
        Optional<User> user;

        if (tenantService.isMultiTenantEnabled()) {
            user = tenantService.getTenantIdsByUserEmail(email)
                .stream()
                .findFirst()
                .flatMap(tenantId -> TenantUtils.callWithTenantId(
                    tenantId, () -> userService.requestPasswordReset(email)));
        } else {
            user = userService.requestPasswordReset(email);
        }

        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.orElseThrow());
        } else {
            // Pretend the request has been successful to prevent checking which emails really exist
            // but log that an invalid attempt has been made
            log.warn("Password reset requested for non existing mail");
        }
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws AccountResourceException {@code 500 (Internal Server Error)} if the password could not be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }

        Optional<User> user;

        if (tenantService.isMultiTenantEnabled()) {
            String tenantId = tenantService.getTenantIdByUserResetKey(keyAndPassword.getKey());

            if (tenantId == null) {
                throw new AccountResourceException(
                    "No user was found for this reset key", AccountErrorType.USER_NOT_FOUND);
            }

            user = TenantUtils.callWithTenantId(
                tenantId,
                () -> userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey()));
        } else {
            user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());
        }

        if (user.isEmpty()) {
            throw new AccountResourceException("No user was found for this reset key", AccountErrorType.USER_NOT_FOUND);
        }
    }

    private static boolean isEqualsIgnoreCase(Optional<User> existingUser, String userLogin) {
        return OptionalUtils.get(existingUser, User::getLogin)
            .equalsIgnoreCase(userLogin);
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return StringUtils.isEmpty(password) ||
            password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH ||
            password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
}
