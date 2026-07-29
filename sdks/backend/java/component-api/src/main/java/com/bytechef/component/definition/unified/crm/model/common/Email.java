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

package com.bytechef.component.definition.unified.crm.model.common;

/**
 * Represents an email address in ByteChef's unified CRM model, pairing the address with its classification.
 *
 * @param emailAddress     the email address
 * @param emailAddressType the classification of the email address
 *
 * @author Ivica Cardic
 */
public record Email(String emailAddress, EmailAddressType emailAddressType) {

    /**
     * Enumerates the supported classifications for an email address.
     */
    public enum EmailAddressType {

        /**
         * The contact's primary email address.
         */
        PRIMARY,

        /**
         * A work email address.
         */
        WORK,

        /**
         * Any email address that does not fall into the other categories.
         */
        OTHER
    }
}
