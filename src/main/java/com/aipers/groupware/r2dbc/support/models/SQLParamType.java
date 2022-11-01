package com.aipers.groupware.r2dbc.support.models;

import com.aipers.groupware.common.utilities.CommonUtils;
import com.aipers.groupware.r2dbc.support.models.params.BeanSqlParam;
import com.aipers.groupware.r2dbc.support.models.params.EnumSqlParam;
import com.aipers.groupware.r2dbc.support.models.params.IterableSqlParam;
import com.aipers.groupware.r2dbc.support.models.params.MapSqlParam;
import com.aipers.groupware.r2dbc.support.models.params.PrimitiveSqlParam;
import com.aipers.groupware.r2dbc.support.models.params.SqlParamWrapper;
import java.util.Map;

public enum SQLParamType {

  PRIMITIVE {
    @Override
    public SqlParamWrapper getParameters(final Object param) {
      return new PrimitiveSqlParam(param, null == param ? Void.class : param.getClass());
    }
  },
  ITERABLE {
    @Override
    public SqlParamWrapper getParameters(final Object param) {
      return new IterableSqlParam(param);
    }
  },
  MAP {
    @Override
    public SqlParamWrapper getParameters(final Object param) {
      return new MapSqlParam((Map<String, Object>) param);
    }
  },
  ENUM {
    @Override
    public SqlParamWrapper getParameters(final Object param) {
      return new EnumSqlParam((Enum) param);
    }
  },

  BEAN {
    @Override
    public SqlParamWrapper getParameters(final Object param) {
      return new BeanSqlParam(param);
    }
  };

  abstract public SqlParamWrapper getParameters(Object param);

  public static SqlParamWrapper of(final Object param) {
    final SQLParamType type = null == param || CommonUtils.isPrimitive(param)
        ? PRIMITIVE
        : CommonUtils.isIterable(param)
          ? ITERABLE
          : Map.class.isAssignableFrom(param.getClass())
            ? MAP
            : Enum.class.isAssignableFrom(param.getClass())
              ? ENUM
              : BEAN;

    return type.getParameters(param);
  }

}
