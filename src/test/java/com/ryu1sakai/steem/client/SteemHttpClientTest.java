package com.ryu1sakai.steem.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.testing.json.MockJsonFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ryu1sakai.steem.client.exception.RetryExceededException;
import com.ryu1sakai.steem.client.exception.SteemRpcErrorException;
import com.ryu1sakai.steem.client.model.SteemRpcRequest;
import com.ryu1sakai.steem.client.model.SteemRpcResponse;
import io.reactivex.Single;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(Theories.class)
public class SteemHttpClientTest {

  private static final JsonFactory JSON_FACTORY = new MockJsonFactory();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule();

  @Mock
  private HttpTransport httpTransport;

  @Mock
  private HttpRequestFactory httpRequestFactory;

  @Mock
  private HttpRequest httpRequest;

  @Before
  public void setUp() throws Exception {
    given(httpTransport.createRequestFactory()).willReturn(httpRequestFactory);
    given(httpRequestFactory.buildPostRequest(any(GenericUrl.class), any(HttpContent.class)))
        .willReturn(httpRequest);
  }

  @Theory
  public void call(boolean isAppbaseApi) throws Exception {
    // set up
    GenericUrl url = new GenericUrl("https://example.net/example");
    SteemNode node = new SteemNode().setUrl(url).setAppbaseApiSupported(isAppbaseApi);

    String expectedResult = RandomStringUtils.randomAlphabetic(8);
    SteemRpcResponse response = new SteemRpcResponse().setResult(expectedResult);

    HttpResponse httpResponse = mock(HttpResponse.class);
    given(httpRequest.execute()).willReturn(httpResponse);
    given(httpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_OK);
    given(httpResponse.parseAs(any())).willReturn(response);

    SteemHttpClient sut
        = new SteemHttpClient(httpTransport, JSON_FACTORY, ImmutableList.of(node), 1);

    // execute
    String api = RandomStringUtils.randomAlphabetic(8);
    String method = RandomStringUtils.randomAlphabetic(8);
    int id = RandomUtils.nextInt();
    Map<String, Object> appbaseParams = ImmutableMap.of(
            RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8));
    List<Object> condenserParams = ImmutableList.of(RandomStringUtils.randomAlphabetic(8));
    SteemCallParameter parameter
            = SteemCallParameter.of(api, method, id, appbaseParams, condenserParams);

    Single<SteemCallResult> actual = sut.call(parameter);

    // verify
    actual.test().assertResult(SteemCallResult.of(expectedResult));

