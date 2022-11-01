package com.aipers.groupware.r2dbc.support.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class SQLBase {

  protected SQLQueryType type;

  @Builder.Default
  protected String query = "";

  @Builder.Default
  protected List<SQLBindQuery> bind = new ArrayList<>();

}
