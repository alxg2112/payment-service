package com.alxg2112.paymentservice.service;

public class AccountNotFoundException extends PaymentServiceException {

  public AccountNotFoundException(String message) {
    super(message);
  }
}
