package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.common.utilities.CommonUtils;
import com.aipers.groupware.common.utilities.StringUtils;
import com.aipers.groupware.r2dbc.support.models.SQLBindQuery;
import com.aipers.groupware.r2dbc.support.models.SQLIterableQuery;
import com.aipers.groupware.r2dbc.support.models.SQLQueryType;
import com.aipers.groupware.r2dbc.support.models.SQLSequenceQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;

@Slf4j
public class SQLParser {

  private final static Pattern ITERABLE_EXPRESSION_PATTERN = Pattern.compile("[\\#|\\$]\\{(.*?)\\}");

  private final static List<String> chooseChildTags = Arrays.asList("when", "otherwise");
  private final static List<String> referenceTags = Arrays.asList("sql", "include");
  private final static List<String> mainTags = Arrays.asList("select", "insert", "update", "delete", "sql");
  private final static List<String> subTags = Arrays.asList("choose", "trim", "where", "set");
  private final static List<String> allowDynamic = Arrays.asList("if", "when", "otherwise", "foreach", "selectkey");

  public SQLBindQuery parse(
      final SQLQueryType tagType, final Attributes attrs,
      final String tag, final List<String> openTags
  ) {
    final String parent = openTags.get(openTags.size() - 1);

    if ("selectkey".equals(tag) && SQLQueryType.INSERT != tagType)
      throw new SQLLoadException("XML syntax is invalid. 'selectKey' is only insert element");

    if ("set".equals(tag) && SQLQueryType.UPDATE != tagType)
      throw new SQLLoadException("XML syntax is invalid. Incorrect usage SET keyword.");

    if (chooseChildTags.contains(tag) && !"choose".equals(parent))
      throw new SQLLoadException("XML syntax is invalid. Choose Sub tag only accepts When and Otherwise.");

    if (referenceTags.contains(tag) || mainTags.contains(tag))
      throw new SQLLoadException("XML syntax is invalid. Should not be placed in here. tag : " + tag);

    if (!subTags.contains(tag) && !allowDynamic.contains(tag))
      throw new SQLLoadException("XML syntax is invalid. this sub tag not supported :: " + tag);

    final SQLBindQuery.SQLBindQueryBuilder query = SQLBindQuery.builder().type(SQLQueryType.of(tag));

    if ("selectkey".equals(tag)) {
      return query
          .sequence(
              SQLSequenceQuery.builder()
                  .keyProperty(attrs.getValue("keyProperty"))
                  .order(StringUtils.nvl(attrs.getValue("order"), "AFTER"))
                  .build()
          )
          .build();
    }
    else if ("foreach".equals(tag)) {
      if (null == attrs.getValue("collection"))
        throw new SQLLoadException("XML syntax is invalid. foreach tag must have the collection attribute.");

      return query
          .prefix(StringUtils.nvl(attrs.getValue("open")))
          .suffix(StringUtils.nvl(attrs.getValue("close")))
          .iterable(
              SQLIterableQuery.builder()
                  .item(attrs.getValue("item"))
                  .index(attrs.getValue("index"))
                  .collection(attrs.getValue("collection"))
                  .separator(StringUtils.nvl(attrs.getValue("separator")))
                .build()
          )
          .build();
    }
    else if (subTags.contains(tag)) {
      return query
          .suffix(StringUtils.nvl(attrs.getValue("suffix")))
          .prefix(((String) CommonUtils.decode(
              tag,
              "trim", StringUtils.nvl(attrs.getValue("prefix")),
              "choose", "",
              tag.toUpperCase()
          )).concat(" "))
          .prefixOverrides((String) CommonUtils.decode(
              tag,
              "where", "AND |OR ",
              "set", ",",
              StringUtils.nvl(attrs.getValue("prefixOverrides"))
          ))
        .build();
    }

    return query.test(
        SQLQueryType.OTHERWISE == SQLQueryType.of(tag)
          ? "true"
          : attrs.getValue("test")
    ).build();
  }

  public String parseIterable(final SQLIterableQuery option, final String query) {
    final AtomicInteger index = new AtomicInteger();
    final ArrayList<String> items = new ArrayList<>();
    final String sql = StringUtils.replace(
        query, ITERABLE_EXPRESSION_PATTERN,
        m -> {
          String key = m.group(1);

          if (
              key.startsWith(option.getCollection()) ||
              (!StringUtils.hasEmpty(option.getItem()) && key.startsWith(option.getItem())) ||
              -1 < key.indexOf("[")
          ) {
            key = key.replaceFirst("^" + option.getCollection(), "")
                .replaceFirst(
                    -1 < key.indexOf("[")
                        ? key.substring(0, key.indexOf("["))
                        : "^" + StringUtils.nvl(option.getItem(), "\\s"),
                    ""
                )
                .replaceFirst("^\\.?", "");

            if ("".equals(key)) {
              option.setIndex(StringUtils.nvl(option.getIndex(), "index"));
              key = String.format("[%s]", option.getIndex());
            }

            items.add(key);
          } else {
            key = m.group();
          }

          return key;
        });

    option.setExpressions(items.stream().distinct().collect(Collectors.toList()));

    return sql;
  }

}
