package com.alxg2112.paymentservice.service;

import com.alxg2112.paymentservice.model.domain.Account;
import com.alxg2112.paymentservice.model.domain.Payment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class SimpleInMemoryPaymentService implements PaymentService {

  private final PaymentServiceProperties properties;
  private final ConcurrentMap<String, ReadWriteLock> locksRegistry = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Account> accountsRegistry = new ConcurrentHashMap<>();

  public SimpleInMemoryPaymentService(List<Account> accounts) {
    this(PaymentServiceProperties.getDefault(), accounts);
  }

  public SimpleInMemoryPaymentService(PaymentServiceProperties properties, List<Account> accounts) {
    this.properties = properties;
    checkAccountsForDuplicateIds(accounts);
    accounts.forEach(
        account -> {
          accountsRegistry.put(account.getAccountId(), account);
          locksRegistry.put(account.getAccountId(), new ReentrantReadWriteLock());
        });
  }

  @Override
  public void processPayment(Payment payment) throws PaymentServiceException {
    String payerAccountId = payment.getPayerAccountId();
    String payeeAccountId = payment.getPayeeAccountId();
    BigDecimal amount = payment.getAmount();

    checkIfAccountExists(payerAccountId);
    checkIfSelfTransfer(payerAccountId, payeeAccountId);
    checkIfAccountExists(payeeAccountId);
    validateAmount(amount);

    // Deadlock avoidance. Lock accounts according to the natural order of their IDs.
    Lock firstLock = payerAccountId.compareTo(payeeAccountId) > 0
        ? locksRegistry.get(payeeAccountId).writeLock()
        : locksRegistry.get(payerAccountId).writeLock();
    Lock secondLock = payerAccountId.compareTo(payeeAccountId) > 0
        ? locksRegistry.get(payerAccountId).writeLock()
        : locksRegistry.get(payeeAccountId).writeLock();

    firstLock.lock();
    secondLock.lock();
    try {
      Account payerAccount = accountsRegistry.get(payerAccountId);
      Account payeeAccount = accountsRegistry.get(payeeAccountId);

      if (!properties.isNegativeBalanceAllowed()) {
        checkBalance(payerAccount, amount);
      }

      BigDecimal payerAccountBalance = payerAccount.getBalance();
      Account payerAccountUpdated = payerAccount
          .withBalance(payerAccountBalance.subtract(amount));
      BigDecimal payeeAccountBalance = payeeAccount.getBalance();
      Account payeeAccountUpdated = payeeAccount.withBalance(payeeAccountBalance.add(amount));

      accountsRegistry.put(payerAccountId, payerAccountUpdated);
      accountsRegistry.put(payeeAccountId, payeeAccountUpdated);
    } finally {
      secondLock.unlock();
      firstLock.unlock();
    }
  }

  @Override
  public Account getAccount(String accountId) throws AccountNotFoundException {
    checkIfAccountExists(accountId);
    Lock accountReadLock = locksRegistry.get(accountId).readLock();
    accountReadLock.lock();
    try {
      return accountsRegistry.get(accountId);
    } finally {
      accountReadLock.unlock();
    }
  }

  @Override
  public List<Account> getAllAccounts() {
    List<String> accountIds = new ArrayList<>(accountsRegistry.keySet());
    Collections.sort(accountIds);

    List<Lock> accountsReadLocksOrdered = accountIds.stream()
        .map(locksRegistry::get)
        .map(ReadWriteLock::readLock)
        .collect(Collectors.toList());
    accountsReadLocksOrdered.forEach(Lock::lock);
    try {
      return List.copyOf(accountsRegistry.values());
    } finally {
      Collections.reverse(accountsReadLocksOrdered);
      accountsReadLocksOrdered.forEach(Lock::unlock);
    }
  }

  private void checkAccountsForDuplicateIds(List<Account> accounts) {
    Set<String> uniqueAccountIds = accounts.stream()
        .map(Account::getAccountId)
        .collect(Collectors.toSet());

    if (uniqueAccountIds.size() < accounts.size()) {
      throw new IllegalArgumentException("Account IDs should be unique");
    }
  }

  private void checkIfSelfTransfer(String payerAccountId, String payeeAccountId)
      throws PaymentServiceException {
    if (payerAccountId.equals(payeeAccountId)) {
      throw new SelfTransferException("Cannot transfer funds to self");
    }
  }

  private void checkIfAccountExists(String accountId) throws AccountNotFoundException {
    if (!accountsRegistry.containsKey(accountId)) {
      throw new AccountNotFoundException(
          String.format("Account with ID '%s' is not found", accountId));
    }
  }

  private void checkBalance(Account account, BigDecimal amount) throws InsufficientFundsException {
    BigDecimal accountBalance = account.getBalance();
    String accountId = account.getAccountId();
    if (accountBalance.compareTo(amount) < 0) {
      throw new InsufficientFundsException(
          String.format("Insufficient funds on account '%s' to complete the operation, "
              + "needed '%s', but got '%s'", accountId, amount, accountBalance)
      );
    }
  }

  private void validateAmount(BigDecimal amount) throws PaymentServiceException {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidPaymentAmountException("Transferred amount must be positive decimal");
    }
  }
}
