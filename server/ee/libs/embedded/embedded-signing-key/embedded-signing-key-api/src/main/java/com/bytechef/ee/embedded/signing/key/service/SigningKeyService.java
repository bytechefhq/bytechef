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

package com.bytechef.ee.embedded.signing.key.service;

import com.bytechef.ee.embedded.signing.key.domain.SigningKey;
import com.bytechef.platform.constant.ModeType;
import java.security.PublicKey;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface SigningKeyService {

    String create(SigningKey signingKey);

    void delete(long id);

    PublicKey getPublicKey(String keyId);

    SigningKey getSigningKey(long id);

    List<SigningKey> getSigningKeys(ModeType type);

    SigningKey update(SigningKey signingKey);
}
