
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

package com.bytechef.hermes.component.registrar.jdbc.handler;

import com.bytechef.hermes.component.registrar.jdbc.sql.DataSourceFactory;
import com.bytechef.hermes.component.registrar.handler.DefaultComponentActionTaskHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.component.factory.InputParametersFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentActionTaskHandler extends DefaultComponentActionTaskHandler implements InitializingBean {

    private final DataSourceFactory dataSourceFactory;

    @SuppressFBWarnings("EI2")
    public JdbcComponentActionTaskHandler(
        ActionDefinition actionDefinition, ContextFactory contextFactory, DataSourceFactory dataSourceFactory,
        InputParametersFactory inputParametersFactory, JdbcComponentHandler jdbcComponentHandler) {

        super(actionDefinition, jdbcComponentHandler, contextFactory, inputParametersFactory);

        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public void afterPropertiesSet() {
        ((JdbcComponentHandler) componentHandler).setDataSourceFactory(dataSourceFactory);
    }
}
