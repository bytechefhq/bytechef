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

package com.bytechef.platform.connection.web.rest.util;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.web.rest.model.ConnectionModel;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
public class ConnectionApiControllerUtils {

    public static ResponseEntity<ConnectionModel> getConnection(
        Long id, ConnectionFacade connectionFacade, ConversionService conversionService) {

        return ResponseEntity.ok(
            Validate.notNull(
                conversionService.convert(
                    connectionFacade.getConnection(Validate.notNull(id, "id")), ConnectionModel.class),
                "connection")
                .parameters(null));
    }

    public static ResponseEntity<ConnectionModel> updateConnection(
        Long id, ConnectionModel connectionModel, ConnectionFacade connectionFacade,
        ConversionService conversionService) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectionFacade.update(conversionService.convert(connectionModel.id(id), ConnectionDTO.class)),
                ConnectionModel.class));
    }
}
