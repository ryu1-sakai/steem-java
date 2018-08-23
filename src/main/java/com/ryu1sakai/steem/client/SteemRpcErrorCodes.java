package com.ryu1sakai.steem.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * See <a href="https://github.com/steemit/steem/blob/master/libraries/plugins/json_rpc/include/steem/plugins/json_rpc/json_rpc_plugin.hpp">
 *   json_rpc_plugin.hpp</a> in <a href="https://github.com/steemit/steem">steemit/steem</a> and
 *   <a href="https://github.com/steemit/jussi/blob/master/jussi/errors.py">errors.py</a> in
 *   <a href="https://github.com/steemit/jussi">steemit/jussi</a>.
 */
public final class SteemRpcErrorCodes {
  private SteemRpcErrorCodes() {}

  public static final int JSON_RPC_SERVER_ERROR       = -32000;
  public static final int JSON_RPC_NO_PARAMS          = -32001;
  public static final int JSON_RPC_PARSE_PARAMS_ERROR = -32002;
  public static final int JSON_RPC_ERROR_DURING_CALL  = -32003;

  public static final int JSON_RPC_INVALID_REQUEST    = -32600;
  public static final int JSON_RPC_METHOD_NOT_FOUND   = -32601;
  public static final int JSON_RPC_INVALID_PARAMS     = -32602;
  public static final int JSON_RPC_INTERNAL_ERROR     = -32603;

  public static final int JSON_RPC_PARSE_ERROR        = -32700;

  public static final int JUSSI_UPSTREAM_RESPONSE_ERROR = 1100;

  // Legacy (pre-appbase) nodes always return error code 1
  public static final int JSON_RPC_LEGACY_NODE_ERROR = 1;

  @VisibleForTesting
  static Set<Integer> wellKnownCodes() {
    return ImmutableSet.of(JSON_RPC_SERVER_ERROR,
            JSON_RPC_NO_PARAMS,
            JSON_RPC_PARSE_PARAMS_ERROR,
            JSON_RPC_ERROR_DURING_CALL,
            JSON_RPC_INVALID_REQUEST,
            JSON_RPC_METHOD_NOT_FOUND,
            JSON_RPC_INVALID_PARAMS,
            JSON_RPC_INTERNAL_ERROR,
            JSON_RPC_PARSE_ERROR,
            JUSSI_UPSTREAM_RESPONSE_ERROR,
            JSON_RPC_LEGACY_NODE_ERROR);
  }
}