    ArgumentCaptor<HttpContent> contentCaptor = ArgumentCaptor.forClass(HttpContent.class);
    then(httpRequestFactory).should().buildPostRequest(eq(url), contentCaptor.capture());
    SteemRpcRequest rpcRequest
            = isAppbaseApi ? parameter.forAppbaseApi() : parameter.forCondenserApi();
    verifyJsonHttpContent(contentCaptor.getValue(), new JsonHttpContent(JSON_FACTORY, rpcRequest));
  }

  @Test
  public void call_switchToCondenserApi() throws Exception {
    // set up
    GenericUrl url = new GenericUrl("https://example.net/example");
    SteemNode node = new SteemNode().setUrl(url).setAppbaseApiSupported(true);

    SteemRpcResponse.Error legacyError
        = new SteemRpcResponse.Error().setCode(SteemRpcErrorCodes.JSON_RPC_LEGACY_NODE_ERROR);
    SteemRpcResponse response1 = new SteemRpcResponse().setError(legacyError);
    String expectedResult = RandomStringUtils.randomAlphabetic(8);
    SteemRpcResponse response2 = new SteemRpcResponse().setResult(expectedResult);

    HttpResponse httpResponse = mock(HttpResponse.class);
    given(httpRequest.execute()).willReturn(httpResponse);
    given(httpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_OK);
    given(httpResponse.parseAs(any())).willReturn(response1, response2);

    SteemHttpClient sut
        = new SteemHttpClient(httpTransport, JSON_FACTORY, ImmutableList.of(node), 1);

    // execute
    String api = RandomStringUtils.randomAlphabetic(8);
    String method = RandomStringUtils.randomAlphabetic(8);
    int id = RandomUtils.nextInt();
    Map<String, Object> appbaseParams = ImmutableMap.of(
            RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8));
    List<Object> condenserParams = ImmutableList.of(RandomStringUtils.randomAlphabetic(8));
    SteemCallParameter parameter
            = SteemCallParameter.of(api, method, id, appbaseParams, condenserParams);

    Single<SteemCallResult> actual = sut.call(parameter);

    // verify
    actual.test().assertResult(SteemCallResult.of(expectedResult));

    ArgumentCaptor<HttpContent> contentCaptor = ArgumentCaptor.forClass(HttpContent.class);
    then(httpRequestFactory).should(times(2)).buildPostRequest(eq(url), contentCaptor.capture());
    List<HttpContent> httpContents = contentCaptor.getAllValues();
    verifyJsonHttpContent(
        httpContents.get(0), new JsonHttpContent(JSON_FACTORY, parameter.forCondenserApi()));
    verifyJsonHttpContent(
        httpContents.get(1), new JsonHttpContent(JSON_FACTORY, parameter.forAppbaseApi()));
  }

  @DataPoints("HTTP Status Code")
  public static final List<Integer> HTTP_STATUS_CODES
          = IntStream.range(200, 600).boxed().collect(ImmutableList.toImmutableList());

  @Theory
  public void call_recoveryFromHttpError(@FromDataPoints("HTTP Status Code") int errorStatusCode)
          throws Exception {
    assumeThat(errorStatusCode).isNotEqualTo(HttpStatusCodes.STATUS_CODE_OK)
            .matches(code -> !HttpStatusCodes.isRedirect(code));

    // set up
    GenericUrl failingUrl = new GenericUrl("https://failure.example.net/example");
    SteemNode failingNode = new SteemNode().setUrl(failingUrl).setAppbaseApiSupported(true);
    GenericUrl successiveUrl = new GenericUrl("https://success.example.net/example");
    SteemNode successiveNode = new SteemNode().setUrl(successiveUrl).setAppbaseApiSupported(true);

    HttpResponse errorHttpResponse = mock(HttpResponse.class);
    given(errorHttpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_NOT_FOUND);
    HttpResponse successHttpResponse = mock(HttpResponse.class);
    given(successHttpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_OK);

    given(httpRequest.execute()).willReturn(errorHttpResponse, successHttpResponse);

    String expectedResult = RandomStringUtils.randomAlphabetic(8);
    SteemRpcResponse finalResponse = new SteemRpcResponse().setResult(expectedResult);
    given(successHttpResponse.parseAs(any())).willReturn(finalResponse);

    SteemHttpClient sut = new SteemHttpClient(
            httpTransport, JSON_FACTORY, ImmutableList.of(failingNode, successiveNode), 2);

    // execute
    String api = RandomStringUtils.randomAlphabetic(8);
    String method = RandomStringUtils.randomAlphabetic(8);
    int id = RandomUtils.nextInt();
    Map<String, Object> appbaseParams = ImmutableMap.of(
            RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8));
    List<Object> condenserParams = ImmutableList.of(RandomStringUtils.randomAlphabetic(8));
    SteemCallParameter parameter
            = SteemCallParameter.of(api, method, id, appbaseParams, condenserParams);

    Single<SteemCallResult> actual = sut.call(parameter);

    // verify
    actual.test().assertResult(SteemCallResult.of(expectedResult));

    then(httpRequestFactory).should().buildPostRequest(eq(successiveUrl), any(HttpContent.class));
    then(httpRequestFactory).should().buildPostRequest(eq(failingUrl), any(HttpContent.class));
  }

  @DataPoints("Steem RPC Error Code")
  public static final Set<Integer> RPC_ERROR_CODES = SteemRpcErrorCodes.wellKnownCodes();

  @DataPoints("Steem RPC Error Message")
  public static final Set<String> RPC_ERROR_MESSAGES = SteemRpcErrorMessages.wellKnownMessages();

  @Theory
  public void call_rpcError_recover(@FromDataPoints("Steem RPC Error Code") int errorCode,
                                    @FromDataPoints("Steem RPC Error Message") String errorMessage)
          throws Exception {
    SteemRpcResponse.Error error
            = new SteemRpcResponse.Error().setCode(errorCode).setMessage(errorMessage);
    assumeThat(error).matches(SteemHttpClient::isRecoverableError);

    // set up
    GenericUrl failingUrl = new GenericUrl("https://failure.example.net/example");
    SteemNode failingNode = new SteemNode().setUrl(failingUrl).setAppbaseApiSupported(true);
    GenericUrl nextUrl = new GenericUrl("https://success.example.net/example");
    SteemNode nextNode = new SteemNode().setUrl(nextUrl).setAppbaseApiSupported(true);

    HttpResponse httpResponse = mock(HttpResponse.class);
    given(httpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_OK);
    given(httpRequest.execute()).willReturn(httpResponse);

    SteemRpcResponse errorResponse = new SteemRpcResponse().setError(error);
    String expectedResult = RandomStringUtils.randomAlphabetic(8);
    SteemRpcResponse successResponse = new SteemRpcResponse().setResult(expectedResult);
    given(httpResponse.parseAs(any())).willReturn(errorResponse, successResponse);

    SteemHttpClient sut = new SteemHttpClient(
            httpTransport, JSON_FACTORY, ImmutableList.of(failingNode, nextNode), 2);

    // execute
    String api = RandomStringUtils.randomAlphabetic(8);
    String method = RandomStringUtils.randomAlphabetic(8);
    int id = RandomUtils.nextInt();
    Map<String, Object> appbaseParams = ImmutableMap.of(
            RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8));
    List<Object> condenserParams = ImmutableList.of(RandomStringUtils.randomAlphabetic(8));
    SteemCallParameter parameter
            = SteemCallParameter.of(api, method, id, appbaseParams, condenserParams);

    Single<SteemCallResult> actual = sut.call(parameter);

    // verify
    actual.test().assertResult(SteemCallResult.of(expectedResult));

    then(httpRequestFactory).should().buildPostRequest(eq(failingUrl), any(HttpContent.class));
    then(httpRequestFactory).should().buildPostRequest(eq(nextUrl), any(HttpContent.class));
  }

  @Theory
  public void call_rpcError_cannotRecover(
          @FromDataPoints("Steem RPC Error Code") int errorCode,
          @FromDataPoints("Steem RPC Error Message") String errorMessage) throws Exception {
    assumeThat(errorCode).isNotEqualTo(SteemRpcErrorCodes.JSON_RPC_LEGACY_NODE_ERROR);
    SteemRpcResponse.Error error
            = new SteemRpcResponse.Error().setCode(errorCode).setMessage(errorMessage);
    assumeThat(error).matches(e -> !SteemHttpClient.isRecoverableError(e));

    // set up
    GenericUrl failingUrl = new GenericUrl("https://example.net/example");
    SteemNode failingNode = new SteemNode().setUrl(failingUrl).setAppbaseApiSupported(true);
    GenericUrl successiveUrl = new GenericUrl("https://success.example.net/example");
    SteemNode successiveNode = new SteemNode().setUrl(successiveUrl).setAppbaseApiSupported(true);

    HttpResponse httpResponse = mock(HttpResponse.class);
    given(httpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_OK);

    given(httpRequest.execute()).willReturn(httpResponse);

    SteemRpcResponse errorResponse = new SteemRpcResponse().setError(error);
    String expectedResult = RandomStringUtils.randomAlphabetic(8);
    SteemRpcResponse successResponse = new SteemRpcResponse().setResult(expectedResult);
    given(httpResponse.parseAs(any())).willReturn(errorResponse, successResponse);

    SteemHttpClient sut = new SteemHttpClient(
            httpTransport, JSON_FACTORY, ImmutableList.of(failingNode, successiveNode), 2);

    // execute
    String api = RandomStringUtils.randomAlphabetic(8);
    String method = RandomStringUtils.randomAlphabetic(8);
    int id = RandomUtils.nextInt();
    Map<String, Object> appbaseParams = ImmutableMap.of(
            RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8));
    List<Object> condenserParams = ImmutableList.of(RandomStringUtils.randomAlphabetic(8));
    SteemCallParameter parameter
            = SteemCallParameter.of(api, method, id, appbaseParams, condenserParams);

    Single<SteemCallResult> actual = sut.call(parameter);

    // verify
    actual.test().assertError(SteemRpcErrorException.class);

    then(httpRequestFactory).should().buildPostRequest(eq(failingUrl), any(HttpContent.class));
    then(httpRequestFactory)
            .should(never()).buildPostRequest(eq(successiveUrl), any(HttpContent.class));
  }

  @Test
  public void call_recoverFromMultipleFailure() throws Exception {
    call_recoverFromMultipleFailure(1);
    call_recoverFromMultipleFailure(2);
    call_recoverFromMultipleFailure(4);
    call_recoverFromMultipleFailure(8);
  }

  private void call_recoverFromMultipleFailure(int failureCount) throws Exception {
    assumeThat(failureCount).isGreaterThanOrEqualTo(1);

    // set up
    List<SteemNode> steemNodes = Stream.generate(() -> RandomStringUtils.randomAlphanumeric(8))
            .distinct().limit(failureCount + 1)
            .map(host -> new GenericUrl(String.format("https://%s.example.com/example", host)))
            .map(url -> new SteemNode().setUrl(url).setAppbaseApiSupported(true))
            .collect(ImmutableList.toImmutableList());

    HttpResponse errorHttpResponse = mock(HttpResponse.class);
    given(errorHttpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_NOT_FOUND);
    HttpResponse successHttpResponse = mock(HttpResponse.class);
    given(successHttpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_OK);

    HttpResponse[] tail = new HttpResponse[steemNodes.size() - 1];
    Arrays.fill(tail, errorHttpResponse);
    tail[tail.length - 1] = successHttpResponse; // Only the last is a success response
    given(httpRequest.execute()).willReturn(errorHttpResponse, tail);

    String expectedResult = RandomStringUtils.randomAlphabetic(8);
    SteemRpcResponse finalResponse = new SteemRpcResponse().setResult(expectedResult);
    given(successHttpResponse.parseAs(any())).willReturn(finalResponse);

    SteemHttpClient sut
            = new SteemHttpClient(httpTransport, JSON_FACTORY, steemNodes, failureCount + 1);

    // execute
    String api = RandomStringUtils.randomAlphabetic(8);
    String method = RandomStringUtils.randomAlphabetic(8);
    int id = RandomUtils.nextInt();
    Map<String, Object> appbaseParams = ImmutableMap.of(
            RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8));
    List<Object> condenserParams = ImmutableList.of(RandomStringUtils.randomAlphabetic(8));
    SteemCallParameter parameter
            = SteemCallParameter.of(api, method, id, appbaseParams, condenserParams);

    Single<SteemCallResult> actual = sut.call(parameter);

    // verify
    actual.test().assertResult(SteemCallResult.of(expectedResult));

    for (SteemNode steemNode : steemNodes) {
      then(httpRequestFactory)
              .should().buildPostRequest(eq(steemNode.getUrl()), any(HttpContent.class));
    }
  }

  @Test
  public void call_recoveryCountExceeded() throws Exception {
    call_recoveryCountExceeded(1);
    call_recoveryCountExceeded(2);
    call_recoveryCountExceeded(4);
    call_recoveryCountExceeded(8);
  }

  private void call_recoveryCountExceeded(int maxTries) throws Exception {
    assumeThat(maxTries).isGreaterThanOrEqualTo(1);

    // set up
    List<SteemNode> steemNodes = Stream.generate(() -> RandomStringUtils.randomAlphanumeric(8))
            .distinct().limit(maxTries + 1)
            .map(host -> new GenericUrl(String.format("https://%s.example.com/example", host)))
            .map(url -> new SteemNode().setUrl(url).setAppbaseApiSupported(true))
            .collect(ImmutableList.toImmutableList());

    HttpResponse errorHttpResponse = mock(HttpResponse.class);
    given(errorHttpResponse.getStatusCode()).willReturn(HttpStatusCodes.STATUS_CODE_NOT_FOUND);

    given(httpRequest.execute()).willReturn(errorHttpResponse);

    SteemHttpClient sut
            = new SteemHttpClient(httpTransport, JSON_FACTORY, steemNodes, maxTries);

    // execute
    String api = RandomStringUtils.randomAlphabetic(8);
    String method = RandomStringUtils.randomAlphabetic(8);
    int id = RandomUtils.nextInt();
    Map<String, Object> appbaseParams = ImmutableMap.of(
            RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8));
    List<Object> condenserParams = ImmutableList.of(RandomStringUtils.randomAlphabetic(8));
    SteemCallParameter parameter
            = SteemCallParameter.of(api, method, id, appbaseParams, condenserParams);

    Single<SteemCallResult> actual = sut.call(parameter);

    // verify
    actual.test().assertError(RetryExceededException.class);

    for (SteemNode steemNode : steemNodes.subList(0, maxTries)) {
      then(httpRequestFactory)
              .should().buildPostRequest(eq(steemNode.getUrl()), any(HttpContent.class));
    }
    SteemNode remainingNode = steemNodes.get(maxTries);
    then(httpRequestFactory)
            .should(never()).buildPostRequest(eq(remainingNode.getUrl()), any(HttpContent.class));
  }

  private static void verifyJsonHttpContent(HttpContent actual, JsonHttpContent expected) {
    assertThat(actual).isInstanceOf(JsonHttpContent.class);
    String actualString = toString((JsonHttpContent) actual);
    String expectedString = toString(expected);
    assertThat(actualString).isEqualTo(expectedString);
  }

  private static String toString(JsonHttpContent jsonHttpContent) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      jsonHttpContent.writeTo(outputStream);
      return outputStream.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
