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

package com.bytechef.platform.user.service;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Ivica Cardic
 */
public interface UserService {

    Optional<User> activateRegistration(String key);

    void changePassword(String currentClearTextPassword, String newPassword);

    Optional<User> completePasswordReset(String newPassword, String key);

    long countActiveUsers();

    User createUser(AdminUserDTO userDTO);

    void deleteUser(String login);

    Page<User> getAllActiveUsers(Pageable pageable);

    Optional<User> fetchCurrentUser();

    Optional<User> fetchUser(long id);

    Optional<User> fetchUserByEmail(String email);

    Optional<User> fetchUserByLogin(String login);

    Page<User> getAllManagedUsers(Pageable pageable);

    User getCurrentUser();

    User getUser(long id);

    void saveUser(User user);

    User registerUser(AdminUserDTO userDTO, String password);

    void removeNotActivatedUsers();

    void removeOldPersistentTokens();

    Optional<User> requestPasswordReset(String email);

    Optional<User> updateUser(AdminUserDTO userDTO);

    void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl);
}
