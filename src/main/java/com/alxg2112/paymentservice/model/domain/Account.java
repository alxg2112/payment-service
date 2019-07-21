package com.alxg2112.paymentservice.model.domain;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class Account {

  private String accountId;
  private BigDecimal balance;
}
