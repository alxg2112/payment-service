package com.alxg2112.paymentservice.service;

public class SelfTransferException extends PaymentServiceException {

  public SelfTransferException(String message) {
    super(message);
  }
}
