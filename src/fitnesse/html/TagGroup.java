// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.html;

import java.util.Iterator;

public class TagGroup extends HtmlTag {
  public TagGroup() {
    super("group");
  }

  public String html(int depth) throws Exception {
    StringBuffer buffer = new StringBuffer();
    for (Iterator<HtmlElement> iterator = childTags.iterator(); iterator.hasNext();) {
      HtmlElement element = iterator.next();
      if (element instanceof HtmlTag)
        buffer.append(((HtmlTag) element).html(depth));
      else
        buffer.append(element.html());
    }
    return buffer.toString();
  }
}
