package com.ryu1sakai.steem.client;

import lombok.Value;

@Value(staticConstructor = "of")
public class SteemCallResult {
  private Object result;
}
