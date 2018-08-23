package com.ryu1sakai.steem.client;

import com.google.common.collect.ImmutableList;
import com.ryu1sakai.steem.client.model.SteemRpcRequest;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.Value;

@Value
public class SteemCallParameter {
  private static final String JSON_RPC = "2.0";

  private int id;
  private String api;
  private String method;
  private Map<String, Object> appbaseParams;
  @Nullable
  private List<Object> condenserParams;

  public static SteemCallParameter of(String api, String method, int id,
                                      @NonNull Map<String, Object> appbaseParams) {
    return new SteemCallParameter(id, api, method, appbaseParams, null);
  }

  public static SteemCallParameter of(String api, String method, int id,
                                      @NonNull Map<String, Object> appbaseParams,
                                      @NonNull List<Object> condenserParams) {
    return new SteemCallParameter(id, api, method, appbaseParams, condenserParams);
  }

  public static SteemCallParameter of(String api, String method,
                                      @NonNull Map<String, Object> appbaseParams) {
    return new SteemCallParameter(0, api, method, appbaseParams, null);
  }

  public static SteemCallParameter of(String api, String method,
                                      @NonNull Map<String, Object> appbaseParams,
                                      @NonNull List<Object> condenserParams) {
    return new SteemCallParameter(0, api, method, appbaseParams, condenserParams);
  }

  public boolean isApplicableToCondenserApi() {
    return condenserParams != null;
  }

  public SteemRpcRequest forAppbaseApi() {
    return new SteemRpcRequest()
        .setJsonrpc(JSON_RPC)
        .setId(id)
        .setMethod(api + '.' + method)
        .setParams(condenserParams);
  }

  public SteemRpcRequest forCondenserApi() {
    return new SteemRpcRequest()
        .setJsonrpc(JSON_RPC)
        .setId(id)
        .setMethod("condenser_api" + '.' + method)
        .setParams(condenserParams);
  }
}
