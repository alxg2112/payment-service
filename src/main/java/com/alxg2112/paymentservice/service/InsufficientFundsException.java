package com.alxg2112.paymentservice.service;

public class InsufficientFundsException extends PaymentServiceException {

  public InsufficientFundsException(String message) {
    super(message);
  }
}
