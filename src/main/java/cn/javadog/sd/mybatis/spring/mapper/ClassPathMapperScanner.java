package cn.javadog.sd.mybatis.spring.mapper;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.SqlSessionTemplate;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;

/**
 * @author 余勇
 * @date 2019-12-21 17:52
 *
 * 通过 {@code basePackage}, {@code annotationClass},  {@code markerInterface} 三种方式注册mapper。
 * 如果指定了 {@code annotationClass} 和/或 {@code markerInterface} ，会同时生效。
 * 此类之前是{@link MapperScannerConfigurer}的似有类，1.2.0版本之后移出来了
 */
public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner {

  /**
   * 日志打印器
   */
  private static final Log LOGGER = LogFactory.getLog(ClassPathMapperScanner.class);

  /**
   *是否将扫描到的mapper添加到全局配置，默认true
   */
  private boolean addToConfig = true;

  /**
   * SQL会话工厂
   */
  private SqlSessionFactory sqlSessionFactory;

  /**
   * SQL会话模板
   */
  private SqlSessionTemplate sqlSessionTemplate;

  /**
   * SQL会话模板的bean的名字
   */
  private String sqlSessionTemplateBeanName;

  /**
   * SQL会话工厂的bean的名字
   */
  private String sqlSessionFactoryBeanName;

  /**
   * 要扫描的mapper限制条件：需要加的注解
   */
  private Class<? extends Annotation> annotationClass;

  /**
   * 要扫描的mapper限制条件：需要继承的接口
   */
  private Class<?> markerInterface;

  /**
   * mapper工厂bean实现类
   */
  private Class<? extends MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;

  /**
   * 构造函数
   */
  public ClassPathMapperScanner(BeanDefinitionRegistry registry) {
    super(registry, false);
  }

  /*所有属性的set方法*/

  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  public void setMarkerInterface(Class<?> markerInterface) {
    this.markerInterface = markerInterface;
  }

  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  public void setSqlSessionTemplateBeanName(String sqlSessionTemplateBeanName) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateBeanName;
  }

  public void setSqlSessionFactoryBeanName(String sqlSessionFactoryBeanName) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName;
  }

  /**
   * 2.0.1版本后废弃了, 请使用 {@link #setMapperFactoryBeanClass(Class)}.
   */
  @Deprecated
  public void setMapperFactoryBean(MapperFactoryBean<?> mapperFactoryBean) {
    this.mapperFactoryBeanClass = mapperFactoryBean == null ? MapperFactoryBean.class : mapperFactoryBean.getClass();
  }

  public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
    this.mapperFactoryBeanClass = mapperFactoryBeanClass == null ? MapperFactoryBean.class : mapperFactoryBeanClass;
  }

  /**
   * 注册过滤器。
   * 配置父扫描仪去扫描合适的mapper接口，它可以扫描所有的接口，也可以扫描那些继承了指定接口的接口，或者标记指定注解的接口。
   * 就是根据👆的一些属性条件，配置过滤器的规则
   */
  public void registerFilters() {
    // 标记接受所有接口
    boolean acceptAllInterfaces = true;

    // 如果指定了注解，则添加 INCLUDE 过滤器 AnnotationTypeFilter 对象
    if (this.annotationClass != null) {
      addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
      // 不再接受所有接口
      acceptAllInterfaces = false;
    }

    // 如果指定了接口，则添加 INCLUDE 过滤器 AssignableTypeFilter 对象
    if (this.markerInterface != null) {
      // AssignableTypeFilter匿名实现类，忽略掉类型名称匹配的
      addIncludeFilter(new AssignableTypeFilter(this.markerInterface) {
        @Override
        protected boolean matchClassName(String className) {
          return false;
        }
      });
      // 不再接受所有接口
      acceptAllInterfaces = false;
    }

    // 如果接受所有接口，则添加自定义 INCLUDE 过滤器 TypeFilter ，全部返回 true
    if (acceptAllInterfaces) {
      // 扫描所有接口
      addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    }

    // 添加 INCLUDE 过滤器，排除 package-info.java
    addExcludeFilter((metadataReader, metadataReaderFactory) -> {
      String className = metadataReader.getClassMetadata().getClassName();
      return className.endsWith("package-info");
    });
  }

  /**
   * 调用父类扫描仪执行包扫描，将扫描到的 Mapper 接口，注册成 beanClass 为 MapperFactoryBean 的 BeanDefinition 对象。
   */
  @Override
  public Set<BeanDefinitionHolder> doScan(String... basePackages) {
    // 执行扫描，获得包下符合的类们，并分装成 BeanDefinitionHolder 对象的集合
    Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

    if (beanDefinitions.isEmpty()) {
      LOGGER.warn("No MyBatis mapper was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
    } else {
      // 处理 BeanDefinitionHolder 对象的集合
      processBeanDefinitions(beanDefinitions);
    }

    return beanDefinitions;
  }

  /**
   * 处理 BeanDefinitionHolder 对象的集合
   */
  private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
    GenericBeanDefinition definition;
    // 遍历
    for (BeanDefinitionHolder holder : beanDefinitions) {
      definition = (GenericBeanDefinition) holder.getBeanDefinition();
      // 获取bean的名称
      String beanClassName = definition.getBeanClassName();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Creating MapperFactoryBean with name '" + holder.getBeanName()
            + "' and '" + beanClassName + "' mapperInterface");
      }

      definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
      // mapper 接口是bean原始的类型，实际类型是 MapperFactoryBean
      definition.setBeanClass(this.mapperFactoryBeanClass);
      // 设置 addToConfig 属性
      definition.getPropertyValues().add("addToConfig", this.addToConfig);
      // 是否已经显式设置了 sqlSessionFactoryBeanName 或 sqlSessionFactory 属性
      boolean explicitFactoryUsed = false;
      if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
        // 设置 sqlSessionFactoryBeanName 属性
        definition.getPropertyValues().add("sqlSessionFactory", new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
        explicitFactoryUsed = true;
      } else if (this.sqlSessionFactory != null) {
        // 设置 sqlSessionFactory 属性
        definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
        explicitFactoryUsed = true;
      }

      // 如果 sqlSessionTemplateBeanName 或 sqlSessionTemplate 非空，设置到 `MapperFactoryBean.sqlSessionTemplate` 属性
      if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
        if (explicitFactoryUsed) {
          LOGGER.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
        }
        definition.getPropertyValues().add("sqlSessionTemplate", new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
        explicitFactoryUsed = true;
      } else if (this.sqlSessionTemplate != null) {
        if (explicitFactoryUsed) {
          LOGGER.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
        }
        definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
        explicitFactoryUsed = true;
      }

      // 如果未显式设置，则设置根据类型自动注入
      if (!explicitFactoryUsed) {
        LOGGER.debug("Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
    if (super.checkCandidate(beanName, beanDefinition)) {
      return true;
    } else {
      LOGGER.warn("Skipping MapperFactoryBean with name '" + beanName
          + "' and '" + beanDefinition.getBeanClassName() + "' mapperInterface"
          + ". Bean already defined with the same name!");
      return false;
    }
  }

}
