/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.connection.config;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToEncryptedStringWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.encryption.Encryption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final Encryption encryption;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        // Delegates to JsonUtils under the hood once the Jackson 3.x ObjectMapper is reachable here
        return Arrays.asList(
            new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
            new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
            new EncryptedStringWrapperToStringConverter(encryption),
            new StringToEncryptedStringWrapperConverter(encryption),
            new MapWrapperToStringConverter(objectMapper),
            new StringToMapWrapperConverter(objectMapper));
    }
}
