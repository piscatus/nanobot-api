package com.nanobot.nanobotbackend.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

  private static final String MONGO_DATABASE_NAME = "data";

  @Bean
  public MongoClient mongoClient() {
    MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(
        new com.mongodb.ConnectionString(
          "mongodb://" +
          System.getenv("NODE1_NAME") +
          ":" +
          System.getenv("NODE1_PORT") +
          "," +
          System.getenv("NODE2_NAME") +
          ":" +
          System.getenv("NODE2_PORT") +
          "," +
          System.getenv("ARBITER_NAME") +
          ":" +
          System.getenv("ARBITER_PORT") +
          "/" +
          MONGO_DATABASE_NAME +
          "?replicaSet=" +
          System.getenv("REPL_SET_NAME")
        )
      )
      .writeConcern(WriteConcern.W2)
      .readConcern(ReadConcern.MAJORITY)
      .readPreference(ReadPreference.primaryPreferred())
      .build();

    return MongoClients.create(settings);
  }

  @Bean
  public MongoTemplate mongoTemplate(MongoClient mongoClient) {
    MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(
      mongoClient,
      MONGO_DATABASE_NAME
    );

    return new MongoTemplate(factory);
  }

  @SuppressWarnings("null")
  @Bean
  public MongoTransactionManager transactionManager(
    MongoDatabaseFactory dbFactory
  ) {
    return new MongoTransactionManager(dbFactory);
  }
}
