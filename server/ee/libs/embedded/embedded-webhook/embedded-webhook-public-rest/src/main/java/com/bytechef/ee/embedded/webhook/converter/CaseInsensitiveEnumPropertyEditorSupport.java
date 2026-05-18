/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.converter;

import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import java.beans.PropertyEditorSupport;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CaseInsensitiveEnumPropertyEditorSupport extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(EnvironmentModel.fromValue(text));
    }
}
