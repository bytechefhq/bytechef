/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel.CredentialStoreTypeEnum;
import com.bytechef.platform.credential.store.CredentialStoreType;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("com.bytechef.ee.embedded.configuration.web.rest.mapper.CredentialStoreTypeMapper")
public class CredentialStoreTypeMapper {

    public CredentialStoreTypeEnum mapToCredentialStoreTypeEnum(CredentialStoreType credentialStoreType) {
        if (credentialStoreType == null) {
            return null;
        }

        return CredentialStoreTypeEnum.valueOf(credentialStoreType.name());
    }

    public CredentialStoreType mapToCredentialStoreType(CredentialStoreTypeEnum credentialStoreTypeEnum) {
        if (credentialStoreTypeEnum == null) {
            return null;
        }

        return CredentialStoreType.valueOf(credentialStoreTypeEnum.name());
    }
}
