package cn.javadog.sd.mybatis.spring.mapper;

import static org.springframework.util.Assert.notNull;

import java.lang.annotation.Annotation;
import java.util.Map;

import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 *
 * @author 余勇
 * @date 2019-12-21 19:36
 *
 * 实现 BeanDefinitionRegistryPostProcessor、InitializingBean、ApplicationContextAware、BeanNameAware 接口，定义需要扫描的包，
 * 将包中符合的 Mapper 接口，注册成 beanClass 为 MapperFactoryBean 的 BeanDefinition 对象，从而实现创建 Mapper 对象。
 *
 * Configuration sample:
 *
 * <pre class="code">
 * {@code
 *   <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *       <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
 *       <!-- optional unless there are multiple session factories defined -->
 *       <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre>
 *
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 */
public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

  /**
   * 要扫描的包
   */
  private String basePackage;

  /**
   * 是否将扫描的mapper注册进去
   */
  private boolean addToConfig = true;

  /**
   * sql会话工厂
   */
  private SqlSessionFactory sqlSessionFactory;

  /**
   * SQL会话模板
   */
  private SqlSessionTemplate sqlSessionTemplate;

  /**
   * SQL会话工厂的bean的名称
   */
  private String sqlSessionFactoryBeanName;

  /**
   * sql会话模板的bean的名称
   */
  private String sqlSessionTemplateBeanName;

  /**
   * 扫描的mapper必须修饰的注解
   */
  private Class<? extends Annotation> annotationClass;

  /**
   * 扫描的mapper必须继承的接口
   */
  private Class<?> markerInterface;

  /**
   * mapper工厂bean实现类
   */
  private Class<? extends MapperFactoryBean> mapperFactoryBeanClass;

  /**
   * Spring容器上下文
   */
  private ApplicationContext applicationContext;

  /**
   * 当前bean的名称
   */
  private String beanName;

  /**
   * 是否处理属性的占位符
   */
  private boolean processPropertyPlaceHolders;

  /**
   * bean的名称生成器
   */
  private BeanNameGenerator nameGenerator;

  public void setBasePackage(String basePackage) {
    this.basePackage = basePackage;
  }

  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  public void setMarkerInterface(Class<?> superClass) {
    this.markerInterface = superClass;
  }

  /**
   * 当容器中有多个{@code SqlSessionTemplate}实例时(通常这种情况发生在有多个数据库)，指定使用哪一个实例
   *
   * @deprecated 已废弃 {@link #setSqlSessionTemplateBeanName(String)} 代替
   */
  @Deprecated
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  /**
   * 当容器中有多个{@code SqlSessionTemplate}实例时(通常这种情况发生在有多个数据库)，指定使用哪一个实例
   *
   * Note 使用beanName而不是bean实例的引用，这是因为在启动过程中，扫描仪很早就加载了，相关mybatis的实例还没有来得及构建
   *
   * @since 1.1.0
   *
   */
  public void setSqlSessionTemplateBeanName(String sqlSessionTemplateName) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateName;
  }

  /**
   * 当容器中有多个{@code SqlSessionFactory}实例时(通常这种情况发生在有多个数据库)，指定使用哪一个实例
   *
   * @deprecated 使用 {@link #setSqlSessionFactoryBeanName(String)} 代替.
   */
  @Deprecated
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  /**
   * 当容器中有多个{@code SqlSessionFactory}实例时(通常这种情况发生在有多个数据库)，指定使用哪一个实例
   *
   * Note 使用beanName而不是bean实例的引用，这是因为在启动过程中，扫描仪很早就加载了，相关mybatis的实例还没有来得及构建
   *
   * @since 1.1.0
   */
  public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
  }

  /**
   * 标记是否处理属性的占位符，默认为false，意味着不处理属性的占位符
   * @since 1.1.1
   *
   * @param processPropertyPlaceHolders a flag that whether execute a property placeholder processing or not
   */
  public void setProcessPropertyPlaceHolders(boolean processPropertyPlaceHolders) {
    this.processPropertyPlaceHolders = processPropertyPlaceHolders;
  }

  public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
    this.mapperFactoryBeanClass = mapperFactoryBeanClass;
  }

  /**
   * 通过aware注入
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * 通过aware注入
   */
  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  /**
   * 允许扫描仪时，回去bean名称生成器
   *
   * @since 1.2.0
   */
  public BeanNameGenerator getNameGenerator() {
    return nameGenerator;
  }

  public void setNameGenerator(BeanNameGenerator nameGenerator) {
    this.nameGenerator = nameGenerator;
  }

  /**
   * 构造函数完成后执行，主要是校验 basePackage
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    notNull(this.basePackage, "Property 'basePackage' is required");
  }

  /**
   * 未实现
   */
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  }

  /**
   * 务必了解下 BeanFactoryPostProcessor 执行时间
   * 
   * @since 1.0.2
   */
  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    // 如果有属性占位符，则进行获得，例如 ${basePackage} 等等
    if (this.processPropertyPlaceHolders) {
      processPropertyPlaceHolders();
    }

    // 创建 ClassPathMapperScanner 对象，并设置其相关属性
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
    scanner.setAddToConfig(this.addToConfig);
    scanner.setAnnotationClass(this.annotationClass);
    scanner.setMarkerInterface(this.markerInterface);
    scanner.setSqlSessionFactory(this.sqlSessionFactory);
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
    scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
    scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
    scanner.setResourceLoader(this.applicationContext);
    scanner.setBeanNameGenerator(this.nameGenerator);
    scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
    // 注册 scanner 过滤器
    scanner.registerFilters();
    // 执行扫描
    scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  }

  /**
   * 容器启动过程中，BeanDefinitionRegistries 很早就调用了，在 BeanFactoryPostProcessors 调用之前。
   * 这意味着 PropertyResourceConfigurers 还没有加载，并且这个类的所有属性的替换将会失败。
   * 为了避免这个，找到容器中所有 PropertyResourceConfigurer 的实例，当此类记载完成bean注册后，再去更新占位符的值。
   */
  private void processPropertyPlaceHolders() {
    // 拿到所有的 PropertyResourceConfigurer
    Map<String, PropertyResourceConfigurer> prcs = applicationContext.getBeansOfType(PropertyResourceConfigurer.class);

    if (!prcs.isEmpty() && applicationContext instanceof ConfigurableApplicationContext) {
      // 拿到当前类的类定义
      BeanDefinition mapperScannerBean = ((ConfigurableApplicationContext) applicationContext)
          .getBeanFactory().getBeanDefinition(beanName);

      // PropertyResourceConfigurer 不暴露任何方法来执行属性值的占位符替换。而是创建一个包含mapper扫描仪的 BeanFactory，然后处理这个BeanFactory
      DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
      // 注册当前扫描仪
      factory.registerBeanDefinition(beanName, mapperScannerBean);
      // 处理每一个属性
      for (PropertyResourceConfigurer prc : prcs.values()) {
        prc.postProcessBeanFactory(factory);
      }
      // 获取处理后的值
      PropertyValues values = mapperScannerBean.getPropertyValues();
      // 解析每一个属性值赋给当前对象
      this.basePackage = updatePropertyValue("basePackage", values);
      this.sqlSessionFactoryBeanName = updatePropertyValue("sqlSessionFactoryBeanName", values);
      this.sqlSessionTemplateBeanName = updatePropertyValue("sqlSessionTemplateBeanName", values);
    }
  }

  /**
   * 获得属性值，并转换成 String 类型
   */
  private String updatePropertyValue(String propertyName, PropertyValues values) {
    PropertyValue property = values.getPropertyValue(propertyName);

    if (property == null) {
      return null;
    }

    Object value = property.getValue();

    if (value == null) {
      return null;
    } else if (value instanceof String) {
      return value.toString();
    } else if (value instanceof TypedStringValue) {
      return ((TypedStringValue) value).getValue();
    } else {
      return null;
    }
  }

}
