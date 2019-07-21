package com.alxg2112.paymentservice;

import com.alxg2112.paymentservice.controller.ControllerVerticle;
import com.alxg2112.paymentservice.model.domain.Account;
import com.alxg2112.paymentservice.service.PaymentService;
import com.alxg2112.paymentservice.service.SimpleInMemoryPaymentService;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;

public class ServerBootstrap {

  private static final String CONFIG_DIRECTORY = "config";
  private static final String SERVER_CONFIG_FILE = "config.json";
  private static final String ACCOUNTS_DATA_FILE = "accounts-data.json";

  public static void main(String[] args) throws Exception {
    JsonObject serverConfig = new JsonObject(getConfigFileContent(SERVER_CONFIG_FILE));

    String accountsDataRaw = getConfigFileContent(ACCOUNTS_DATA_FILE);
    List<Account> accounts = Arrays
        .asList(Json.decodeValue(accountsDataRaw, Account[].class));
    PaymentService paymentService = new SimpleInMemoryPaymentService(accounts);

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new ControllerVerticle(paymentService),
        new DeploymentOptions().setConfig(serverConfig));
  }

  private static String getConfigFileContent(String configFile)
      throws IOException {
    Path configOverridePath =
        Paths.get(CONFIG_DIRECTORY + File.separator + configFile);
    if (Files.exists(configOverridePath)) {
      return Files.readString(configOverridePath);
    }
    return IOUtils.toString(ServerBootstrap.class
            .getClassLoader()
            .getResourceAsStream(configFile),
        StandardCharsets.UTF_8);
  }
}
