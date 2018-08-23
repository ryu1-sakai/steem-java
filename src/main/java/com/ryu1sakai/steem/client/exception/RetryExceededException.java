package com.ryu1sakai.steem.client.exception;

public class RetryExceededException extends RuntimeException {

  public RetryExceededException(String message, Throwable cause) {
    super(message, cause);
  }
}
