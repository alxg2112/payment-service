package com.alxg2112.paymentservice.controller;

import com.alxg2112.paymentservice.ServerProperties;
import com.alxg2112.paymentservice.model.domain.Account;
import com.alxg2112.paymentservice.model.domain.Payment;
import com.alxg2112.paymentservice.service.AccountNotFoundException;
import com.alxg2112.paymentservice.service.PaymentService;
import com.alxg2112.paymentservice.service.PaymentServiceException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ControllerVerticle extends AbstractVerticle {

  private static final String BASE_URL = "/api";

  private static final String PAYMENTS_ENDPOINT = "/payments";

  private static final String ACCOUNTS_ENDPOINT = "/accounts";
  private static final String ACCOUNT_ID_PARAM = "accountId";

  private final PaymentService paymentService;

  @Override
  public void start(Future<Void> startFuture) {
    Router router = Router.router(vertx);

    router.get(BASE_URL + ACCOUNTS_ENDPOINT).handler(routingContext -> {
      List<Account> allAccounts = paymentService.getAllAccounts();
      routingContext.response().setStatusCode(HttpResponseStatus.OK.code())
          .end(Json.encodePrettily(allAccounts));
    });

    router.get(BASE_URL + ACCOUNTS_ENDPOINT + "/:" + ACCOUNT_ID_PARAM)
        .handler(routingContext -> {
          String accountId = routingContext.request().getParam(ACCOUNT_ID_PARAM);
          try {
            Account account = paymentService.getAccount(accountId);
            routingContext.response().setStatusCode(HttpResponseStatus.OK.code())
                .end(Json.encodePrettily(account));
          } catch (AccountNotFoundException e) {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                .end(e.getMessage());
          }
        });

    router.route(BASE_URL + PAYMENTS_ENDPOINT + '*').handler(BodyHandler.create());
    router.post(BASE_URL + PAYMENTS_ENDPOINT).handler(routingContext -> {
      Payment payment = Json.decodeValue(routingContext.getBodyAsString(), Payment.class);
      try {
        paymentService.processPayment(payment);
        routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
      } catch (AccountNotFoundException e) {
        routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code())
            .end(e.getMessage());
      } catch (PaymentServiceException e) {
        routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
            .end(e.getMessage());
      }

    });

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(
            config().getInteger(ServerProperties.HTTP_PORT),
            result -> {
              if (result.succeeded()) {
                startFuture.complete();
              } else {
                startFuture.fail(result.cause());
              }
            }
        );
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }
}
