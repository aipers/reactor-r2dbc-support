# reactive r2dbc Dynamic SQL support utility

## JitPack 통해 release 배포됨 (with TAG)
- implementation 'com.github.aipers:reactor-r2dbc-support:0.0.1.RELEASE'

## Library
- 이 라이브러리는 ognl, aopalliance, r2dbc-spi, reactor-core, com.aipers.groupware:common-utils 라이브러리를 **포함하여 빌드**됩니다,
- 추가로 해당 라이브러리는 slf4j, lombok, jackson 라이브러리를 종속성으로 갖지만, 포함하여 빌드되지 않기때문에 **각 프로젝트에서 해당 라이브러리를 포함**하여야 합니다.

### Support package **_com.aipers.groupware.r2dbc.support_**
#### SQL Type Alias
- SQLTypeAlias (parameter, result type Alias 처리)
  - SQLTypeAliasScanner (지정된 basePackage 하위에 Alias Annotation 달린 모든 POJO 객체 로드하여 SQLTypeAlias 등록)

#### Aspects
##### 트랜잭션 관리를 위한 proxy 지원 메서드
- _Spring Transaction 사용하면 이 기능은 **무시**하면 됩니다._
- Transacting: proxy 처리를 위한 주석
- TransactionInvocationHandler: java reflection 기반으로 proxy 직접 사용을 지원하는 유틸리티
- TransactionInvocationInterceptor: aopalliance MethodInterceptor 사용을 지원하는 유틸리티

#### Connections
##### R2DBC 커넥션 사용
- TransactionContext: reactor 메서드의 Context
- TransactionIsolation: 커넥션의 격리 수준 enum
- TransactionManager: 트랜잭션 및 커넥션 관리용 유틸리티

#### Converters
##### 쿼리 결과를 매핑하기 위한 타입 컨버터 모음
- 기본 JAVA 타입의 유형 데이터 컨버터
  - BigDecimal, Byte, Character, Date, Double, Enum, Float, Shot, Integer, Long
- 이 기능을 확장하고 싶다면 SQLTypeConverter 클래스를 상속받아 확장한 클래스를 작성 후 SQLMapper 생성자의 매개변수로 넘겨줄 수 있음

#### SQL Model
##### Params
- SqlParamWrapper (처리할 수 있는 SQL 타입으로 변환하기 위한 Wrapping 객체의 추상화 선언)
- BeanSqlParam (POJO 객체 Wrapper)
  - ReflectionWrapper: POJO 객체 처리를 위한 invoke reflection 유틸리티
- EnumSqlParam (ENUM 객체 Wrapper)
- IterableSqlParam (반복 가능한 객체 Wrapper)
- PrimitiveSqlParam (원시 객체의 Wrapper)

#### Query
- SQLQuery
  - select, insert, update, delete 쿼리로 시작되는 메인 쿼리객체
- SQLSequenceQuery
  - insert 객체 처리 전/후 데이터 조회 쿼리 (like mybatis selectKey)
- SQLBindQuery
  - Dynamic 쿼리객체
- SQLIterableQuery
  - foreach 처리하기 위한 쿼리객체

#### SQL XML Processing
- SQLLoader
  - 모든 클래스 패스의 전달된 path 하위의 xml 파일을 로드하여 SQLReader 에게 전달하는 역할을 담당합니다.
  - 모든 가공된 쿼리집합을 보관하는 역할을 담당합니다.
  - property auto-refreshable 설정이 참인경우 xml mapper 변경사항을 체크하여 재로드 요청하는 역할을 담당합니다.
- SQLReader
  - SAX Parser 사용하여 작성된 쿼리들을 로드하여 SQLParser 에게 전달하는 역할을 담당합니다.
- SQLParser
  - 작성된 Dynamic 쿼리문을 규칙에 따라 SQLQuery 객체로 변환하는 역할을 담당합니다.

#### SQL Execute
##### Utility
- SQLBuilder (SQLQuery 집합 으로 SQL 생성 유틸리티)
  - SQLQuery 객체와 파라메터 객체를 참조하여, Dynamic 쿼리를 생성합니다.
  - 생성된 Dynamic 쿼리와 파라메터를 참조하여 statement bind 할 수 있도록 빌드합니다.
- SQLResult (기본 Result type or map 타입 반환)
  - 기본 map 혹은 Alias 지정된 1차원 Model 사용하지 않는 경우 직접 BiFunction RowMapper 지정해서 사용해야 함
  - collection 처리는 fetch 후 bufferUntilChanged + map 사용하여 직접 변환해야 함
##### Processor
- SQLMapper
  - 요청된 쿼리에 매핑되는 SQLQuery 객체를 찾아 치환값을 변환 후 SQLBuilder 통해 쿼리를 빌드합니다 
  - 빌드된 쿼리와 파라메터를 SQLBinder 객체로 반환합니다.
- SQLBinder
  - 빌드된 쿼리와 파라메터를 실제 Statement 바인딩 처리 하고 SQLFetch 객체로 돌려줍니다.
- SQLFetch
  - statement 객체를 실행 시킨 후 Mono 혹은 Flux 형태의 객체로 결과를 반환 하는 역할을 담당합니다.

```
_warning_  
이 SQL 서비스는 MyBatis 익숙한 사용자를 위해 비슷하게 사용 할 수 있도록 만들었지만,
MyBatis 서비스가 아니며, 해당 라이브러리의 모든 기능을 지원하지 않습니다.
* 이 기능은 완료상태가 아니며, 필요한 경우 기능을 추가하여 사용하고 있음  
* Spring 사용하며, 기본 CRUD만 사용 하는 Entity는 ReactiveCrudRepository 병행사용 권장
```

