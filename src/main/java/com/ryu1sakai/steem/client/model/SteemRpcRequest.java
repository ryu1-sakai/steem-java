package com.ryu1sakai.steem.client.model;

import com.google.api.client.util.Key;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SteemRpcRequest {
  @Key
  private String jsonrpc;

  @Key
  private long id;

  @Key
  private String method;

  @Key
  private Object params;
}
