package com.ryu1sakai.steem.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public final class SteemRpcErrorMessages {
  private SteemRpcErrorMessages() {}

  public static final String INTERNAL_ERROR = "Internal Error";

  public static final String INVALID_UPSTREAM_RESPONSE = "Bad or missing upstream response";

  public static final String UNABLE_TO_LOCK_DATABASE = "Unable to acquire database lock";

  public static final String UNKNOWN_EXCEPTION = "Unknown exception";

  public static final String UPSTREAM_RESPONSE_ERROR = "Upstream response error";

  @VisibleForTesting
  static Set<String> wellKnownMessages() {
    return ImmutableSet.of(INTERNAL_ERROR,
            INVALID_UPSTREAM_RESPONSE,
            UNABLE_TO_LOCK_DATABASE,
            UNKNOWN_EXCEPTION,
            UPSTREAM_RESPONSE_ERROR);
  }
}
