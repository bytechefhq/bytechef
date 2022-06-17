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

package com.bytechef.task.handler.mysql.v1_0;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class MySQLTaskDescriptorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testGetMySQLTaskDescriptors() throws JsonProcessingException {
        JSONAssert.assertEquals(
                """
            {"auth":{"options":[{"value":"mysql"}]},"description":"Query, insert nd update data from MySQL.","displayName":"MySQL","name":"mysql","operations":[{"description":"Execute an SQL query.","name":"query","inputs":[{"description":"The raw SQL query to execute. You can use expressions or :property1 and :property2 in conjunction with parameters.","displayName":"Query","name":"query","placeholder":"SELECT id, name FROM customer WHERE age > :age AND height <= :height","required":true,"type":"STRING"},{"description":"The list of properties which should be used as query parameters.","displayName":"Parameters","name":"parameters","type":"OBJECT","properties":[{"type":"BOOLEAN"},{"type":"DATE_TIME"},{"type":"NUMBER"},{"type":"STRING"}]}],"displayName":"Query"},{"description":"Insert rows in database.","name":"insert","inputs":[{"description":"Name of the schema the table belongs to.","displayName":"Schema","name":"schema","required":true,"defaultValue":"public","type":"STRING"},{"description":"Name of the table in which to insert data to.","displayName":"Table","name":"table","required":true,"type":"STRING"},{"description":"The list of the properties which should used as columns for the new rows.","displayName":"Columns","name":"columns","type":"ARRAY","items":[{"type":"STRING"}]},{"description":"List of rows.","displayName":"Rows","name":"rows","type":"ARRAY","items":[{"type":"OBJECT","additionalProperties":true}]}],"displayName":"Insert"},{"description":"Update rows in database.","name":"update","inputs":[{"description":"Name of the schema the table belongs to.","displayName":"Schema","name":"schema","required":true,"defaultValue":"public","type":"STRING"},{"description":"Name of the table in which to update data in.","displayName":"Table","name":"table","required":true,"type":"STRING"},{"description":"The list of the properties which should used as columns for the updated rows.","displayName":"Columns","name":"columns","type":"ARRAY","items":[{"type":"STRING"}]},{"description":"The name of the property which decides which rows in the database should be updated.","displayName":"Update Key","name":"updateKey","placeholder":"id","type":"STRING"},{"description":"List of rows.","displayName":"Rows","name":"rows","type":"ARRAY","items":[{"type":"OBJECT","additionalProperties":true}]}],"displayName":"Update"},{"description":"Delete rows from database.","name":"delete","inputs":[{"description":"Name of the schema the table belongs to.","displayName":"Schema","name":"schema","required":true,"defaultValue":"public","type":"STRING"},{"description":"Name of the table in which to update data in.","displayName":"Table","name":"table","required":true,"type":"STRING"},{"description":"Name of the property which decides which rows in the database should be deleted.","displayName":"Update Key","name":"deleteKey","placeholder":"id","type":"STRING"},{"description":"List of rows.","displayName":"Rows","name":"rows","type":"ARRAY","items":[{"type":"OBJECT","additionalProperties":true}]}],"displayName":"Delete"},{"description":"Execute an SQL DML or DML statement.","name":"execute","inputs":[{"description":"The raw DML or DDL statement to execute. You can use expressions or :property1 and :property2 in conjunction with parameters.","displayName":"Execute","name":"execute","placeholder":"UPDATE TABLE product set name = :name WHERE product > :product AND price <= :price","required":true,"type":"STRING"},{"description":"List of rows.","displayName":"Rows","name":"rows","type":"ARRAY","items":[{"type":"OBJECT","additionalProperties":true}]},{"description":"The list of properties which should be used as parameters.","displayName":"Parameters","name":"parameters","type":"OBJECT","properties":[{"type":"BOOLEAN"},{"type":"DATE_TIME"},{"type":"NUMBER"},{"type":"STRING"}]}],"displayName":"Execute"}],"version":1.0}
            """,
                (JSONObject) JSONParser.parseJSON(
                        objectMapper.writeValueAsString(new MySQLTaskDescriptorHandler().getTaskDescriptor())),
                true);
    }
}
