
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.repository.jdbc.event;

import com.bytechef.atlas.domain.Context;
import com.bytechef.commons.utils.UUIDUtils;
import org.springframework.core.annotation.Order;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@Order(1)
@Component
public class ContextCallback implements BeforeConvertCallback<Context> {

    @Override
    public Context onBeforeConvert(Context context) {
        // TODO check why Auditing does not populate auditing fields
        if (context.isNew()) {
            context.setCreatedBy("system");
            context.setCreatedDate(LocalDateTime.now());
            context.setId(UUIDUtils.generate());
        }

        return context;
    }
}
