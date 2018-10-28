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

import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.uuid.UUIDGenerator;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static java.util.stream.Stream.of;

public class MongoContextRepository implements ContextRepository<Context> {

  private final MongoCollection<Document> collection;

  public MongoContextRepository(MongoDatabase aDatabase) {
    this.collection = aDatabase
        .getCollection("context");
  }

  @Override
  public Context push(String aStackId, Context aContext) {
    collection
        .insertOne(new Document()
            .append("_id", UUIDGenerator.generate())
            .append("stackId", aStackId)
            .append("context", contextToDocument(aContext))
            .append("createTime", new Date())
        );

    return aContext;
  }

  @Override
  public Context peek(String aStackId) {
    Context context = collection
        .withDocumentClass(Context.class)
        .aggregate(ImmutableList.of(
            match(eq("stackId", aStackId)),
            sort(descending("createTime")),
            limit(1),
            replaceRoot("$context")
        ))
        .first();

    if (context != null) {
      return context;
    }

    return null;
  }

  @Override
  public List<Context> getStack(String aStackId) {
    return collection
        .withDocumentClass(Context.class)
        .aggregate(ImmutableList.of(
            match(eq("stackId", aStackId)),
            sort(descending("createTime")),
            replaceRoot("$context")
        ))
        .into(new ArrayList<>());
  }

  private BsonDocument contextToDocument(Context aContext) {
    BsonDocument doc = new BsonDocument();

    collection
        .getCodecRegistry()
        .get(Context.class)
        .encode(new BsonDocumentWriter(doc), aContext, EncoderContext.builder().isEncodingCollectibleDocument(true).build());

    return doc;
  }

  @PostConstruct
  void ensureIndexes() {
    of(DSL.CREATE_TIME, "stackId")
        .forEach(it -> collection.createIndex(ascending(it)));
  }
}
