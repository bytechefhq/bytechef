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

package com.bytechef.encryption;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class EncryptionTest {

    private Encryption encryption = new Encryption(() -> "tTB1/UBIbYLuCXVi4PPfzA==");

    @Test
    public void testEncrypt() {
        Assertions.assertEquals("EQuGMfU8kiNQIxJ/Y0xoeg==", encryption.encrypt("text"));
    }

    @Test
    public void testDecrypt() {
        Assertions.assertEquals("text", encryption.decrypt("EQuGMfU8kiNQIxJ/Y0xoeg=="));
    }
}
