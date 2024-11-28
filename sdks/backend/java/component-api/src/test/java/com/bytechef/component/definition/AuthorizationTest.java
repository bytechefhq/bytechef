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

package com.bytechef.component.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Igor Beslic
 */
public class AuthorizationTest {

    @Test
    public void testAuthorizationAuthorizationTypeIsApplicable() {

        for (String applicableValue : getRandomApplicableValues()) {
            Assertions.assertTrue(
                Authorization.AuthorizationType.isApplicable(applicableValue), applicableValue + " is valid");
        }

        for (String nonApplicableValue : getRandomNonApplicableValues()) {
            Assertions.assertFalse(
                Authorization.AuthorizationType.isApplicable(nonApplicableValue), nonApplicableValue + " is valid");
        }

    }

    private static Random random = new Random();

    private List<String> getRandomApplicableValues() {
        Authorization.AuthorizationType[] values = Authorization.AuthorizationType.values();

        List<String> names = new ArrayList<>();

        for (Authorization.AuthorizationType value : values) {
            if (value == Authorization.AuthorizationType.NONE) {
                continue;
            }

            String name = value.getName();

            if (random.nextBoolean()) {
                names.add(name.toLowerCase());

                continue;
            }

            if (random.nextBoolean()) {
                char charToReplace = name.charAt(random.nextInt(name.length()));

                if (charToReplace == 95) {
                    continue;
                }

                names.add(name.replace(charToReplace, (char) (charToReplace - 32)));

                continue;
            }

            names.add(name);

        }

        return names;
    }

    private List<String> getRandomNonApplicableValues() {
        List<String> randomApplicableValues = getRandomApplicableValues();

        List<String> nonApplicableValues = new ArrayList<>();

        for (String randomApplicableValue : randomApplicableValues) {
            nonApplicableValues.add(randomApplicableValue + "_wrong");
        }

        nonApplicableValues.add("none");
        nonApplicableValues.add("None");
        nonApplicableValues.add("nOne");
        nonApplicableValues.add("noNe");
        nonApplicableValues.add("nonE");
        nonApplicableValues.add("NonE");
        nonApplicableValues.add("nOnE");
        nonApplicableValues.add("noNE");
        nonApplicableValues.add("NoNE");
        nonApplicableValues.add("nONE");
        nonApplicableValues.add("NONE");

        return nonApplicableValues;
    }

}
