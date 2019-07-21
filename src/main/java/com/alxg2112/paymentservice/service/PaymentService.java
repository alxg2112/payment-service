package com.alxg2112.paymentservice.service;

import com.alxg2112.paymentservice.model.domain.Account;
import com.alxg2112.paymentservice.model.domain.Payment;
import java.util.List;

public interface PaymentService {

  void processPayment(Payment payment) throws PaymentServiceException;

  Account getAccount(String accountId) throws AccountNotFoundException;

  List<Account> getAllAccounts();
}
