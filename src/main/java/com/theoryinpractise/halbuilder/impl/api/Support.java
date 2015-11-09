package com.theoryinpractise.halbuilder.impl.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import javaslang.collection.HashSet;
import javaslang.collection.Set;

public class Support {

  public static final Splitter WHITESPACE_SPLITTER = Splitter.onPattern("\\s").omitEmptyStrings();
  public static final String REL = "rel";
  public static final String SELF = "self";
  public static final String LINK = "link";
  public static final String HREF = "href";
  public static final String LINKS = "_links";
  public static final String EMBEDDED = "_embedded";
  public static final String NAME = "name";
  public static final String CURIES = "curies";
  public static final String TITLE = "title";
  public static final String HREFLANG = "hreflang";
  public static final String TEMPLATED = "templated";
  public static final Set<String> RESERVED_JSON_PROPERTIES = HashSet.ofAll(EMBEDDED, LINKS);

  public static final String PROFILE = "profile";

  public static void checkRelType(String rel) {
    Preconditions.checkArgument(rel != null, "Provided rel should not be null.");
    Preconditions.checkArgument(!"".equals(rel) && !rel.contains(" "),
        "Provided rel value should be a single rel type, as "
        + "defined by http://tools.ietf.org/html/rfc5988");
  }
}
