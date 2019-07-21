package com.alxg2112.paymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.alxg2112.paymentservice.model.domain.Account;
import com.alxg2112.paymentservice.model.domain.Payment;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class SimpleInMemoryPaymentServiceTest {

  private static final PaymentServiceProperties PROPERTIES = PaymentServiceProperties.builder()
      .negativeBalanceAllowed(true) // Allow negative balance for test purposes
      .build();

  private static final String FIRST_ACCOUNT_ID = "b360b1c1-2db2-4712-8365-b7ebfd398282";
  private static final String SECOND_ACCOUNT_ID = "12cab280-b6ba-4159-8731-7d3a35318de9";
  private static final String THIRD_ACCOUNT_ID = "4510b789-145e-41a9-a433-408d212320e9";
  private static final String NOT_EXISTING_ACCOUNT_ID = "b4cf88dd-300c-4851-8caf-be9ec1aadae9";

  private static final List<String> ALL_ACCOUNT_IDS = List.of(
      FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID, THIRD_ACCOUNT_ID
  );

  private static final BigDecimal FIRST_ACCOUNT_BALANCE = BigDecimal.valueOf(100.00D);
  private static final BigDecimal SECOND_ACCOUNT_BALANCE = BigDecimal.valueOf(500.00D);
  private static final BigDecimal THIRD_ACCOUNT_BALANCE = BigDecimal.valueOf(1000.00D);

  // Invariant: account balances sum must be constant
  private static final BigDecimal ALL_ACCOUNT_BALANCES_INITIAL_SUM = FIRST_ACCOUNT_BALANCE
      .add(SECOND_ACCOUNT_BALANCE)
      .add(THIRD_ACCOUNT_BALANCE);

  private static final BigDecimal PAYMENT_AMOUNT = BigDecimal.ONE;
  private static final int NUMBER_OF_PAYMENTS = 10000;

  private PaymentService sut;

  @Before
  public void setUp() {
    sut = new SimpleInMemoryPaymentService(PROPERTIES, sampleAccounts());
  }

  @Test
  public void shouldProperlyProcessConcurrentPayments() {
    ExecutorService executorService = Executors
        .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    List<Future> futures = IntStream.range(0, NUMBER_OF_PAYMENTS)
        .mapToObj(num -> {
          List<String> accountIds = new ArrayList<>(ALL_ACCOUNT_IDS);
          Collections.shuffle(accountIds);
          String payerAccountId = accountIds.get(0);
          String payeeAccountId = accountIds.get(1);
          Payment payment = Payment.builder()
              .payerAccountId(payerAccountId)
              .payeeAccountId(payeeAccountId)
              .amount(PAYMENT_AMOUNT)
              .build();
          return executorService.submit(() -> {
            sut.processPayment(payment);
            return null;
          });
        }).collect(Collectors.toList());

    futures.forEach(future -> {
      try {
        future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    });

    executorService.shutdown();

    BigDecimal accountBalancesSum = sut.getAllAccounts()
        .stream()
        .map(Account::getBalance)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    assertThat(accountBalancesSum).isEqualTo(ALL_ACCOUNT_BALANCES_INITIAL_SUM);
  }

  @Test
  public void shouldThrowAccountNotFoundException() {
    Throwable thrown = catchThrowable(() -> sut.getAccount(NOT_EXISTING_ACCOUNT_ID));

    assertThat(thrown).isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  public void shouldThrowInsufficientFundsException() {
    // Prohibit negative balance just for this test case
    sut = new SimpleInMemoryPaymentService(
        PaymentServiceProperties.builder()
            .negativeBalanceAllowed(false)
            .build(), sampleAccounts());

    Throwable thrown = catchThrowable(() -> sut.processPayment(
        Payment.builder()
            .payerAccountId(FIRST_ACCOUNT_ID)
            .payeeAccountId(SECOND_ACCOUNT_ID)
            .amount(FIRST_ACCOUNT_BALANCE.add(BigDecimal.ONE))
            .build()
    ));

    assertThat(thrown).isInstanceOf(InsufficientFundsException.class);
  }

  @Test
  public void shouldThrowPaymentServiceExceptionOnSelfTransfer() {
    Throwable thrown = catchThrowable(() -> sut.processPayment(
        Payment.builder()
            .payerAccountId(FIRST_ACCOUNT_ID)
            .payeeAccountId(FIRST_ACCOUNT_ID)
            .amount(PAYMENT_AMOUNT)
            .build()
    ));

    assertThat(thrown).isInstanceOf(SelfTransferException.class);
  }

  @Test
  @UseDataProvider("accountNotFoundPaymentCases")
  public void shouldThrowAccountNotFoundException(Payment payment) {
    Throwable thrown = catchThrowable(() -> sut.processPayment(payment));

    assertThat(thrown).isInstanceOf(AccountNotFoundException.class);
  }

  @Test
  @UseDataProvider("invalidPaymentAmountCases")
  public void shouldThrowInvalidPaymentAmountException(Payment payment) {
    Throwable thrown = catchThrowable(() -> sut.processPayment(payment));

    assertThat(thrown).isInstanceOf(InvalidPaymentAmountException.class);
  }

  @DataProvider
  public static Object[][] accountNotFoundPaymentCases() {
    return new Object[][]{
        {
            Payment.builder()
                .payerAccountId(NOT_EXISTING_ACCOUNT_ID)
                .payeeAccountId(SECOND_ACCOUNT_ID)
                .amount(PAYMENT_AMOUNT)
                .build()},
        {
            Payment.builder()
                .payerAccountId(FIRST_ACCOUNT_ID)
                .payeeAccountId(NOT_EXISTING_ACCOUNT_ID)
                .amount(PAYMENT_AMOUNT)
                .build()
        },
    };
  }

  @DataProvider
  public static Object[][] invalidPaymentAmountCases() {
    return new Object[][]{
        {
            Payment.builder()
                .payerAccountId(FIRST_ACCOUNT_ID)
                .payeeAccountId(SECOND_ACCOUNT_ID)
                .amount(BigDecimal.valueOf(-1.00D))
                .build()},
        {
            Payment.builder()
                .payerAccountId(FIRST_ACCOUNT_ID)
                .payeeAccountId(SECOND_ACCOUNT_ID)
                .amount(BigDecimal.ZERO)
                .build()
        },
    };
  }

  private static List<Account> sampleAccounts() {
    return List.of(
        Account.builder()
            .accountId(FIRST_ACCOUNT_ID)
            .balance(FIRST_ACCOUNT_BALANCE)
            .build(),
        Account.builder()
            .accountId(SECOND_ACCOUNT_ID)
            .balance(SECOND_ACCOUNT_BALANCE)
            .build(),
        Account.builder()
            .accountId(THIRD_ACCOUNT_ID)
            .balance(THIRD_ACCOUNT_BALANCE)
            .build()
    );
  }
}