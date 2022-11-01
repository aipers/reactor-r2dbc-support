package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.common.utilities.StringUtils;
import com.aipers.groupware.r2dbc.support.converers.SQLTypeConverter;
import com.aipers.groupware.r2dbc.support.models.SQLQuery;
import com.aipers.groupware.r2dbc.support.models.SQLQueryType;
import com.aipers.groupware.r2dbc.support.models.SQLSequenceQuery;
import com.aipers.groupware.r2dbc.support.models.params.SqlParamWrapper;
import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLBinder<R> {

  private final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");
  private final Pattern BINDING_PATTERN = Pattern.compile("\\#\\{(.*?)\\}");

  private final String sql;
  private final Class<R> resultType;
  private final SQLQueryType queryType;
  private final SqlParamWrapper params;
  private final SQLSequenceQuery selectKeyQuery;
  private final SQLTypeConverter sqlTypeConverter;

  public SQLBinder(
      final String sql, final SQLQuery query,
      final SQLTypeConverter sqlTypeconverter, final SqlParamWrapper params
  ) {
    this.sql = sql;
    this.params = params;
    this.queryType = query.getType();
    this.resultType = query.getResultType();
    this.sqlTypeConverter = sqlTypeconverter;
    this.selectKeyQuery = SQLQueryType.SELECT == query.getType() && null != query.getSequence()
      ? query.getSequence()
      : null;
  }

  public SQLFetch<R> execute() {
    final LinkedHashSet<String> binds = new LinkedHashSet<>();
    final String query = StringUtils.replace(
        StringUtils.replace(sql, REPLACE_PATTERN, m -> String.valueOf(params.get(m.group(1)))),
        BINDING_PATTERN,
        m -> {
          final String property = m.group(1);
          binds.add(property);
          return ":".concat(property);
        }
    );

    if (log.isTraceEnabled()) {
      log.trace(
          "MAPPED SQL :: {}\n\n{}",
          query,
          binds.stream()
              .map(bind -> String.format("%s: [%s]", bind, params.get(bind)))
              .collect(Collectors.toList())
      );
    }

    final Function<Connection, Statement> statementFunction = (conn) -> {
      final Statement statement = conn.createStatement(query);

      binds.forEach(bind -> {
        final Object value = params.get(bind);
        if (null == value) {
          statement.bindNull(bind, params.getType(bind));
        } else {
          statement.bind(bind, value);
        }
      });

      return statement;
    };

    return new SQLFetch(statementFunction, this.queryType, this.selectKeyQuery, this.getRowMapper());
  }

  public BiFunction<Row, RowMetadata, R> getRowMapper() {
    return Map.class.isAssignableFrom(resultType)
        ? (row, metadata) ->
              (R) metadata.getColumnMetadatas()
                    .stream()
                    .collect(Collectors.toMap(
                        ColumnMetadata::getName,
                        meta -> row.get(meta.getName(), meta.getJavaType())
                    ))
        : new SQLResult(resultType, this.sqlTypeConverter);
  }

}
