/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.repository.engine.jdbc.context;

/**
 *
 * @author Arik Cohe
 * @author Ivica Cardic
 * @since Apt 7, 2017
 */
public class PostgresJdbcContextRepository extends AbstractJdbcContextRepository {

    @Override
    protected String getPushSql() {
        return "insert into context (id,stack_id,serialized_context,create_time) values (?,?,?::jsonb,?)";
    }
}
