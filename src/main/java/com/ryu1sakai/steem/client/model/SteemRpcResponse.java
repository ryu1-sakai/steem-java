package com.ryu1sakai.steem.client.model;

import com.google.api.client.util.Key;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SteemRpcResponse {

  @Data
  @Accessors(chain = true)
  public static class ErrorData {
    @Key
    private String name;

    @Key
    private String exception;
  }

  @Data
  @Accessors(chain = true)
  public static class Error {
    @Key
    private Integer code;

    @Key
    private String message;

    @Key
    private ErrorData data;
  }

  @Key
  private Object result;

  @Key
  private Error error;
}
