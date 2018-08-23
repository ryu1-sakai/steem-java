package com.ryu1sakai.steem.client;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.ryu1sakai.steem.client.exception.MalformedProtocolException;
import com.ryu1sakai.steem.client.exception.RecoverableRpcErrorException;
import com.ryu1sakai.steem.client.exception.RetryExceededException;
import com.ryu1sakai.steem.client.exception.SteemRpcErrorException;
import com.ryu1sakai.steem.client.model.SteemRpcRequest;
import com.ryu1sakai.steem.client.model.SteemRpcResponse;
import io.reactivex.Single;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteemHttpClient implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(SteemHttpClient.class);

  private final HttpTransport httpTransport;
  private final HttpRequestFactory httpRequestFactory;
  private final JsonFactory jsonFactory;
  private final Iterator<SteemNode> cyclicNodeIterator; // Not thread safe
  private final int maxTries;

  private SteemNode currentNode;

  public SteemHttpClient(HttpTransport httpTransport, JsonFactory jsonFactory,
                         List<SteemNode> nodes, int maxTries) {
    this.httpTransport = httpTransport;
    httpRequestFactory = httpTransport.createRequestFactory();
    this.jsonFactory = jsonFactory;
    cyclicNodeIterator = Iterators.cycle(nodes);
    currentNode = cyclicNodeIterator.next();
    this.maxTries = maxTries;
  }

  public Single<SteemCallResult> call(SteemCallParameter requestParam) {
    return call(requestParam, 0);
  }

  private Single<SteemCallResult> call(SteemCallParameter requestParam, int triedCount) {
    SteemNode targetNode = currentNode;
    return callOneNode(targetNode, requestParam)
            .onErrorResumeNext(error -> {
              if (!shouldTryAnotherNode(error)) {
                logger.warn("Unrecoverable error : param<{}> node<{}>",
                        requestParam, currentNode.getUrl(), error);
                return Single.error(error);
              }
              if (triedCount + 1 >= maxTries) {
                logger.warn("Try count exceeded : param<{}> node<{}>",
                        requestParam, currentNode.getUrl(), error);
                return Single.error(new RetryExceededException(
                        String.format("param<%s>", requestParam),
                        error));
              }
              rotateNode(targetNode);
              return call(requestParam, triedCount + 1);
            });
  }

  @Override
  public void close() throws IOException {
    httpTransport.shutdown();
  }

  @VisibleForTesting
  static boolean isRecoverableError(SteemRpcResponse.Error error) {
    int code = error.getCode();
    String errorMessage = error.getMessage();
    if (code == SteemRpcErrorCodes.JSON_RPC_ERROR_DURING_CALL
            && errorMessage.equals(SteemRpcErrorMessages.UNABLE_TO_LOCK_DATABASE)) {
      return true;
    }
    if (code == SteemRpcErrorCodes.JSON_RPC_SERVER_ERROR
            && errorMessage.equals(SteemRpcErrorMessages.UNKNOWN_EXCEPTION)) {
      return true;
    }
    if (code == SteemRpcErrorCodes.JSON_RPC_INTERNAL_ERROR
            && errorMessage.equals(SteemRpcErrorMessages.INTERNAL_ERROR)) {
      return true;
    }
    if (code == SteemRpcErrorCodes.JUSSI_UPSTREAM_RESPONSE_ERROR
            && errorMessage.equals(SteemRpcErrorMessages.UPSTREAM_RESPONSE_ERROR)) {
      return true;
    }
    return false;
  }

  private Single<SteemCallResult> callOneNode(SteemNode node, SteemCallParameter requestParam) {
    GenericUrl url = node.getUrl();
    boolean usingAppbaseApi = node.isAppbaseApiSupported();
    SteemRpcRequest request =
            usingAppbaseApi ? requestParam.forAppbaseApi() : requestParam.forCondenserApi();
    return callRpc(url, request)
            .flatMap(response -> {
              SteemRpcResponse.Error error = response.getError();
              if (error == null) {
                return Single.just(SteemCallResult.of(response.getResult()));
              }
              if (usingAppbaseApi && isErrorFromLegacyNode(error)) {
                logger.info("Stop using Appbase API to {}", url);
                node.setAppbaseApiSupported(false);
                if (requestParam.isApplicableToCondenserApi()) {
                  return callOneNode(node, requestParam);
                }
              }
              return Single.error(handleResponseError(response.getError(), node));
            });
  }

  private Single<SteemRpcResponse> callRpc(GenericUrl url, SteemRpcRequest request) {
    return Single
            .fromCallable(() -> {
              HttpContent content = new JsonHttpContent(jsonFactory, request);
              HttpRequest httpRequest = httpRequestFactory.buildPostRequest(url, content);
              return httpRequest.execute();
            })
            .flatMap(response -> {
              if (!isSuccessStatusCode(response.getStatusCode())) {
                String message = String.format("Non-success status %s <%s> from <%s>",
                        response.getStatusCode(), response.getStatusMessage(), url);
                logger.warn(message);
                return Single.error(new RecoverableRpcErrorException(message));
              }
              return Single.just(response.parseAs(SteemRpcResponse.class));
            });
  }

  private void rotateNode(SteemNode before) {
    synchronized (cyclicNodeIterator) {
      if (currentNode != before) {
        return; // Already rotated by another thread
      }
      currentNode = cyclicNodeIterator.next();
    }
  }

  private static boolean shouldTryAnotherNode(Throwable error) {
    if (error instanceof RecoverableRpcErrorException) {
      return true;
    }
    if (error instanceof MalformedProtocolException) {
      // The current node returned a wrong protocol, but another node may return correct one
      return true;
    }
    if (error instanceof IOException) {
      // The current server may have a lower-layer problem. Let's try the next node.
      return true;
    }
    return false;
  }

  private static boolean isErrorFromLegacyNode(SteemRpcResponse.Error error) {

    return error.getCode() == SteemRpcErrorCodes.JSON_RPC_LEGACY_NODE_ERROR;
  }

  private static Throwable handleResponseError(SteemRpcResponse.Error error, SteemNode node) {
    if (error.getCode() == null || error.getMessage() == null) {
      String message = String.format("Malformed error from node<%s> : %s", node.getUrl(), error);
      logger.warn(message);
      return new MalformedProtocolException(message);
    }
    if (isRecoverableError(error)) {
      String message = String.format("Recoverable error from node<%s> : %s", node.getUrl(), error);
      logger.info(message);
      return new RecoverableRpcErrorException(message);
    }
    String message = String.format("Unrecoverable error from node<%s> : %s", node.getUrl(), error);
    logger.warn(message);
    return new SteemRpcErrorException(message);
  }

  private static boolean isSuccessStatusCode(int statusCode) {
    return HttpStatusCodes.isRedirect(statusCode) || statusCode == HttpStatusCodes.STATUS_CODE_OK;
  }
}
