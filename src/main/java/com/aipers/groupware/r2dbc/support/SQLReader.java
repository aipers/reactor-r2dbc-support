package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.common.utilities.CommonUtils;
import com.aipers.groupware.common.utilities.StringUtils;
import com.aipers.groupware.r2dbc.support.aliases.SQLTypeAlias;
import com.aipers.groupware.r2dbc.support.models.SQLBase;
import com.aipers.groupware.r2dbc.support.models.SQLBindQuery;
import com.aipers.groupware.r2dbc.support.models.SQLQuery;
import com.aipers.groupware.r2dbc.support.models.SQLQueryType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Slf4j
public class SQLReader extends DefaultHandler {

  private final static String ROOT = "mapper";

  @Getter
  private String filename;
  @Getter
  private String namespace;
  @Getter
  private Map<String, SQLQuery> queries;

  private final URL resource;
  private final SAXParser parser;
  private final boolean missAbort;
  private final SQLParser sqlParser = new SQLParser();

  public SQLReader(
      final URL resource, final SAXParser parser, final boolean missAbort
  ) throws IOException {
    this.parser = parser;
    this.resource = resource;
    this.missAbort = missAbort;
  }

  public boolean read() throws IOException, SAXException {
    try (final InputStream resourceStream = resource.openStream()) {
      this.filename = resource.getPath();

      parser.parse(resourceStream, this);
    }

    return true;
  }

  private String currentId;
  private List<String> openTags = new ArrayList<>();

  private final static List<String> selfClosingTags = Arrays.asList("include");
  private final static List<String> mainTags = Arrays.asList("select", "insert", "update", "delete", "sql");

  @Override
  public void startDocument() {
    log.trace("XML Parse start :: {}", resource.getPath());
  }

  @Override
  public void endDocument() {
    this.queries.keySet().stream().forEach(
        key -> this.queries.get(key).setQuery(
            StringUtils.normalizeWhitespace(this.queries.get(key).getQuery()).trim()
        )
    );
  }

  @Override
  public void startElement(
      final String uri, final String localName, final String tag, final Attributes attrs
  ) {
    if (ROOT.equals(tag)) {
      this.queries = new HashMap<>();
      this.namespace = attrs.getValue("namespace");
      return;
    }

    if (null == this.namespace) return;

    if (selfClosingTags.contains(tag)) {
      final String refId = attrs.getValue("refid");

      if (StringUtils.hasEmpty(refId))
        throw new SQLLoadException("XML syntax is invalid. ref property is required.");

      final String referenceQuery = String.format(" [_%s_] ", refId);
      if (1 >= depth()) {
        this.queries.get(this.currentId).setQuery(
            this.queries.get(this.currentId).getQuery() + referenceQuery
        );
      } else {
        final SQLBindQuery bind = getCurrentBind();
        bind.setQuery(bind.getQuery() + referenceQuery);
      }

      return;
    }

    if (this.openTags.isEmpty()) {
      if (!mainTags.contains(tag))
        throw new SQLLoadException("XML syntax is invalid. this tag not supported :: " + tag);

      final String id = attrs.getValue("id");

      if (StringUtils.hasEmpty(id))
        throw new SQLLoadException("XML syntax is invalid. main tag id property is required.");

      if (id.contains("."))
        throw new SQLLoadException("XML syntax is invalid. id attribute cannot contain characters.");

      this.currentId = id;
      this.queries.put(
          this.currentId,
          SQLQuery.builder()
              .type(SQLQueryType.of(tag))
              .resultType(this.normalizeClass(attrs.getValue("resultType")))
              .parameterType(this.normalizeClass(attrs.getValue("parameterType")))
            .build()
      );
    } else {
      parse(tag, attrs);
    }

    this.openTags.add(tag);
  }

  @Override
  public void endElement(final String uri, final String localName, final String tag) {
    if (this.openTags.isEmpty()) return;

    this.openTags.remove(this.openTags.size() - 1);
  }

  @Override
  public void characters(char[] chars, int start, int length) {
    if (0 == length) return;

    final String text = new String(chars, start, length);
    if (text.matches("\\n\\s+")) return;

    if (1 == depth()) {
      this.queries.get(this.currentId).setQuery(this.queries.get(this.currentId).getQuery() + text);
    } else {
      parseText(text);
    }
  }

  private void parse(final String tag, final Attributes attrs) {
    final int depth = depth();
    final SQLQuery parent = this.queries.get(this.currentId);
    final SQLBindQuery parsed = sqlParser.parse(parent.getType(), attrs, tag, this.openTags);
    final SQLQueryType type = parsed.getType();

    if (1 == depth) {
      if (SQLQueryType.SELECTKEY == type) {
        parent.setSequence(parsed.getSequence());
        parent.getSequence().setResultType(
            this.normalizeClass(attrs.getValue("resultType"))
        );
      } else {
        parent.setQuery(parent.getQuery() + "\n{" + parent.getBind().size() + "} ");
        parent.getBind().add(parsed);
      }
    } else {
      final SQLBindQuery bind = getCurrentBind();

      bind.setQuery(bind.getQuery() + "\n{" + bind.getBind().size() + "} ");
      bind.getBind().add(parsed);
    }
  }

  private void parseText(final String text) {
    final SQLBase target = 1 == depth() ? this.queries.get(this.currentId) : getCurrentBind();
    final String query = (1 < depth() && null != ((SQLBindQuery) target).getIterable())
        ? sqlParser.parseIterable(((SQLBindQuery) target).getIterable(), StringUtils.deSafeHTML(text))
        : StringUtils.deSafeHTML(text);

    target.setQuery(target.getQuery() + query);
  }

  private Class normalizeClass(final String type) {
    if (StringUtils.hasEmpty(type)) return null;

    try {
      final Class clazz = SQLTypeAlias.getType(type);

      return CommonUtils.isEmpty(clazz) ? Class.forName(type) : clazz;
    } catch (ClassNotFoundException e) {
      log.warn("XML syntax invalid. non existent class: {} {}:{}", filename, namespace, type);
      if (this.missAbort) throw new RuntimeException(e);
    }

    return Object.class;
  }

  private int depth() {
    return this.openTags.size();
  }

  private SQLBindQuery getCurrentBind() {
    SQLBindQuery bind = this.queries.get(this.currentId).getBind().get(
        this.queries.get(this.currentId).getBind().size() - 1
    );

    for (int i = depth();i > 2;i--) {
      bind = bind.getBind().get(bind.getBind().size() - 1);
    }

    return bind;
  }

}
