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

package com.bytechef.platform.user.repository;

import com.bytechef.platform.user.domain.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ApiKeyRepository extends ListCrudRepository<ApiKey, Long> {

    List<ApiKey> findAllByType(int type);

    List<ApiKey> findAllByTypeIsNull();

    Optional<ApiKey> findBySecretKeyAndEnvironmentAndType(String secretKey, int environment, int type);

    Optional<ApiKey> findBySecretKeyAndEnvironmentIsNull(String secretKey);
}
