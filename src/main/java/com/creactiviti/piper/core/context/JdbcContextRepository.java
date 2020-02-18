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
 */
package com.creactiviti.piper.core.context;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.creactiviti.piper.core.json.JsonHelper;
import com.creactiviti.piper.core.uuid.UUIDGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author Arik Cohe
 * @since Apt 7, 2017
 */
public class JdbcContextRepository implements ContextRepository {

  private JdbcTemplate jdbc;
  private ObjectMapper objectMapper = new ObjectMapper();
  
  @Override
  public Context push(String aStackId, Context aContext) {
    jdbc.update("insert into context (id,stack_id,serialized_context,create_time) values (?,?,?,?)",UUIDGenerator.generate(),aStackId,JsonHelper.writeValueAsString(objectMapper, aContext), new Date());
    return aContext;
  }

  @Override
  public Context peek (String aStackId) {
    try {
      String sql = "select id,serialized_context from context where stack_id = ? order by create_time desc limit 1";
      return jdbc.queryForObject(sql,new Object[]{aStackId},this::contextRowMapper);
    }
    catch (EmptyResultDataAccessException e) {
      return null;
    }
  }
  
  @Override
  public List<Context> getStack (String aStackId) {
    String sql = "select id,serialized_context from context where stack_id = ? order by create_time desc";
    return jdbc.query(sql, this::contextRowMapper,aStackId);
  }
  
  private Context contextRowMapper (ResultSet aResultSet, int aIndex) throws SQLException {
    String serialized = aResultSet.getString(2);
    return new MapContext(JsonHelper.readValue(objectMapper, serialized, Map.class));    
  }

  public void setJdbcTemplate (JdbcTemplate aJdbcTemplate) {
    jdbc = aJdbcTemplate;
  }
  
  public void setObjectMapper(ObjectMapper aObjectMapper) {
    objectMapper = aObjectMapper;
  }

}
