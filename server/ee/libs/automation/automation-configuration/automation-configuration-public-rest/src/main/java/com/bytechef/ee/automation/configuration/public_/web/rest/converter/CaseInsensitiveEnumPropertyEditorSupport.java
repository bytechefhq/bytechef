/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest.converter;

import com.bytechef.ee.automation.configuration.public_.web.rest.model.EnvironmentModel;
import java.beans.PropertyEditorSupport;
import java.util.Locale;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CaseInsensitiveEnumPropertyEditorSupport extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        String value = text == null ? "" : text.trim();

        setValue(value.isEmpty() ? null : EnvironmentModel.fromValue(value.toUpperCase(Locale.ROOT)));
    }
}