※ 기능이 복잡하거나 collection, association 많이 사용해야 하는 경우 mybatis-r2dbc 혹은 reactive-mybatis-support 교체 고려


#### How to use ?
- SQLProperties 객체를 상속받는 POJO 객체를 생성하여 프로퍼티 값을 주입하거나, 기본 값을 Override 하여 사용합니다.
  - use: 이 항목에 의해 해당 라이브러리의 모든 기능을 on/off 할 수 있습니다.
  - use-alias: @Alias 주석을 사용할지 여부를 지정합니다.
  - miss-abort: 지정된 유형의 타입 클래스 (parameter type, result type) 찾을 수 없을때 오류를 발생할 지 여부를 지정합니다.
    - false 지정된 경우 특정할 수 없는 모든 타입은 Object.class 지정 됩니다.
  - auto-refreshable: xml 파일의 변경사항을 감지하고 쿼리객체를 자동으로 갱신할지 여부를 지정 합니다.
  - path : mapper xml 스캔할 폴더경로를 지정합니다. 기본적으로 모든 클래스패스의 하위를 참조합니다.
    - mappers -> classpath*:/mappers
  - base-package: Alias annotation 스캔할 기본 패키지를 지정합니다.
    - use-alias 값이 off 경우 해당 값은 필요하지 않습니다.

###### Example
- src/SQLSupportProperties.java
  ```java
  import lombok.Getter;
  import properteis.com.aipers.groupware.r2dbc.support.test.SQLProperties;
  import org.springframework.boot.context.properties.ConfigurationProperties;
  import org.springframework.boot.context.properties.ConstructorBinding;
  import org.springframework.boot.context.properties.bind.DefaultValue;
  
  @Getter
  @ConstructorBinding
  @ConfigurationProperties(prefix = "aipers.sql")
  public class SQLSupportProperties extends SQLProperties {
    private final boolean use;
    private final String path;
    private final boolean useAlias;
    private final boolean missAbort;
    private final String basePackage;
  
    public SQLProperties(
      @DefaultValue("true") final String use, @DefaultValue("mappers") final String path,
      @DefaultValue("true") final String useAlias, @DefaultValue("false") final String classMissAbort,
      @DefaultValue("com.aipers.groupware") final String basePackage
    ) {
      this.path = path;
      this.basePackage = basePackage;
      this.use = Boolean.parseBoolean(use);
      this.useAlias = Boolean.parseBoolean(useAlias);
      this.missAbort = Boolean.parseBoolean(classMissAbort);
    }
    
    public String getPath() {
      if (path.startsWith("file:")) throw new RuntimeException("Unsupported mapper path format");

      return this.path.replaceFirst("(classpath)?(\\*)?(:)?(\\/)?", "");
    }
  }
  ```
  - src/SQLSupportAutoConfiguration.java
    ```java
    import com.aipers.groupware.r2dbc.support.SQLLoader;
    import com.aipers.groupware.r2dbc.support.SQLMapper;
    import converers.com.aipers.groupware.r2dbc.support.test.SQLTypeConverter;
    import properteis.com.aipers.groupware.r2dbc.support.test.SQLProperties;
    import io.r2dbc.spi.ConnectionFactory;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
  
    @Slf4j
    @Configuration
    @RequiredArgsConstructor
    public class SQLSupportAutoConfiguration {
  
      @Bean
      protected SQLTypeConverter sqlTypeConverter() {
        return new SQLTypeConverter();
      }
  
      @Bean
      protected SQLLoader sqlLoader(
        final SQLProperties properties, final ConnectionFactory connectionFactory
      ) {
        return new SQLLoader(properties, connectionFactory);
      }
  
      @Bean
      protected SQLMapper sqlMapper(final SQLLoader sqlLoader, final SQLTypeConverter typeConverter) {
        return new SQLMapper(sqlLoader, typeConverter);
      }
  
    }

    ```
- src/TestDTO.java
  ```java
  import aliases.com.aipers.groupware.r2dbc.support.test.Alias;
  import java.math.BigInteger;
  import lombok.Data;
  
  @Data
  @Alias("TestDTO")
  public class TestDTO {
  
    private int seq;
    private String id;
    private Float random;
    private BigInteger number;
  
  }
  ```
- src/TestRepository.class
  ```java
  import com.aipers.groupware.r2dbc.support.SQLRepository;
  import reactor.core.publisher.Flux;
  
  public interface TestRepository extends SQLRepository {
  
    Flux<TestDTO> findAll();
  
  }
  ```
- src/TestRepositoryImplement.java
  ```java
  import com.aipers.groupware.r2dbc.support.SQLMapper;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.stereotype.Repository;
  import reactor.core.publisher.Flux;
  
  @Slf4j
  @Repository
  @RequiredArgsConstructor
  public class TestRepositoryImplement implements TestRepository {
  
    private final SQLMapper mapper;
  
    public Flux<TestDTO> findAll() {
      return mapper.build(namespace(), id(), null).execute().all();
    }
  
  }
  ```
- resources/mappers/Test.xml
  ```xml
  <mapper namespace="TestRepository">
  
    <!-- with PostgreSQL -->
    <select id="findAll" resultType="TestDTO">
    <![CDATA[
      SELECT seq, md5(random()::text) AS id,
             RANDOM() AS random,
             (RANDOM() * 100)::INTEGER AS number
        FROM (SELECT 1 AS seq UNION ALL SELECT 2 UNION ALL SELECT 3) AS tmp
    ]]>
    </select>
  
  </mapper>
  ```
