package com.ryu1sakai.steem.client;

import com.google.api.client.http.GenericUrl;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SteemNode {
  private GenericUrl url;

  private boolean appbaseApiSupported;
}
