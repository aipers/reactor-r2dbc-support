package com.aipers.groupware.r2dbc.support.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper=false)
public class SQLBindQuery extends SQLBase {

  private String test;

  private String prefix;
  private String suffix;
  private String prefixOverrides;

  private SQLIterableQuery iterable;
  private SQLSequenceQuery sequence;

}
