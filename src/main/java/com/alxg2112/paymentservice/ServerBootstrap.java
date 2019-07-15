package com.alxg2112.paymentservice;

import io.vertx.core.Vertx;

public class ServerBootstrap {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new PaymentServiceVerticle());
  }
}
