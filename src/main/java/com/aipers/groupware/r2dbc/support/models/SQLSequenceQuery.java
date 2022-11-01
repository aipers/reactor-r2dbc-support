package com.aipers.groupware.r2dbc.support.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper=false)
public class SQLSequenceQuery extends SQLBase {

  private String order;
  private String keyProperty;
  private Class resultType;

}
