package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.common.Constants;
import com.aipers.groupware.common.utilities.CommonUtils;
import com.aipers.groupware.common.utilities.StreamUtils;
import com.aipers.groupware.common.utilities.StringUtils;
import com.aipers.groupware.r2dbc.support.models.SQLBindQuery;
import com.aipers.groupware.r2dbc.support.models.SQLIterableQuery;
import com.aipers.groupware.r2dbc.support.models.SQLParamType;
import com.aipers.groupware.r2dbc.support.models.SQLQueryType;
import com.aipers.groupware.r2dbc.support.models.params.SqlParamWrapper;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ognl.Ognl;
import ognl.OgnlException;

public abstract class SQLBuilder {

  private static final Pattern DYNAMIC_PATTERN = Pattern.compile("\\{(\\d)\\}");
  private static final List<SQLParamType> SINGLE_PARAM_TYPE = Arrays.asList(
      SQLParamType.PRIMITIVE, SQLParamType.ITERABLE
  );

  public static final String build(
      final SQLQueryType type, final String query,
      final List<SQLBindQuery> binds, final SqlParamWrapper param
  ) {
    final List<String> queries = binds.stream()
        .map(bind -> buildQuery(bind, param))
        .collect(Collectors.toList());

    if (SQLQueryType.CHOOSE == type) {
      final AtomicInteger otherIndex = new AtomicInteger(-1);
      final AtomicInteger matchedIndex = new AtomicInteger(-1);

      IntStream.range(0, binds.size()).forEach(i -> {
        if (SQLQueryType.OTHERWISE == binds.get(i).getType()) {
          otherIndex.set(i);
        } else if (-1 == matchedIndex.get() && !StringUtils.hasEmpty(queries.get(i))) {
          matchedIndex.set(i);
        }
      });

      if (-1 < matchedIndex.get()) {
        IntStream.range(0, queries.size()).forEach(i -> {
          if (matchedIndex.get() != i) {
            queries.set(i, "");
          }
        });
      }
    }

    return StringUtils.replace(query, DYNAMIC_PATTERN, m -> queries.get(Integer.parseInt(m.group(1))));
  }

  private static final String buildQuery(final SQLBindQuery bind, final SqlParamWrapper param) {
    String query = "";

    try {
      if (!CommonUtils.isEmpty(bind.getIterable())) {
        final SQLIterableQuery option = bind.getIterable();
        final Object iterable = param.get(option.getCollection());

        if (!CommonUtils.isIterable(iterable)) {
          throw new SQLMapperException("foreach tag parameter is not repeatable");
        }

        final AtomicInteger index = new AtomicInteger();
        final String unique = String.valueOf(UUID.randomUUID()).replaceAll("-", "_");
        final List<String> queries = (List<String>) StreamUtils.stream(iterable).map(item -> {
          final int idx = index.getAndIncrement();

          String sql = StringUtils.deSafeHTML(bind.getQuery());
          for (final String expression : option.getExpressions()) {
            try {
              final boolean isIndex = !StringUtils.hasEmpty(option.getIndex())
                  && -1 < expression.indexOf("[" + option.getIndex() + "]");
              final Object ognlValue = Ognl.getValue(
                  isIndex
                      ? StringUtils.replace(expression, option.getIndex(), String.valueOf(idx))
                      : expression,
                  isIndex ? iterable : item
              );
              final String key = String.format(
                  "foreach.%s.%s.%d.%s", unique, option.getCollection(), idx, expression
              );

              param.set(key, ognlValue);
              sql = StringUtils.replace(sql, expression, String.format("#{%s}", key));
            } catch (OgnlException e) {
              sql = StringUtils.replace(sql, expression, "null");
            }
          }

          return sql;
        }).collect(Collectors.toList());

        query += String.join(StringUtils.nvl(option.getSeparator(), " "), queries);
      }

      if (!StringUtils.hasEmpty(bind.getTest())) {
        query = null != param && CommonUtils.isTrue(
            Ognl.getValue(
                bind.getTest(),
                SINGLE_PARAM_TYPE.contains(param) ? param.get("all") : param.getValue()
            )
        )
            ? bind.getQuery()
            : Constants.EMPTY_STRING;
      } else if (!CommonUtils.isEmpty(bind.getBind())) {
        query = build(bind.getType(), bind.getQuery(), bind.getBind(), param);
      }

      if (!StringUtils.hasEmpty(bind.getPrefix())) {
        if (!StringUtils.hasEmpty(bind.getPrefixOverrides())) {
          query = query
              .replaceFirst("^(\\s+)?(" + bind.getPrefixOverrides() + ")", bind.getPrefix());
        } else {
          query = bind.getPrefix() + " " + query;
        }
      }

      if (!StringUtils.hasEmpty(bind.getSuffix())) {
        query = String.format("%s %s", query, bind.getSuffix());
      }
    } catch (OgnlException e) {
    }

    return query;
  }

}
