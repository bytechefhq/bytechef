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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.Date;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.Updates.*;

public class MongoCounterRepository implements CounterRepository {
  private final MongoCollection<Document> collection;

  public MongoCounterRepository(MongoDatabase aDatabase) {
    this.collection = aDatabase
        .getCollection("counter");
  }

  @Override
  public void set(String aCounterName, long aValue) {
    collection
        .findOneAndUpdate(
            eq("_id", aCounterName),
            combine(
                Updates.set(DSL.VALUE, aValue),
                setOnInsert("_id", aCounterName),
                setOnInsert(DSL.CREATE_TIME, new Date())
            ),
            new FindOneAndUpdateOptions()
                .upsert(true)
        );
  }

  @Override
  public long decrement(String aCounterName) {
    Document doc = collection
        .findOneAndUpdate(
            eq("_id", aCounterName),
            inc(DSL.VALUE, -1),
            new FindOneAndUpdateOptions()
                .returnDocument(AFTER)
                .projection(include(DSL.VALUE))
        );

    if (doc == null) {
      throw new IllegalArgumentException("Counter not found: " + aCounterName);
    }

    return doc.getLong(DSL.VALUE);
  }


  @Override
  public void delete(String aCounterName) {
    collection
        .deleteOne(eq("_id", aCounterName));
  }

}
