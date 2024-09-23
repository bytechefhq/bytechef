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

import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.user.domain.SigningKey;
import java.security.PublicKey;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface SigningKeyService {

    String create(@NonNull SigningKey signingKey);

    void delete(long id);

    PublicKey getPublicKey(@NonNull String keyId, @NonNull Environment environment);

    SigningKey getSigningKey(long id);

    List<SigningKey> getSigningKeys(@NonNull AppType type);

    SigningKey update(@NonNull SigningKey signingKey);
}
