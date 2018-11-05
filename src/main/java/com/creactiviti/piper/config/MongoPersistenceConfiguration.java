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

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.context.Context;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.context.MongoContextRepository;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.MongoJobRepository;
import com.creactiviti.piper.core.job.SimpleJob;
import com.creactiviti.piper.core.task.MongoCounterRepository;
import com.creactiviti.piper.core.task.MongoTaskExecutionRepository;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskExecution;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.*;

import static com.mongodb.MongoClient.getDefaultCodecRegistry;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;
import static org.bson.codecs.configuration.CodecRegistries.*;
import static org.bson.codecs.pojo.Conventions.CLASS_AND_PROPERTY_CONVENTION;
import static org.springframework.util.StringUtils.hasText;

@Configuration
@ConditionalOnProperty(name = "piper.persistence.provider", havingValue = "mongo")
public class MongoPersistenceConfiguration {
  @Bean
  MongoTaskExecutionRepository mongoJobTaskRepository(MongoDatabase mongoDatabase, CodecRegistry codecRegistry) {
    MongoTaskExecutionRepository mongoJobTaskRepository = new MongoTaskExecutionRepository(mongoDatabase);
    return mongoJobTaskRepository;
  }

  @Bean
  MongoJobRepository mongoJobRepository(MongoDatabase mongoDatabase, CodecRegistry codecRegistry) {
    MongoJobRepository mongoJobRepository = new MongoJobRepository(mongoDatabase);
    return mongoJobRepository;
  }

  @Bean
  MongoContextRepository mongoContextRepository(MongoDatabase mongoDatabase, CodecRegistry codecRegistry, ObjectMapper objectMapper) {
    MongoContextRepository mongoContextRepository = new MongoContextRepository(mongoDatabase);
    return mongoContextRepository;
  }

  @Bean
  MongoCounterRepository mongoCounterRepository(MongoDatabase mongoDatabase, CodecRegistry codecRegistry, ObjectMapper objectMapper) {
    return new MongoCounterRepository(mongoDatabase);
  }

  @Bean
  ConnectionString mongoConnectionString(@Value("${spring.datasource.url:mongodb://localhost:27017/piper}") URI datasourceURI) {
    return new ConnectionString(datasourceURI.toString());
  }

  @Bean
  MongoClient mongoClient(ConnectionString mongoConnectionString) {
    return MongoClients.create(mongoConnectionString);
  }

  @Bean
  MongoDatabase mongoDatabase(MongoClient mongoClient,
                              ConnectionString mongoConnectionString,
                              CodecRegistry codecRegistry) {
    return mongoClient
        .getDatabase(mongoConnectionString.getDatabase())
        .withCodecRegistry(codecRegistry);
  }

  @Bean
  CodecRegistry codecRegistry() {
    PojoCodecProvider pojoCodecProvider = PojoCodecProvider
        .builder()
        .conventions(ImmutableList.of(CLASS_AND_PROPERTY_CONVENTION))
        .register(Context.class)
        .register(Job.class)
        .register(TaskExecution.class)
        .register(Error.class)
        .register(Accessor.class)
        .build();

    return fromRegistries(
        fromProviders(
            new PiperCodecProvider(),
            pojoCodecProvider,
            new EnumPropertyCodecProvider()),
        getDefaultCodecRegistry(),
        fromCodecs(new StringArrayCodec())
    );
  }

  public static class StringArrayCodec implements Codec<String[]> {
    @Override
    public void encode(final BsonWriter writer, final String[] values, final EncoderContext encoderContext) {
      writer.writeStartArray();
      for (String v : values) {
        writer.writeString(v);
      }
      writer.writeEndArray();
    }

    @Override
    public String[] decode(final BsonReader reader, final DecoderContext decoderContext) {
      List<String> values = new ArrayList<>();
      reader.readStartArray();
      while (reader.getCurrentBsonType() != BsonType.STRING) {
        values.add(reader.readString());
      }
      reader.readEndArray();

      return values.toArray(new String[0]);
    }

    @Override
    public Class<String[]> getEncoderClass() {
      return String[].class;
    }
  }

  final static class EnumPropertyCodecProvider implements CodecProvider {
    EnumPropertyCodecProvider() {

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
      if (Enum.class.isAssignableFrom(clazz)) {
        return (Codec<T>) new EnumCodec(clazz);
      }
      return null;
    }

    private static class EnumCodec<T extends Enum<T>> implements Codec<T> {
      private final Class<T> clazz;

      EnumCodec(final Class<T> clazz) {
        this.clazz = clazz;
      }

      @Override
      public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
        writer.writeString(value.name());
      }

      @Override
      public Class<T> getEncoderClass() {
        return clazz;
      }

      @Override
      public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        return Enum.valueOf(clazz, reader.readString());
      }
    }
  }

  public static class PiperCodecProvider implements CodecProvider {
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
      if (Job.class.isAssignableFrom(clazz)
          || TaskExecution.class.isAssignableFrom(clazz)
          || Context.class.isAssignableFrom(clazz)) {

        return (Codec<T>) new AccessorCodec(clazz, registry);
      }

      // CodecProvider returns null if it's not a provider for the requested Class
      return null;
    }

    private class AccessorCodec<T extends Accessor> implements Codec<T> {
      final Class<T> clazz;
      final CodecRegistry registry;

      private AccessorCodec(Class<T> clazz, CodecRegistry registry) {
        this.clazz = clazz;
        this.registry = registry;
      }

      @SuppressWarnings({"unchecked"})
      @Override
      public T decode(BsonReader reader, DecoderContext decoderContext) {
        Map m = registry
            .get(Map.class)
            .decode(reader, decoderContext);

        ofNullable(m.remove("_id")).ifPresent(it -> m.put(DSL.ID, it));

        if (TaskExecution.class.isAssignableFrom(clazz)) {
          return clazz.cast(SimpleTaskExecution.createFromMap(m));
        }

        if (Job.class.isAssignableFrom(clazz)) {
          return clazz.cast(new SimpleJob(m));
        }

        if (Context.class.isAssignableFrom(clazz)) {
          return clazz.cast(new MapContext(m));
        }

        throw new IllegalArgumentException("Type not expected: " + clazz);
      }

      @Override
      public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        Map map = value.asMap()
            .entrySet()
            .stream()
            .collect(
                LinkedHashMap::new,
                (m, e) -> m.put(
                    // use the well-known field name for mongo primary key
                    e.getKey().equals(DSL.ID) ? "_id" : e.getKey(),
                    // use mongodb BSON Date where relevant
                    of(DSL.CREATE_TIME, DSL.START_TIME, DSL.END_TIME)
                        .filter(it -> it.equals(e.getKey()))
                        .filter(it -> nonNull(e.getValue()))
                        .map(it -> (Object) value.getDate(it))
                        .findFirst()
                        .orElse(e.getValue())),
                Map::putAll
            );

        registry
            .get(Map.class)
            .encode(writer, map, encoderContext);
      }

      @Override
      public Class<T> getEncoderClass() {
        return clazz;
      }
    }
  }

}
