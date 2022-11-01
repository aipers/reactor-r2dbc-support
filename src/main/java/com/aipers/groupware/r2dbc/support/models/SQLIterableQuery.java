package com.aipers.groupware.r2dbc.support.models;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SQLIterableQuery {

  private String item;
  private String index;
  private String separator;
  private String collection;

  private List<String> expressions;

}
