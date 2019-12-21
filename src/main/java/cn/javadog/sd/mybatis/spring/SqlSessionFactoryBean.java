package cn.javadog.sd.mybatis.spring;

import static org.springframework.util.Assert.notNull;
import static org.springframework.util.Assert.state;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasLength;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import cn.javadog.sd.mybatis.builder.xml.XMLConfigBuilder;
import cn.javadog.sd.mybatis.builder.xml.XMLMapperBuilder;
import cn.javadog.sd.mybatis.mapping.Environment;
import cn.javadog.sd.mybatis.plugin.Interceptor;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.session.SqlSessionFactoryBuilder;
import cn.javadog.sd.mybatis.spring.transaction.SpringManagedTransactionFactory;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.support.io.VFS;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.wrapper.ObjectWrapperFactory;
import cn.javadog.sd.mybatis.support.transaction.TransactionFactory;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * @author 余勇
 * @date 2019-12-21 14:58
 *
 * 实现 FactoryBean、InitializingBean、ApplicationListener 接口，负责创建 SqlSessionFactory 对象。
 * 在Spring容器中，这是最常用的方式去创建一个共享的 SqlSessionFactory 对象。这个对象可以通过依赖注入的方式
 * 传递给 MyBatis 的 DAO，也就是mapper。
 *
 * {@code DataSourceTransactionManager} 和 {@code JtaTransactionManager} 都可以用作给 {@code SqlSessionFactory} 提供事务支持。
 * 当使用容器管理事务或者事务跨数据库时，必须使用 JTA。
 */
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ApplicationEvent> {

  /**
   * 日志
   */
  private static final Log LOGGER = LogFactory.getLog(SqlSessionFactoryBean.class);

  /**
   * 指定 mybatis-config.xml 路径的 Resource 对象
   */
  private Resource configLocation;

  /**
   * 全局配置
   */
  private Configuration configuration;

  /**
   * 指定 Mapper 路径的 Resource 数组
   */
  private Resource[] mapperLocations;

  /**
   * dataSource 对象
   */
  private DataSource dataSource;

  /**
   * 事务工厂
   */
  private TransactionFactory transactionFactory;

  /**
   * 全局配置属性，用于解析占位符
   */
  private Properties configurationProperties;

  /**
   * sql会话工厂构造器
   */
  private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

  /**
   * SQL会话工厂
   */
  private SqlSessionFactory sqlSessionFactory;

  /**
   * 所使用环境名，需要 spring 3.1
   */
  private String environment = SqlSessionFactoryBean.class.getSimpleName();

  /**
   * 是否 failFast
   */
  private boolean failFast;

  /**
   * 插件
   */
  private Interceptor[] plugins;

  /**
   * 类型转换器
   */
  private TypeHandler<?>[] typeHandlers;

  /**
   * 类型转换器所在的包
   */
  private String typeHandlersPackage;

  /**
   * 类型别名
   */
  private Class<?>[] typeAliases;

  /**
   * 类型别名所在的包
   */
  private String typeAliasesPackage;

  /**
   * 指定类型别名的父类
   */
  private Class<?> typeAliasesSuperType;

  /**
   * 文件心态实现类，用于找到指定的包
   */
  private Class<? extends VFS> vfs;

  /**
   * 缓存对象
   */
  private Cache cache;

  /**
   * 对象工厂
   */
  private ObjectFactory objectFactory;

  /**
   * 对象包装类工厂
   */
  private ObjectWrapperFactory objectWrapperFactory;

  /*相应的get/set*/

  public void setObjectFactory(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
    this.objectWrapperFactory = objectWrapperFactory;
  }

  public Class<? extends VFS> getVfs() {
    return this.vfs;
  }

  public Cache getCache() {
    return this.cache;
  }

  public void setCache(Cache cache) {
    this.cache = cache;
  }

  public void setPlugins(Interceptor[] plugins) {
    this.plugins = plugins;
  }

  public void setTypeAliasesPackage(String typeAliasesPackage) {
    this.typeAliasesPackage = typeAliasesPackage;
  }

  public void setTypeAliasesSuperType(Class<?> typeAliasesSuperType) {
    this.typeAliasesSuperType = typeAliasesSuperType;
  }

  public void setTypeHandlersPackage(String typeHandlersPackage) {
    this.typeHandlersPackage = typeHandlersPackage;
  }

  /**
   * 设置所有的类型处理器。这些处理器必须有 {@code MappedTypes} 注解修饰，也可以有 {@code MappedJdbcTypes}
   */
  public void setTypeHandlers(TypeHandler<?>[] typeHandlers) {
    this.typeHandlers = typeHandlers;
  }

  public void setTypeAliases(Class<?>[] typeAliases) {
    this.typeAliases = typeAliases;
  }

  /**
   * 如果为true，当Configuration加载完所有 mappedstatement 后，会去检验是否还有中途失败的 mappedstatement。默认不检验
   */
  public void setFailFast(boolean failFast) {
    this.failFast = failFast;
  }

  public void setConfigLocation(Resource configLocation) {
    this.configLocation = configLocation;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setMapperLocations(Resource[] mapperLocations) {
    this.mapperLocations = mapperLocations;
  }

  public void setConfigurationProperties(Properties sqlSessionFactoryProperties) {
    this.configurationProperties = sqlSessionFactoryProperties;
  }

  /**
   * @param dataSource a JDBC {@code DataSource}
   * 设置 dataSource，可以用于管理事务。可以通过 {@code DataSourceUtils} 或者 {@code DataSourceTransactionManager} 直接拿到一个
   * 支持事务的 JDBC 连接。这里被指定的 {@code DataSource} 应该是最终能管理事务的 {@code DataSource}，而不是一个 {@code TransactionAwareDataSourceProxy}.
   * 只有操作数据的代码会使用 {@code TransactionAwareDataSourceProxy}。如果传进来的就是 {@code TransactionAwareDataSourceProxy}，
   * 那么也会从中解析出  {@code DataSource}.
   */
  public void setDataSource(DataSource dataSource) {
    if (dataSource instanceof TransactionAwareDataSourceProxy) {
      // 如果传进来的是 TransactionAwareDataSourceProxy，那我们管理事务时，使用的是内部的DataSource，否则操作数据的代码无法使用事务
      this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
    } else {
      this.dataSource = dataSource;
    }
  }

  /**
   * 设置 {@code SqlSessionFactoryBuilder} 用于创建 {@code SqlSessionFactory}.
   * 这个方法主要是用于测试 SqlSessionFactory 能够被依赖注入，默认情况下 sqlSessionFactoryBuilder 创建的为 {@code DefaultSqlSessionFactory}
   * @param sqlSessionFactoryBuilder a SqlSessionFactoryBuilder
   *
   */
  public void setSqlSessionFactoryBuilder(SqlSessionFactoryBuilder sqlSessionFactoryBuilder) {
    this.sqlSessionFactoryBuilder = sqlSessionFactoryBuilder;
  }

  /**
   * 是指事务工厂，默认是{@code SpringManagedTransactionFactory}
   * 强烈建议使用默认的事务工厂，如果不使用的话，当有一个事务还是活跃的，这个时候，任何通过Spring管理的mybatis去获取 SqlSession 的操作都会GG
   */
  public void setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
  }

  /**
   * 这里设置的会覆盖mybatis-config里设置的环境。这里仅仅是为了设置占位符，默认值是 {@code SqlSessionFactoryBean.class.getSimpleName()}.
   */
  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  /**
   * 构建 SqlSessionFactory 对象。此方法会在Spring实例化当前bean之后进行
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    notNull(dataSource, "Property 'dataSource' is required");
    notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");
    state((configuration == null && configLocation == null) || !(configuration != null && configLocation != null),
              "Property 'configuration' and 'configLocation' can not specified with together");

    // 创建 SqlSessionFactory 对象
    this.sqlSessionFactory = buildSqlSessionFactory();
  }

  /**
   * 创建 SqlSessionFactory 对象
   * 默认的实现使用标准的Mybatis的 {@code XMLConfigBuilder} API 。
   * 1.3.0版本之后，可以直接指定一个 {@link Configuration}实例，而不需要去解析配置文件
   */
  protected SqlSessionFactory buildSqlSessionFactory() throws IOException {

    final Configuration targetConfiguration;

    // 初始化 configuration 对象，和设置其 `configuration.variables` 属性
    XMLConfigBuilder xmlConfigBuilder = null;
    // 不为空，如上面注释所说，直接指定的
    if (this.configuration != null) {
      targetConfiguration = this.configuration;
      if (targetConfiguration.getVariables() == null) {
        targetConfiguration.setVariables(this.configurationProperties);
      } else if (this.configurationProperties != null) {
        targetConfiguration.getVariables().putAll(this.configurationProperties);
      }
    }
    else if (this.configLocation != null) {
      xmlConfigBuilder = new XMLConfigBuilder(this.configLocation.getInputStream(), null, this.configurationProperties);
      // 这个时候的 Configuration 只是个空，还没有解析
      targetConfiguration = xmlConfigBuilder.getConfiguration();
    }
    // 啥也没有，就是个空
    else {
      LOGGER.debug("Property 'configuration' or 'configLocation' not specified, using default MyBatis Configuration");
      targetConfiguration = new Configuration();
      Optional.ofNullable(this.configurationProperties).ifPresent(targetConfiguration::setVariables);
    }

    // 设置 Configuration、objectWrapperFactory、vfs
    Optional.ofNullable(this.objectFactory).ifPresent(targetConfiguration::setObjectFactory);
    Optional.ofNullable(this.objectWrapperFactory).ifPresent(targetConfiguration::setObjectWrapperFactory);
    Optional.ofNullable(this.vfs).ifPresent(targetConfiguration::setVfsImpl);

    // 扫描包注册别名
    if (hasLength(this.typeAliasesPackage)) {
      // 切割一下包名
      String[] typeAliasPackageArray = tokenizeToStringArray(this.typeAliasesPackage,
          ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
      // 逐个扫描，并没有觉得stream更好看
      Stream.of(typeAliasPackageArray).forEach(packageToScan -> {
        targetConfiguration.getTypeAliasRegistry().registerAliases(packageToScan,
            typeAliasesSuperType == null ? Object.class : typeAliasesSuperType);
        LOGGER.debug("Scanned package: '" + packageToScan + "' for aliases");
      });
    }
    // 注册别名
    if (!isEmpty(this.typeAliases)) {
      Stream.of(this.typeAliases).forEach(typeAlias -> {
        targetConfiguration.getTypeAliasRegistry().registerAlias(typeAlias);
        LOGGER.debug( "Registered type alias: '" + typeAlias + "'");
      });
    }
    // 注册插件
    if (!isEmpty(this.plugins)) {
      Stream.of(this.plugins).forEach(plugin -> {
        targetConfiguration.addInterceptor(plugin);
        LOGGER.debug("Registered plugin: '" + plugin + "'");
      });
    }
    // 扫描包注册类型处理器
    if (hasLength(this.typeHandlersPackage)) {
      // 切割下包名
      String[] typeHandlersPackageArray = tokenizeToStringArray(this.typeHandlersPackage,
          ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
      Stream.of(typeHandlersPackageArray).forEach(packageToScan -> {
        targetConfiguration.getTypeHandlerRegistry().register(packageToScan);
        LOGGER.debug("Scanned package: '" + packageToScan + "' for type handlers");
      });
    }
    // 注册类型处理器
    if (!isEmpty(this.typeHandlers)) {
      Stream.of(this.typeHandlers).forEach(typeHandler -> {
        targetConfiguration.getTypeHandlerRegistry().register(typeHandler);
        LOGGER.debug("Registered type handler: '" + typeHandler + "'");
      });
    }
    // 添加缓存
    Optional.ofNullable(this.cache).ifPresent(targetConfiguration::addCache);

    // 真正开始解析，会覆盖上面的设置，不过一般我们不使用全局配置文件，直接设置Spring的属性
    if (xmlConfigBuilder != null) {
      try {
        xmlConfigBuilder.parse();
        LOGGER.debug("Parsed configuration file: '" + this.configLocation + "'");
      } catch (Exception ex) {
        throw new NestedIOException("Failed to parse config resource: " + this.configLocation, ex);
      } finally {
        ErrorContext.instance().reset();
      }
    }

    // 设置环境，包括 dataSource 和 transactionFactory，会覆盖 targetConfiguration 中的设置
    targetConfiguration.setEnvironment(new Environment(this.environment,
        this.transactionFactory == null ? new SpringManagedTransactionFactory() : this.transactionFactory,
        this.dataSource));

    // 逐个解析mapper.xml
    if (!isEmpty(this.mapperLocations)) {
      for (Resource mapperLocation : this.mapperLocations) {
        if (mapperLocation == null) {
          continue;
        }
        try {
          XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
              targetConfiguration, mapperLocation.toString(), targetConfiguration.getSqlFragments());
          xmlMapperBuilder.parse();
        } catch (Exception e) {
          throw new NestedIOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
        } finally {
          ErrorContext.instance().reset();
        }
        LOGGER.debug("Parsed mapper file: '" + mapperLocation + "'");
      }
    } else {
      LOGGER.debug("Property 'mapperLocations' was not specified or no matching resources found");
    }

    return this.sqlSessionFactoryBuilder.build(targetConfiguration);
  }

  /**
   * 获取 sqlSessionFactory
   */
  @Override
  public SqlSessionFactory getObject() throws Exception {
    if (this.sqlSessionFactory == null) {
      afterPropertiesSet();
    }

    return this.sqlSessionFactory;
  }

  /**
   * 获取 sqlSessionFactory 的类型
   */
  @Override
  public Class<? extends SqlSessionFactory> getObjectType() {
    return this.sqlSessionFactory == null ? SqlSessionFactory.class : this.sqlSessionFactory.getClass();
  }

  /**
   * 是否单例
   */
  @Override
  public boolean isSingleton() {
    return true;
  }

  /**
   * 监听容器刷新事件，会去 fail-fast 检查
   */
  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (failFast && event instanceof ContextRefreshedEvent) {
      // 检查所有的 MappedStatement 能否被加载
      this.sqlSessionFactory.getConfiguration().getMappedStatementNames();
    }
  }

}
