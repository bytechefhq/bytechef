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

package com.bytechef.encryption;

import com.bytechef.commons.util.EncodingUtils;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractEncryptionKey implements EncryptionKey {

    private @Nullable String key;

    @Override
    public String getKey() {
        if (key == null) {
            key = doGetKey();
        }

        return key;
    }

    protected abstract String doGetKey();

    protected String generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

        // Creating a SecureRandom object
        SecureRandom secureRandom = new SecureRandom();

        // Initializing the KeyGenerator
        keyGenerator.init(secureRandom);

        // Creating/Generating a key
        Key key = keyGenerator.generateKey();

        return EncodingUtils.base64EncodeToString(key.getEncoded());
    }
}
