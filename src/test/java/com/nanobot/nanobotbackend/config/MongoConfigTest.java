package com.nanobot.nanobotbackend.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@ExtendWith(MockitoExtension.class)
class MongoConfigTest {

  @Mock
  private MongoClient mongoClient;

  @Test
  void mongoTemplateBeanCreatesMongoTemplateFromClient() {
    MongoConfig config = new MongoConfig();
    MongoTemplate result = config.mongoTemplate(mongoClient);

    assertNotNull(result);
  }

  @Test
  void transactionManagerBeanCreatesManagerFromDatabaseFactory() {
    MongoConfig config = new MongoConfig();
    MongoDatabaseFactory factory =
      new SimpleMongoClientDatabaseFactory(mongoClient, "data");

    MongoTransactionManager result = config.transactionManager(factory);

    assertNotNull(result);
  }
}
