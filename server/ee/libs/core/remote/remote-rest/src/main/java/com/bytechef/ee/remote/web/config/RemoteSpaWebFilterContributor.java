/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.web.config;

import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteSpaWebFilterContributor implements SpaWebFilterContributor {

    @Override
    public List<String> getNonSpaPathPrefixes() {
        return List.of("/remote");
    }
}
