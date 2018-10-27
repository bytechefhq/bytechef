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
package com.creactiviti.piper.core.task;

import com.creactiviti.piper.core.DSL;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;
import static java.text.MessageFormat.format;

public class MongoTaskExecutionRepository implements TaskExecutionRepository {
  private final MongoCollection<TaskExecution> collection;

  public MongoTaskExecutionRepository(MongoDatabase database) {
    this.collection = database
        .getCollection("job")
        .withDocumentClass(TaskExecution.class);
  }

  @Override
  public TaskExecution findOne(String aId) {
    return collection
        .aggregate(ImmutableList.of(
            unwind('$' + DSL.EXECUTION),
            match(eq(format("{0}._id", DSL.EXECUTION), aId)),
            replaceRoot('$' + DSL.EXECUTION)
        ))
        .first();
  }

  @Override
  public List<TaskExecution> findByParentId(String aParentId) {
    return collection
        .aggregate(ImmutableList.of(
            unwind('$' + DSL.EXECUTION),
            match(eq(format("{0}.parentId", DSL.EXECUTION), aParentId)),
            unwind('$' + DSL.EXECUTION)
        ))
        .into(new ArrayList<>());
  }


  @Override
  public void create(TaskExecution aTaskExecution) {
    collection
        .updateOne(
            eq("_id", aTaskExecution.getJobId()),
            push(DSL.EXECUTION, aTaskExecution)
        );
  }

  @Override
  public TaskExecution merge(TaskExecution aTaskExecution) {
    collection
        .updateOne(
            and(eq("_id", aTaskExecution.getJobId()), eq("execution._id", aTaskExecution.getId())),
            set(format("{0}.$", DSL.EXECUTION), aTaskExecution)
        );

    return aTaskExecution;
  }

  @Override
  public List<TaskExecution> getExecution(String aJobId) {
    return collection
        .aggregate(ImmutableList.of(
            match(eq("_id", aJobId)),
            unwind('$' + DSL.EXECUTION),
            replaceRoot('$' + DSL.EXECUTION)
        ))
        .into(new ArrayList<>());
  }
}
