/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.encryption;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.KeyGenerator;

public abstract class AbstractEncryptionKey implements EncryptionKey {

    protected String key;

    @Override
    public String getKey() {
        return key;
    }

    protected String generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

        //Creating a SecureRandom object
        SecureRandom secureRandom = new SecureRandom();

        //Initializing the KeyGenerator
        keyGenerator.init(secureRandom);

        //Creating/Generating a key
        Key key = keyGenerator.generateKey();

        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
