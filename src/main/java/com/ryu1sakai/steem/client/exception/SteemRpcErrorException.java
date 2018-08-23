package com.ryu1sakai.steem.client.exception;

public class SteemRpcErrorException extends RuntimeException {

  public SteemRpcErrorException(String message) {
    super(message);
  }
}
