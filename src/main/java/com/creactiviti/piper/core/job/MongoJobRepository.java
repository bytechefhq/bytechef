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
package com.creactiviti.piper.core.job;

import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.Page;
import com.creactiviti.piper.core.ResultPage;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Updates.set;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

public class MongoJobRepository implements JobRepository {
  private final MongoCollection<Job> collection;

  public static final int DEFAULT_PAGE_SIZE = 20;

  public MongoJobRepository(MongoDatabase aDatabase) {
    this.collection = aDatabase
        .getCollection("job")
        .withDocumentClass(Job.class);
  }

  @Override
  public Job findOne(String aId) {
    return collection
        .find(eq("_id", aId))
        .first();
  }

  @Override
  public Job findJobByTaskId(String aTaskId) {
    return collection
        .find(elemMatch(DSL.EXECUTION, eq("_id", aTaskId)))
        .first();
  }

  @Override
  public Page<Job> findAll(int aPageNumber) {
    int offset = (aPageNumber - 1) * DEFAULT_PAGE_SIZE;
    int limit = DEFAULT_PAGE_SIZE;

    List<Job> items = newArrayList(
        collection
            .find()
            .skip(offset)
            .limit(limit));

    long totalItems = collection
        .countDocuments();

    ResultPage<Job> resultPage = new ResultPage<>(Job.class);
    resultPage.setItems(items);
    resultPage.setNumber(items.size() > 0 ? aPageNumber : 0);
    resultPage.setTotalItems((int) totalItems);
    resultPage.setTotalPages(items.size() > 0 ? (int) totalItems / DEFAULT_PAGE_SIZE + 1 : 0);
    return resultPage;
  }

  @Override
  public Job merge(Job aJob) {
    Bson combinedUpdate = jobToBsonDocument(aJob)
        .entrySet()
        .stream()
        .filter(it -> !DSL.EXECUTION.equals(it.getKey()))
        .map(e -> set(e.getKey(), e.getValue()))
        .collect(collectingAndThen(toList(), Updates::combine));

    collection
        .updateOne(
            eq("_id", aJob.getId()),
            combinedUpdate);

    return aJob;
  }

  @Override
  public void create(Job aJob) {
    collection
        .insertOne(aJob);
  }

  @Override
  public int countRunningJobs() {
    return
        (int) collection
            .countDocuments(eq(DSL.STATUS, JobStatus.CREATED));
  }

  @Override
  public int countCompletedJobsToday() {
    Date today = Date.from(now().truncatedTo(DAYS));

    return (int) collection
        .countDocuments(
            and(
                eq(DSL.STATUS, JobStatus.COMPLETED),
                gte(DSL.END_TIME, today)));
  }

  @Override
  public int countCompletedJobsYesterday() {
    Instant today = now().truncatedTo(DAYS);
    Instant yesterday = today.minus(1, DAYS);

    return (int) collection
        .countDocuments(
            and(
                eq(DSL.STATUS, JobStatus.COMPLETED),
                gte(DSL.END_TIME, Date.from(yesterday)),
                lt(DSL.END_TIME, Date.from(today))));
  }


  private BsonDocument jobToBsonDocument(Job aJob) {
    BsonDocument doc = new BsonDocument();

    collection
        .getCodecRegistry()
        .get(Job.class)
        .encode(new BsonDocumentWriter(doc), aJob, EncoderContext.builder().isEncodingCollectibleDocument(true).build());

    return doc;
  }

  @PostConstruct
  void ensureIndexes() {
    of(DSL.CREATE_TIME, DSL.START_TIME, DSL.END_TIME, DSL.PARENT_TASK_EXECUTION_ID, DSL.STATUS)
        .forEach(it -> collection.createIndex(ascending(it)));
  }


}
