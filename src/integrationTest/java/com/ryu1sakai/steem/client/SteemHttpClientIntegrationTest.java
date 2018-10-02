package com.ryu1sakai.steem.client;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ryu1sakai.steem.client.model.SteemRpcRequest;
import com.ryu1sakai.steem.client.model.SteemRpcResponse;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SteemHttpClientIntegrationTest {

  private static final String OFFICIAL_NODE_URL = "https://api.steemit.com/";

  @Test
  public void callRpc_appbaseApi() {
    // setup
    GenericUrl url = new GenericUrl(OFFICIAL_NODE_URL);
    SteemNode steemNode = new SteemNode().setUrl(url).setAppbaseApiSupported(true);
    SteemHttpClient sut = new SteemHttpClient(
            new ApacheHttpTransport(), new JacksonFactory(), ImmutableList.of(steemNode), 1);

    // exercise
    int limit = 10;
    SteemRpcRequest request = new SteemRpcRequest()
            .setJsonrpc("2.0")
            .setMethod("market_history_api.get_recent_trades")
            .setParams(ImmutableMap.of("limit", limit))
            .setId(1);
    Single<SteemRpcResponse> actual = sut.callRpc(url, request);

    // then
    actual.test().assertValue(response -> response.getError() == null)
            .assertValue(response -> {
              Object resultObject = response.getResult();
              if (!(resultObject instanceof Map)) {
                return false;
              }
              Map<?, ?> result = (Map<?, ?>) resultObject;
              Object tradesObject = result.get("trades");
              if (!(tradesObject instanceof List)) {
                return false;
              }
              List<?> trades = (List<?>) tradesObject;
              return trades.size() == limit;
            });
  }

  @Test
  public void callRpc_condenserApi() {
    // setup
    GenericUrl url = new GenericUrl(OFFICIAL_NODE_URL);
    SteemNode steemNode = new SteemNode().setUrl(url).setAppbaseApiSupported(true);
    SteemHttpClient sut = new SteemHttpClient(
            new ApacheHttpTransport(), new JacksonFactory(), ImmutableList.of(steemNode), 1);

    // exerciseint
    int limit = 10;
    SteemRpcRequest request = new SteemRpcRequest()
            .setJsonrpc("2.0")
            .setMethod("condenser_api.get_recent_trades")
            .setParams(ImmutableList.of(limit))
            .setId(1);
    Single<SteemRpcResponse> actual = sut.callRpc(url, request);

    // then
    actual.test().assertValue(response -> response.getError() == null)
            .assertValue(response -> {
                Object resultObject = response.getResult();
                if (!(resultObject instanceof List)) {
                    return false;
                }
                List<?> result = (List<?>) resultObject;
                return result.size() == limit;
            });
  }
}
