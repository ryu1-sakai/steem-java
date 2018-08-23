package com.ryu1sakai.steem.client.model;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SteemRpcRequest {
  private String jsonrpc;

  private long id;

  private String method;

  private List<?> params;
}
