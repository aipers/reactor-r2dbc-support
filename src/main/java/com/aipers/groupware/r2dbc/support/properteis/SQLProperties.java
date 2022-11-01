package com.aipers.groupware.r2dbc.support.properteis;

public interface SQLProperties {

  /**
   * SQL Support 사용여부를 반환합니다
   * @return
   */
  default boolean isUse() {
    return true;
  }

  /**
   * @Alias 주석을 통해 parameterType, resultType 형식을 지정할 수 있는지 여부를 반환합니다.
   * @return
   */
  default boolean isUseAliasScan() {
    return true;
  }

  /**
   * resultType 형식이 지정된 형식과 맞지 않는경우 throw 할지 여부를 반환합니다.
   * @return
   */
  default boolean isMissAbort() {
    return false;
  }

  /**
   * SQL XML 파일의 변경을 체크하고 자동으로 다시 로드할지 여부를 반환합니다.
   * @return
   */
  default boolean isAutoRefreshable() {
    return false;
  }

  /**
   * SQL XML 파일의 기본 경로를 반환합니다.
   * @return
   */
  default String getPath() {
    return "mappers";
  }

  /**
   * SQL Alias 스캔을 위한 package 시작점을 반환 합니다
   * @return
   */
  default String getBasePackage() {
    return "com.aipers.groupware";
  }

}
