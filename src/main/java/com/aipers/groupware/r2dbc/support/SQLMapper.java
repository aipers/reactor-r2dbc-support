package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.common.utilities.StringUtils;
import com.aipers.groupware.r2dbc.support.converers.SQLTypeConverter;
import com.aipers.groupware.r2dbc.support.models.SQLParamType;
import com.aipers.groupware.r2dbc.support.models.SQLQuery;
import com.aipers.groupware.r2dbc.support.models.params.SqlParamWrapper;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SQLMapper {

  private final Pattern REFERENCE_PATTERN = Pattern.compile("\\[\\_(.*?)\\_\\]");

  private final SQLLoader loader;
  private final SQLTypeConverter typeConverter;

  public SQLQuery getQuery(final String namespace, final String id) {
    return loader.getQuery(namespace, id);
  }

  public SQLBinder build(final String namespace, final String id) {
    return build(namespace, id, null);
  }

  public SQLBinder build(final String namespace, final String id, final Object param) {
    final SQLQuery query = getQuery(namespace, id);
    final SqlParamWrapper params = SQLParamType.of(param);

    if (null != query.getParameterType() && null != param &&
        !query.getParameterType().isAssignableFrom(param.getClass())
    ) {
      throw new SQLMapperException("SQL Mapped mismatch parameter type");
    }

    final String sql = StringUtils.normalizeWhitespace(
        StringUtils.replace(
            SQLBuilder.build(query.getType(), query.getQuery(), query.getBind(), params),
            REFERENCE_PATTERN,
            match -> {
              final String mapperName = match.group(1);
              final int findIndex = mapperName.lastIndexOf(".");
              final SQLQuery refQuery = -1 < findIndex
                ? this.getQuery(mapperName.substring(0, findIndex), mapperName.substring(findIndex))
                : this.getQuery(namespace, mapperName);

              return SQLBuilder.build(
                  refQuery.getType(), refQuery.getQuery(), refQuery.getBind(), params
              );
            }
        )
    );

    return new SQLBinder(sql, query, typeConverter, SQLParamType.of(param));
  }

}
