package com.aipers.groupware.r2dbc.support.aliases;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SQLTypeAlias {

  private static SQLTypeAlias _instance;

  private final Map<String, Class> TYPE_ALIASES = new HashMap<>();

  private SQLTypeAlias() {
    this._instance = this;
  }

  public static void initialize(final boolean scan, final String basePackage) throws IOException {
    new SQLTypeAlias();

    if (scan) {
      new SQLTypeAliasScanner(basePackage).initialize();
    } else {
      _instance.TYPE_ALIASES.putAll(getDefaultAliasMap());
    }
  }

  public static void setTypes(final Map<String, Class> types) {
    _instance.TYPE_ALIASES.putAll(types);
  }
  public static void setType(final String alias, final Class type) {
    _instance.TYPE_ALIASES.put(alias, type);
  }
  public static Class getType(final String alias) {
    return _instance.TYPE_ALIASES.get(alias);
  }

  public static Map<String, Class> getDefaultAliasMap() {
    return Collections.unmodifiableMap(
        new HashMap<String, Class>() {
          {
            put("string", String.class);
            put("String", String.class);
            put("short", Short.class);
            put("Short", Short.class);
            put("int", Integer.class);
            put("Integer", Integer.class);
            put("long", Long.class);
            put("Long", Long.class);
            put("float", Float.class);
            put("Float", Float.class);
            put("double", Double.class);
            put("Double", Double.class);
            put("map", Map.class);
            put("hashmap", Map.class);
            put("list", java.util.List.class);
            put("array", java.util.Arrays.class);
          }
        }
    );
  }

}
