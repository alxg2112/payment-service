package com.alxg2112.paymentservice.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentServiceProperties {

  private boolean negativeBalanceAllowed;

  public static PaymentServiceProperties getDefault() {
    return PaymentServiceProperties.builder().negativeBalanceAllowed(false).build();
  }
}
