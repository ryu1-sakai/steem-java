package com.ryu1sakai.steem.client.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SteemRpcResponse {

  @Data
  @Accessors(chain = true)
  public static class ErrorData {
    private String name;

    private String exception;
  }

  @Data
  @Accessors(chain = true)
  public static class Error {
    private Integer code;

    private String message;

    private ErrorData data;
  }

  private String result;

  private Error error;
}
