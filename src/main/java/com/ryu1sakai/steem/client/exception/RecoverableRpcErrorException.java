package com.ryu1sakai.steem.client.exception;

public class RecoverableRpcErrorException extends RuntimeException {

  public RecoverableRpcErrorException(String message) {
    super(message);
  }
}
