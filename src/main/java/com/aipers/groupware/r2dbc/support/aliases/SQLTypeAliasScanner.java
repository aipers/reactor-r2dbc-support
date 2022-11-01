package com.aipers.groupware.r2dbc.support.aliases;

import com.aipers.groupware.common.Constants;
import com.aipers.groupware.common.utilities.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLTypeAliasScanner {

  private final String basePackage;

  public SQLTypeAliasScanner(final String basePackage) {
    this.basePackage = basePackage;
  }

  private Class loadClass(final String className) {
    try {
      return Class.forName(className.substring(0, className.lastIndexOf(".class")));
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private boolean hasAnnotation(final Class clazz) {
    return Optional.ofNullable(clazz)
        .map(c -> null != c.getAnnotation(Alias.class))
        .orElse(false);
  }

  public void initialize() throws IOException {
    log.trace("SQL Type alias scan start ...");

    final ClassLoader loader = this.getClass().getClassLoader();
    final Map<String, Class> types = this.getResources(loader, basePackage, new ArrayList<>())
        .stream()
        .map(this::loadClass)
        .filter(this::hasAnnotation)
        .collect(Collectors.toMap(
            clazz -> {
              final Annotation annotation = clazz.getAnnotation(Alias.class);
              return StringUtils.nvl(((Alias) annotation).value(), clazz.getSimpleName());
            },
            clazz -> clazz
        ));

    log.trace("SQL Type alias scanner to scanned types :: {}", types);

    mergeTypeAlias(types);
  }

  public List<String> getResources(
      final ClassLoader loader, final String base, final List<String> accumulate
  ) throws IOException {
    try (
        final InputStream is = loader.getResourceAsStream(
            base.replace(Constants.PACKAGE_SEPARATOR, Constants.PATH_SEPARATOR));
        final InputStreamReader streamReader = new InputStreamReader(is);
        final BufferedReader reader = new BufferedReader(streamReader)
    ) {
      reader.lines()
          .forEach(line -> {
            final String name = String.format("%s%s%s", base, Constants.PACKAGE_SEPARATOR, line);

            if (name.endsWith(".class")) {
              accumulate.add(name);
            } else {
              try {
                this.getResources(loader, name, accumulate);
              } catch (IOException e) {
              }
            }
          });
    }

    return accumulate;
  }

  private void mergeTypeAlias(final Map<String, Class> types) {
    SQLTypeAlias.setTypes(
        Stream.concat(
            types.entrySet().stream(), SQLTypeAlias.getDefaultAliasMap().entrySet().stream()
        )
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue))
    );
  }

}
