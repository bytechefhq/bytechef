
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

package com.bytechef.tag.web.rest.mapper;

import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.mapper.config.TagMapperSpringConfig;
import com.bytechef.tag.web.rest.model.TagModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = TagMapperSpringConfig.class)
public interface TagModelMapper extends Converter<TagModel, Tag> {

    Tag convert(TagModel tagModel);
}
