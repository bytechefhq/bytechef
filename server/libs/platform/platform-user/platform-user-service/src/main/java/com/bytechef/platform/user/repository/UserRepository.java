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

package com.bytechef.platform.user.repository;

import com.bytechef.platform.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface UserRepository extends ListCrudRepository<User, Long>, ListPagingAndSortingRepository<User, Long> {

    String USERS_BY_LOGIN_CACHE = "UserRepository.usersByLogin";
    String USERS_BY_EMAIL_CACHE = "UserRepository.usersByEmail";

    long countAllByActivatedIsTrue();

    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(LocalDateTime dateTime);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    Optional<User> findByActivationKey(String activationKey);

    Optional<User> findByAuthProviderAndProviderId(String authProvider, String providerId);

    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE)
    Optional<User> findByEmailIgnoreCase(String email);

    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE)
    Optional<User> findByLogin(String login);

    Optional<User> findByResetKey(String resetKey);
}
