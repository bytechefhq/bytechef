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
 * Represents a phone number in ByteChef's unified CRM model, pairing the number with its classification.
 *
 * @param phoneNumber     the phone number
 * @param phoneNumberType the classification of the phone number
 *
 * @author Ivica Cardic
 */
public record Phone(String phoneNumber, PhoneNumberType phoneNumberType) {

    /**
     * Enumerates the supported classifications for a phone number.
     */
    public enum PhoneNumberType {

        /**
         * The contact's primary phone number.
         */
        PRIMARY,

        /**
         * A mobile phone number.
         */
        MOBILE,

        /**
         * A fax number.
         */
        FAX,

        /**
         * Any phone number that does not fall into the other categories.
         */
        OTHER
    }
}
