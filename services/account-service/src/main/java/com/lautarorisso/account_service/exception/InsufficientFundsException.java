package com.lautarorisso.account_service.exception;

public class InsufficientFundsException extends RuntimeException {
  public InsufficientFundsException() {
    super("Insufficient funds");
  }
}
