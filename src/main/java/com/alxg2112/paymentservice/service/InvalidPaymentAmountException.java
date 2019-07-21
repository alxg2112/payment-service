package com.alxg2112.paymentservice.service;

public class InvalidPaymentAmountException extends PaymentServiceException {

  public InvalidPaymentAmountException(String message) {
    super(message);
  }
}
