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

package com.bytechef.component.google.slides.constant;

/**
 * @author Monika Kušter
 */
public class GoogleSlidesConstants {

    private GoogleSlidesConstants() {
    }

    public static final String CREATE_PRESENTATION_BASED_ON_TEMPLATE = "createPresentationBasedOnTemplate";
    public static final String CREATE_PRESENTATION_BASED_ON_TEMPLATE_DESCRIPTION =
        "Creates a new presentation based on an existing one and can replace any placeholder variables found in " +
            "your template presentation, like [[name]], [[email]], etc.";
    public static final String CREATE_PRESENTATION_BASED_ON_TEMPLATE_TITLE = "Create Presentation Based on Template";
    public static final String NAME = "name";
    public static final String VALUES = "values";
}
