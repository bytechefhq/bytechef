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

package com.bytechef.platform.user.service;

import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.InvalidEmailException;
import com.bytechef.platform.user.exception.InvalidPasswordException;
import com.bytechef.platform.user.exception.LoginAlreadyUsedException;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();

    private final AuthorityRepository authorityRepository;
    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;
    private final PersistentTokenRepository persistentTokenRepository;
    private final UserRepository userRepository;

    public UserServiceImpl(
        AuthorityRepository authorityRepository, CacheManager cacheManager, PasswordEncoder passwordEncoder,
        PersistentTokenRepository persistentTokenRepository, UserRepository userRepository) {

        this.authorityRepository = authorityRepository;
        this.cacheManager = cacheManager;
        this.passwordEncoder = passwordEncoder;
        this.persistentTokenRepository = persistentTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> activateRegistration(String key) {
        logger.debug("Activating user for activation key {}", key);

        return userRepository.findByActivationKey(key)
            .map(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                user.setActivationKey(null);

                user = userRepository.save(user);

                this.clearUserCaches(user);

                logger.debug("Activated user: {}", user);

                return user;
            });
    }

    @Override
    public Optional<User> completePasswordReset(String newPassword, String key) {
        logger.debug("Reset user password for reset key {}", key);

        return userRepository.findByResetKey(key)
            .filter(user -> {
                Instant resetDate = user.getResetDate();

                return resetDate.isAfter(Instant.now()
                    .minus(1, ChronoUnit.DAYS));
            })
            .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);

                user = userRepository.save(user);

                this.clearUserCaches(user);

                return user;
            });
    }

    @Override
    public long countActiveUsers() {
        return userRepository.countAllByActivatedIsTrue();
    }

    @Override
    @Transactional
    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils.fetchCurrentUserLogin()
            .flatMap(userRepository::findByLogin)
            .ifPresent(user -> {
                String currentEncryptedPassword = user.getPassword();

                if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                    throw new InvalidPasswordException();
                }

                String encryptedPassword = passwordEncoder.encode(newPassword);

                user.setPassword(encryptedPassword);

                user = userRepository.save(user);

                this.clearUserCaches(user);

                logger.debug("Changed password for User: {}", user);
            });
    }

    @Override
    public User create(AdminUserDTO userDTO) {
        User user = new User();

        String login = userDTO.getLogin();

        user.setLogin(login.toLowerCase());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());

        String email = userDTO.getEmail();

        if (email != null) {
            if (!EMAIL_VALIDATOR.isValid(userDTO.getEmail())) {
                throw new InvalidEmailException(email.toLowerCase());
            }

            user.setEmail(email.toLowerCase());
        }

        user.setImageUrl(userDTO.getImageUrl());

        if (userDTO.getLangKey() == null) {
            user.setLangKey(UserConstants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }

        String encryptedPassword = passwordEncoder.encode(RandomUtils.generatePassword());

        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtils.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);

        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO.getAuthorities()
                .stream()
                .map(authorityRepository::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

            user.setAuthorities(authorities);
        }

        userRepository.save(user);

        this.clearUserCaches(user);

        logger.debug("Created User: {}", user);

        return user;
    }

    @Override
    public void delete(String login) {
        userRepository.findByLogin(login)
            .ifPresent(user -> {
                userRepository.delete(user);

                this.clearUserCaches(user);

                logger.debug("Deleted User: {}", user);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> fetchCurrentUser() {
        return SecurityUtils.fetchCurrentUserLogin()
            .flatMap(userRepository::findByLogin);
    }

    @Override
    public Optional<User> fetchUser(long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> fetchUserByAuthProviderAndProviderId(String authProvider, String providerId) {
        return userRepository.findByAuthProviderAndProviderId(authProvider, providerId);
    }

    @Override
    public Optional<User> fetchUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> fetchUserByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Override
    @Transactional
    public User findOrCreateSocialUser(
        String email, String firstName, String lastName, String imageUrl, String authProvider, String providerId,
        boolean autoProvision, String defaultAuthority) {

        Optional<User> existingUser = userRepository.findByAuthProviderAndProviderId(authProvider, providerId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setImageUrl(imageUrl);

            user = userRepository.save(user);

            clearUserCaches(user);

            return user;
        }

        Optional<User> existingEmailUser = userRepository.findByEmailIgnoreCase(email);

        if (existingEmailUser.isPresent()) {
            User user = existingEmailUser.get();

            user.setAuthProvider(authProvider);
            user.setProviderId(providerId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setImageUrl(imageUrl);

            if (!user.isActivated()) {
                user.setActivated(true);
                user.setActivationKey(null);
            }

            user = userRepository.save(user);

            clearUserCaches(user);

            return user;
        }

        if (!autoProvision) {
            throw new IllegalStateException("Auto-provisioning is disabled for this identity provider");
        }

        User newUser = new User();

        newUser.setLogin(email.toLowerCase());
        newUser.setEmail(email.toLowerCase());
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setImageUrl(imageUrl);
        newUser.setAuthProvider(authProvider);
        newUser.setProviderId(providerId);
        newUser.setActivated(true);
        newUser.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        newUser.setPassword(passwordEncoder.encode(RandomUtils.generatePassword()));

        Set<Authority> authorities = new HashSet<>();

        authorityRepository.findByName(defaultAuthority)
            .ifPresent(authorities::add);

        newUser.setAuthorities(authorities);

        userRepository.save(newUser);

        clearUserCaches(newUser);

        logger.debug("Created social login user: {}", newUser);

        return newUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public User getCurrentUser() {
        return fetchCurrentUser()
            .orElseThrow(UserNotFoundException::new);
    }

    /**
     * Persistent Token are used for providing automatic authentication, they should be automatically deleted after 30
     * days.
     * <p>
     * This is scheduled to get fired everyday, at midnight.
     */
    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public void removeOldPersistentTokens() {
        LocalDate now = LocalDate.now();

        persistentTokenRepository.findAllByTokenDateBefore(now.minusMonths(1))
            .forEach(token -> {
                logger.debug("Deleting token {}", token.getSeries());

                persistentTokenRepository.delete(token);
            });
    }

    @Override
    public Optional<User> requestPasswordReset(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .filter(User::isActivated)
            .map(user -> {
                user.setResetKey(RandomUtils.generateResetKey());
                user.setResetDate(Instant.now());

                user = userRepository.save(user);

                this.clearUserCaches(user);

                return user;
            });
    }

    @Override
    public User registerUser(AdminUserDTO userDTO, String password) {
        if (!EMAIL_VALIDATOR.isValid(userDTO.getEmail())) {
            throw new InvalidEmailException(userDTO.getEmail());
        }

        String login = userDTO.getLogin();

        userRepository.findByLogin(login.toLowerCase())
            .ifPresent(existingUser -> {
                boolean removed = removeNonActivatedUser(existingUser);

                if (!removed) {
                    throw new LoginAlreadyUsedException();
                }
            });

        String email = userDTO.getEmail();

        userRepository.findByEmailIgnoreCase(email)
            .ifPresent(existingUser -> {
                boolean removed = removeNonActivatedUser(existingUser);
                if (!removed) {
                    throw new EmailAlreadyUsedException();
                }
            });

        User newUser = new User();

        String encryptedPassword = passwordEncoder.encode(password);

        newUser.setLogin(login.toLowerCase());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());

        if (email != null) {
            newUser.setEmail(email.toLowerCase());
        }

        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtils.generateActivationKey());

        Set<Authority> authorities = new HashSet<>();

        authorityRepository.findByName(AuthorityConstants.ADMIN)
            .ifPresent(authorities::add);

        newUser.setAuthorities(authorities);

        userRepository.save(newUser);

        this.clearUserCaches(newUser);

        logger.debug("Created User: {}", newUser);

        return newUser;
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    @Override
    public Optional<User> update(AdminUserDTO userDTO) {
        return Optional.of(userRepository.findById(userDTO.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(user -> {
                this.clearUserCaches(user);

                String login = userDTO.getLogin();

                user.setLogin(login.toLowerCase());
                user.setFirstName(userDTO.getFirstName());
                user.setLastName(userDTO.getLastName());

                if (userDTO.getEmail() != null) {
                    String email = userDTO.getEmail();

                    user.setEmail(email.toLowerCase());
                }

                user.setImageUrl(userDTO.getImageUrl());
                user.setActivated(userDTO.isActivated());
                user.setLangKey(userDTO.getLangKey());
                user.setAuthorities(
                    userDTO.getAuthorities()
                        .stream()
                        .map(authorityRepository::findByName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet()));

                user = userRepository.save(user);

                this.clearUserCaches(user);

                logger.debug("Changed Information for User: {}", user);

                return user;
            });
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user.
     * @param lastName  last name of user.
     * @param email     email id of user.
     * @param langKey   language key.
     * @param imageUrl  image URL of user.
     */
    @Override
    public void update(String firstName, String lastName, String email, String langKey, String imageUrl) {
        if (!EMAIL_VALIDATOR.isValid(email)) {
            throw new InvalidEmailException(email);
        }

        SecurityUtils.fetchCurrentUserLogin()
            .flatMap(userRepository::findByLogin)
            .ifPresent(user -> {
                user.setFirstName(firstName);
                user.setLastName(lastName);

                if (email != null) {
                    user.setEmail(email.toLowerCase());
                }

                user.setLangKey(langKey);
                user.setLogin(user.getEmail());
                user.setImageUrl(imageUrl);

                userRepository.save(user);

                this.clearUserCaches(user);

                logger.debug("Changed Information for User: {}", user);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllActiveUsers(Pageable pageable) {
        return userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable);
    }

    @Override
    public User getUser(long id) {
        return userRepository.findById(id)
            .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Override
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
            .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
                LocalDateTimeUtils.toLocalDateTime(
                    Instant.now()
                        .minus(3, ChronoUnit.DAYS)))
            .forEach(user -> {
                logger.debug("Deleting not activated user {}", user.getLogin());

                userRepository.delete(user);

                this.clearUserCaches(user);
            });
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE))
            .evict(TenantCacheKeyUtils.getKey(user.getLogin()));

        if (user.getEmail() != null) {
            Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE))
                .evict(TenantCacheKeyUtils.getKey(user.getEmail()));
        }
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.isActivated()) {
            return false;
        }

        userRepository.delete(existingUser);

        this.clearUserCaches(existingUser);

        return true;
    }
}
