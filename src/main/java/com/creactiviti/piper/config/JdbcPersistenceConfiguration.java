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
package com.creactiviti.piper.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.creactiviti.piper.core.context.JdbcContextRepository;
import com.creactiviti.piper.core.job.JdbcJobRepository;
import com.creactiviti.piper.core.task.CounterRepository;
import com.creactiviti.piper.core.task.JdbcCounterRepository;
import com.creactiviti.piper.core.task.JdbcTaskExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnProperty(name="piper.persistence.provider",havingValue="jdbc")
@Import(value = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class})
public class JdbcPersistenceConfiguration {

  @Bean
  JdbcTaskExecutionRepository jdbcJobTaskRepository (NamedParameterJdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
    JdbcTaskExecutionRepository jdbcJobTaskRepository = new JdbcTaskExecutionRepository();
    jdbcJobTaskRepository.setJdbcOperations(aJdbcTemplate);
    jdbcJobTaskRepository.setObjectMapper(aObjectMapper);
    return jdbcJobTaskRepository;
  }
  
  @Bean
  JdbcJobRepository jdbcJobRepository (NamedParameterJdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
    JdbcJobRepository jdbcJobRepository = new JdbcJobRepository();
    jdbcJobRepository.setJdbcOperations(aJdbcTemplate);
    jdbcJobRepository.setJobTaskRepository(jdbcJobTaskRepository(aJdbcTemplate,aObjectMapper));
    return jdbcJobRepository;
  }
  
  @Bean
  JdbcContextRepository jdbcContextRepository (JdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
    JdbcContextRepository repo = new JdbcContextRepository();
    repo.setJdbcTemplate(aJdbcTemplate);
    repo.setObjectMapper(aObjectMapper);
    return repo;
  }
  
  @Bean
  CounterRepository counterRepository (JdbcTemplate aJdbcOperations) {
    return new JdbcCounterRepository(aJdbcOperations);
  }
  
}
