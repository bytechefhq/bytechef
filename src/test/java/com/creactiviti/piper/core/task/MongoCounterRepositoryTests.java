package com.creactiviti.piper.core.task;

import com.creactiviti.piper.config.MongoPersistenceConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static com.creactiviti.piper.core.task.MongoCounterRepositoryTests.MongoCounterRepositoryTestsContextConfiguration;
import static com.creactiviti.piper.core.task.MongoCounterRepositoryTests.MongodbInitailizer;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static de.flapdoodle.embed.process.runtime.Network.localhostIsIPv6;
import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    MongoPersistenceConfiguration.class,
    MongoCounterRepositoryTestsContextConfiguration.class},
    properties = {"piper.persistence.provider=mongo"}
)
@ContextConfiguration(initializers = MongodbInitailizer.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
public class MongoCounterRepositoryTests {
  @TestConfiguration
  static class MongoCounterRepositoryTestsContextConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper()
          .configure(WRITE_DATES_AS_TIMESTAMPS, false)
          .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ"));
    }
  }

  public static class MongodbInitailizer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      int randomPort = SocketUtils.findAvailableTcpPort();
      addInlinedPropertiesToEnvironment(applicationContext, format("spring.datasource.url=mongodb://{0}:{1,number,0}/piper", "localhost", randomPort));

      try {
        MongodExecutable mongodExe = starter
            .prepare(new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(randomPort, localhostIsIPv6()))
                .build());

        MongodProcess mongod = mongodExe.start();

        applicationContext.addApplicationListener(e -> {
          if (e instanceof ContextClosedEvent) {
            mongod.stop();
            mongodExe.stop();
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private static final MongodStarter starter = MongodStarter.getDefaultInstance();
  private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

  @Autowired
  MongoCounterRepository counterRepository;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test1() {
    counterRepository.set("my-counter-1", 1);
    counterRepository.set("my-counter-2", 2);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Counter not found: my-counter-3");

    counterRepository.decrement("my-counter-3");
  }

  @Test
  public void test2() {
    counterRepository.set("my-counter-1", 1);
    counterRepository.set("my-counter-2", 2);
    counterRepository.set("my-counter-3", 3);

    long v = counterRepository.decrement("my-counter-2");

    assertEquals(1, v);
  }

  @Test
  public void test3() {
    counterRepository.set("my-counter-1", 1);
    counterRepository.set("my-counter-2", 2);

    counterRepository.delete("my-counter-1");

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Counter not found: my-counter-1");

    counterRepository.decrement("my-counter-1");
  }

}
