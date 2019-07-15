package com.alxg2112.paymentservice.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account {

  private Long id;
  private AccountOwner accountOwner;
  private String accountName;
  private BigDecimal balance;
}
