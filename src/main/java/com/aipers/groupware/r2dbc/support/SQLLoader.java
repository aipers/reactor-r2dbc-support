package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.common.utilities.StreamUtils;
import com.aipers.groupware.r2dbc.support.aliases.SQLTypeAlias;
import com.aipers.groupware.r2dbc.support.connections.TransactionManager;
import com.aipers.groupware.r2dbc.support.models.SQLQuery;
import com.aipers.groupware.r2dbc.support.properteis.SQLProperties;
import io.r2dbc.spi.ConnectionFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;

@Slf4j
public class SQLLoader {

  // TODO: SQL 관련 모든 class
  //       완벽히 모든 기능이 추가되지 않음
  //       필요할 때마다 조금씩 추가해서 사용중
  private final Pattern mapperPattern = Pattern.compile(
      String.format("^(_|\\-|\\w|\\d|\\%s)+\\.xml$", File.separator),
      Pattern.CASE_INSENSITIVE
  );

  private final SAXParserFactory saxFactory = SAXParserFactory.newInstance();
  private final Map<String, Map<String, SQLQuery>> queries = new HashMap<>();

  private final SQLProperties properties;

  public SQLLoader(final SQLProperties properties, final ConnectionFactory connectionFactory) {
    this.properties = properties;

    try {
      this.initialize(connectionFactory);
    } catch (IOException e) {
      throw new RuntimeException("SQL Support initialize error", e);
    }
  }

  private void initialize(final ConnectionFactory connectionFactory) throws IOException {
    if (!properties.isUse()) return;

    log.debug("SQL Utility using start ... [{}]", properties);

    TransactionManager.initialize(connectionFactory);
    SQLTypeAlias.initialize(this.properties.isUseAliasScan(), this.properties.getBasePackage());

    this.load();

    if (properties.isAutoRefreshable()) this.watch();
  }

  public SQLQuery getQuery(final String namespace, final String id) {
    log.trace("QUERY FIND :: {}#{}", namespace, id);

    if (null == this.queries.get(namespace))
      throw new SQLLoadException("Can not find SQL Map. Please check the correct namespace : " + namespace);

    final SQLQuery query = this.queries.get(namespace).get(id);

    if (null == query)
      throw new SQLLoadException("Can no find SQL Map. Please check the correct Map ID : " + id);

    return query;
  }

  private void load() throws IOException {
    this.getResources().forEach(file -> {
      final String filePath = properties.getPath().concat(File.separator).concat(file);
      final URL resource = getClass().getClassLoader().getResource(filePath);

      this.load(resource);
    });
  }

  private void load(final URL resource) {
    try {
      final SQLReader reader =
          new SQLReader(resource, saxFactory.newSAXParser(), properties.isMissAbort());

      if (reader.read()) {
        queries.put(reader.getNamespace(), reader.getQueries());
      }
    } catch (IOException | SAXException | ParserConfigurationException e) {
      log.error("SQL mapper load fail", e);
    }
  }

  private List<String> getResources() throws IOException {
    final URL resource = getClass().getClassLoader().getResource(properties.getPath());

    if (null == resource) throw new SQLLoadException("Resource not found in specified mapper path");

    if (getClass().getClassLoader().getResource(properties.getPath()).getPath().contains("!/")) {
      return getResourcesFromJar();
    }

    try (
        final InputStream in = getClass().getClassLoader().getResourceAsStream(properties.getPath());
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
    ) {
      return br.lines().filter(this::isInclude).collect(Collectors.toList());
    }
  }

  private List<String> getResourcesFromJar() throws IOException {
    final URL resourcePath = getClass().getProtectionDomain().getCodeSource().getLocation();
    final String[] paths = resourcePath.getPath().split("!/");
    final String mapperPath = Paths.get(paths[1], properties.getPath()).toString().concat(File.separator);
    final File file = Paths.get(paths[0].substring(5)).toFile(); // 5 mean file:

    try (final JarFile jarFile = new JarFile(file)) {
      return StreamUtils.enumerationAsStream(jarFile.entries())
          .filter(entry ->
              !entry.isDirectory() &&
               entry.getName().startsWith(mapperPath) && isInclude(entry.getName())
          )
          .map(entry -> entry.getName().replaceFirst(mapperPath, ""))
          .collect(Collectors.toList());
    }
  }

  private boolean isInclude(final String filename) {
    return mapperPattern.matcher(filename).matches();
  }

  private void watch() {
    if (getClass().getClassLoader().getResource(properties.getPath()).getPath().contains("!/")) {
      return;
    }

    new Thread(() -> {
      try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
        Paths.get(getClass().getClassLoader().getResource(properties.getPath()).toURI())
            .register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        while (true) {
          final WatchKey watchKey = watchService.take();

          watchKey.pollEvents().forEach(event ->
              load(
                  getClass().getClassLoader().getResource(
                      String.format("%s%s%s", properties.getPath(), File.separator, event.context())
                  )
              )
          );
          watchKey.reset();
        }
      } catch (IOException | URISyntaxException | InterruptedException e) {
        log.error("Mapper watcher error", e);
      }
    }).start();
  }

}
