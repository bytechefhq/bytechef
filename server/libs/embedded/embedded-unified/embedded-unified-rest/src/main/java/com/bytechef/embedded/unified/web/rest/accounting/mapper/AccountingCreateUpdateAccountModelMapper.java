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

package com.bytechef.embedded.unified.web.rest.accounting.mapper;

import com.bytechef.component.definition.unified.accounting.model.AccountUnifiedInputModel;
import com.bytechef.embedded.unified.web.rest.accounting.model.CreateUpdateAccountModel;
import com.bytechef.embedded.unified.web.rest.mapper.JsonNullableMapper;
import com.bytechef.embedded.unified.web.rest.mapper.config.UnifiedConfigurationMapperSpringConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = UnifiedConfigurationMapperSpringConfig.class, uses = {
    JsonNullableMapper.class
})
public interface AccountingCreateUpdateAccountModelMapper
    extends Converter<CreateUpdateAccountModel, AccountUnifiedInputModel> {

    @Override
    AccountUnifiedInputModel convert(CreateUpdateAccountModel source);
}
