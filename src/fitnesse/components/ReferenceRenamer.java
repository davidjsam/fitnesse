// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.components;

import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import fitnesse.wikitext.WidgetBuilder;
import fitnesse.wikitext.WidgetVisitor;
import fitnesse.wikitext.widgets.*;

public abstract class ReferenceRenamer implements FitNesseTraversalListener {
  protected WikiPage root;

  public ReferenceRenamer(WikiPage root) {
    this.root = root;
  }

  protected void renameReferences() throws Exception {
    root.getPageCrawler().traverse(root, this);
  }

  public void processPage(WikiPage currentPage) throws Exception {
    PageData data = currentPage.getData();
    String content = data.getContent();
    ParentWidget widgetRoot = new WidgetRoot(content, currentPage, referenceModifyingWidgetBuilder);
    widgetRoot.acceptVisitor(getVisitor());

    String newContent = widgetRoot.asWikiText();
    boolean pageHasChanged = !newContent.equals(content);
    if (pageHasChanged) {
      data.setContent(newContent);
      currentPage.commit(data);
    }
  }

  protected abstract WidgetVisitor getVisitor();

  public static WidgetBuilder referenceModifyingWidgetBuilder = new WidgetBuilder(new Class[]{
    WikiWordWidget.class,
    LiteralWidget.class,
    CommentWidget.class,
    PreformattedWidget.class,
    LinkWidget.class,
    ImageWidget.class,
    AliasLinkWidget.class,
    ClasspathWidget.class
  });
}
