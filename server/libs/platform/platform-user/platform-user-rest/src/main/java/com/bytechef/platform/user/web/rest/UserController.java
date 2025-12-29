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
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.LoginAlreadyUsedException;
import com.bytechef.platform.user.exception.UserAlreadyExistsException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for managing users.
 * <p>
 * This class accesses the {@link com.bytechef.platform.user.domain.User} entity, and needs to fetch its collection of
 * authorities.
 * <p>
 * For a normal use-case, it would be better to have an eager relationship between User and Authority, and send
 * everything to the client side: there would be no View Model and DTO, a lot less code, and an outer-join which would
 * be good for performance.
 * <p>
 * We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the user and the authorities, because people will quite often do
 * relationships with the user, and we don't want them to get the authorities all the time for nothing (for performance
 * reasons). This is the #1 goal: we should not impact our users' application because of this use-case.</li>
 * <li>Not having an outer join causes n+1 requests to the database. This is not a real issue as we have by default a
 * second-level cache. This means on the first HTTP call we do the n+1 requests, but then all authorities come from the
 * cache, so in fact it's much better than doing an outer join (which will get lots of data from the database, for each
 * HTTP call).</li>
 * <li>As this manages users, for security reasons, we'd rather have a DTO layer.</li>
 * </ul>
 * <p>
 * Another option would be to have a specific JPA entity graph to handle this case.
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private static final List<String> ALLOWED_ORDERED_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(
            "id", "login", "firstName", "lastName", "email", "activated", "langKey", "createdBy", "createdDate",
            "lastModifiedBy", "lastModifiedDate"));

    private final AuthorityService authorityService;
    private final UserService userService;
    private final MailService mailService;

    @SuppressFBWarnings("EI")
    public UserController(AuthorityService authorityService, UserService userService, MailService mailService) {
        this.authorityService = authorityService;
        this.userService = userService;
        this.mailService = mailService;
    }

    /**
     * {@code POST  /internal/users} : Creates a new user.
     * <p>
     * Creates a new user if the login and email are not already used, and sends an mail with an activation link. The
     * user needs to be activated on creation.
     *
     * @param userDTO the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new user, or with status
     *         {@code 400 (Bad Request)} if the login or email is already in use.
     * @throws URISyntaxException         if the Location URI syntax is incorrect.
     * @throws EmailAlreadyUsedException  {@code 400 (Bad Request)} if the email is already in use.
     * @throws LoginAlreadyUsedException  {@code 400 (Bad Request)} if the login is already in use.
     * @throws UserAlreadyExistsException {@code 400 (Bad Request)} if a new user already has an ID.
     */
    @PostMapping("/users")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<User> createUser(@Valid @RequestBody AdminUserDTO userDTO) throws URISyntaxException {
        logger.debug("REST request to save User : {}", userDTO);

        String login = userDTO.getLogin();

        if (userDTO.getId() != null) {
            throw new UserAlreadyExistsException();
            // Lowercase the user login before comparing with database
        } else if (loginExists(login)) {
            throw new LoginAlreadyUsedException();
        } else if (emailExists(userDTO.getEmail())) {
            throw new EmailAlreadyUsedException();
        } else {
            User newUser = userService.create(userDTO);

            mailService.sendCreationEmail(newUser);

            return ResponseEntity.created(new URI("/api/internal/users/" + newUser.getLogin()))
                .body(newUser);
        }
    }

    /**
     * {@code PUT /internal/users} : Updates an existing User.
     *
     * @param userDTO the user to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated user.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already in use.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already in use.
     */
    @PutMapping("/users")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<AdminUserDTO> updateUser(@Valid @RequestBody AdminUserDTO userDTO) {
        logger.debug("REST request to update User : {}", userDTO);

        Optional<User> existingUser = userService.fetchUserByEmail(userDTO.getEmail());

        if (existingUser.isPresent() && (!existingUser.orElseThrow()
            .getId()
            .equals(userDTO.getId()))) {

            throw new EmailAlreadyUsedException();
        }

        String login = userDTO.getLogin();

        existingUser = userService.fetchUserByLogin(login.toLowerCase());

        if (existingUser.isPresent() && (!existingUser.orElseThrow()
            .getId()
            .equals(userDTO.getId()))) {

            throw new LoginAlreadyUsedException();
        }

        List<Authority> authorities = authorityService.getAuthorities();

        Optional<AdminUserDTO> updatedUser = userService.update(userDTO)
            .map(user -> new AdminUserDTO(user, authorities));

        return updatedUser
            .map(response -> ResponseEntity.ok()
                .body(response))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * {@code GET /internal/users} : get all users with all the details - calling this are only allowed for the
     * administrators.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all users.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers(Pageable pageable) {
        logger.debug("REST request to get all User for an admin");

        if (!onlyContainsAllowedProperties(pageable)) {
            return ResponseEntity.badRequest()
                .build();
        }

        List<Authority> authorities = authorityService.getAuthorities();

        final Page<AdminUserDTO> page = userService.getAllManagedUsers(pageable)
            .map(user -> new AdminUserDTO(user, authorities));

        return new ResponseEntity<>(page.getContent(), HttpStatus.OK);
    }

    private boolean onlyContainsAllowedProperties(Pageable pageable) {
        return pageable.getSort()
            .stream()
            .map(Sort.Order::getProperty)
            .allMatch(ALLOWED_ORDERED_PROPERTIES::contains);
    }

    /**
     * {@code GET /internal/users/:login} : get the "login" user.
     *
     * @param login the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the "login" user, or with status
     *         {@code 404 (Not Found)}.
     */
    @GetMapping("/users/{login}")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<AdminUserDTO> getUser(
        @PathVariable("login") @Pattern(regexp = UserConstants.LOGIN_REGEX) String login) {

        logger.debug("REST request to get User : {}", login);

        List<Authority> authorities = authorityService.getAuthorities();

        return userService.fetchUserByLogin(login)
            .map(user -> new AdminUserDTO(user, authorities))
            .map(response -> ResponseEntity.ok()
                .body(response))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * {@code DELETE /internal/users/:login} : delete the "login" User.
     *
     * @param login the login of the user to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/users/{login}")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteUser(
        @PathVariable("login") @Pattern(regexp = UserConstants.LOGIN_REGEX) String login) {

        logger.debug("REST request to delete User: {}", login);

        userService.delete(login);

        return ResponseEntity.noContent()
            .build();
    }

    private boolean emailExists(String email) {
        return userService.fetchUserByEmail(email)
            .isPresent();
    }

    private boolean loginExists(String login) {
        return userService.fetchUserByLogin(login.toLowerCase())
            .isPresent();
    }
}
