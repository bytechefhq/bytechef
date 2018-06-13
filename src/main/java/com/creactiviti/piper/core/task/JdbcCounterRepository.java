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
 */package com.creactiviti.piper.core.task;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.annotation.Transactional;

public class JdbcCounterRepository implements CounterRepository {

  private final JdbcOperations jdbc;
  
  
  public JdbcCounterRepository (JdbcOperations aJdbcOperations) {
    jdbc = aJdbcOperations;
  }
  
  @Override
  @Transactional
  public void set (String aCounterName, long aValue) {
    try {
      jdbc.queryForObject("select value from counter where id = ? for update", Long.class, aCounterName);
      jdbc.update("update counter set value = ? where id = ?",aValue,aCounterName);
    }
    catch (EmptyResultDataAccessException e) {
      jdbc.update("insert into counter (id,value,create_time) values (?,?,current_timestamp)",aCounterName,aValue);
    }
  }
  
  @Override
  @Transactional
  public long decrement(String aCounterName) {
    Long value = jdbc.queryForObject("select value from counter where id = ? for update", Long.class, aCounterName);
    value = value - 1;
    jdbc.update("update counter set value = ? where id = ?",value,aCounterName);
    return value;
  }
  

  @Override
  public void delete (String aCounterName) {
    jdbc.update("delete from counter where id = ?",aCounterName);
  }

}
