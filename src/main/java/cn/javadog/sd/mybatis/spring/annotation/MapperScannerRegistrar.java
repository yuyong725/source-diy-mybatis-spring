package cn.javadog.sd.mybatis.spring.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.javadog.sd.mybatis.spring.mapper.ClassPathMapperScanner;
import cn.javadog.sd.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author 余勇
 * @date 2019-12-21 17:33
 * 这是一个 {@link ImportBeanDefinitionRegistrar}，允许 MyBatis 通过注解上的配置去扫描需要注册的mapper。
 * 通过@Component配置的方式注册bean，而通过 {@code BeanDefinitionRegistryPostProcessor} 解析 XML 的配置。
 * 此方式通过 @Enable 注解的方式开启
 */
public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

  /**
   * ResourceLoader 对象，加载资源
   */
  private ResourceLoader resourceLoader;

  /**
   * 设置 resourceLoader，由aware注入
   */
  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * 注册 BeanDefinition
   */
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    // 获得 @MapperScan 注解信息
    AnnotationAttributes mapperScanAttrs = AnnotationAttributes
        .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));
    if (mapperScanAttrs != null) {
      // 根据注解上的规则，注册 BeanDefinition
      registerBeanDefinitions(mapperScanAttrs, registry);
    }
  }

  /**
   * 根据 @MapperScan 注解信息，注册 BeanDefinition
   */
  void registerBeanDefinitions(AnnotationAttributes annoAttrs, BeanDefinitionRegistry registry) {
    // 创建 ClassPathMapperScanner 对象
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);

    // 设置 resourceLoader，需要 Spring 3.1 的支持
    Optional.ofNullable(resourceLoader).ifPresent(scanner::setResourceLoader);
    // 拿到 annotationClass
    Class<? extends Annotation> annotationClass = annoAttrs.getClass("annotationClass");
    if (!Annotation.class.equals(annotationClass)) {
      scanner.setAnnotationClass(annotationClass);
    }
    // 拿到 markerInterface
    Class<?> markerInterface = annoAttrs.getClass("markerInterface");
    if (!Class.class.equals(markerInterface)) {
      scanner.setMarkerInterface(markerInterface);
    }
    // 拿到 nameGenerator
    Class<? extends BeanNameGenerator> generatorClass = annoAttrs.getClass("nameGenerator");
    if (!BeanNameGenerator.class.equals(generatorClass)) {
      scanner.setBeanNameGenerator(BeanUtils.instantiateClass(generatorClass));
    }
    // 拿到 factoryBean
    Class<? extends MapperFactoryBean> mapperFactoryBeanClass = annoAttrs.getClass("factoryBean");
    if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
      scanner.setMapperFactoryBeanClass(mapperFactoryBeanClass);
    }
    // 拿到 sqlSessionTemplateRef
    scanner.setSqlSessionTemplateBeanName(annoAttrs.getString("sqlSessionTemplateRef"));
    // 拿到 sqlSessionFactoryRef
    scanner.setSqlSessionFactoryBeanName(annoAttrs.getString("sqlSessionFactoryRef"));
    // 拿到 basePackages
    List<String> basePackages = new ArrayList<>();
    basePackages.addAll(
        Arrays.stream(annoAttrs.getStringArray("value"))
            .filter(StringUtils::hasText)
            .collect(Collectors.toList()));
    // 拿到 basePackages
    basePackages.addAll(
        Arrays.stream(annoAttrs.getStringArray("basePackages"))
            .filter(StringUtils::hasText)
            .collect(Collectors.toList()));
    // 拿到 basePackages，basePackageClasses的本质依然是通过类名保证包名的绝对正确
    basePackages.addAll(
        Arrays.stream(annoAttrs.getClassArray("basePackageClasses"))
            .map(ClassUtils::getPackageName)
            .collect(Collectors.toList()));
    // 注册 scanner 的过滤器
    scanner.registerFilters();
    // 开始扫描包
    scanner.doScan(StringUtils.toStringArray(basePackages));
  }

  /**
   * 针对 {@link MapperScans} 的 {@link MapperScannerRegistrar}
   */
  static class RepeatingRegistrar extends MapperScannerRegistrar {

    /**
     * 注册 BeanDefinition
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
        BeanDefinitionRegistry registry) {
      // 拿到 MapperScans 的所有属性，实际就一个 MapperScan
      AnnotationAttributes mapperScansAttrs = AnnotationAttributes
          .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScans.class.getName()));
      if (mapperScansAttrs != null) {
        Arrays.stream(mapperScansAttrs.getAnnotationArray("value"))
            // 遍历，调用 MapperScannerRegistrar 的注册逻辑
            .forEach(mapperScanAttrs -> registerBeanDefinitions(mapperScanAttrs, registry));
      }
    }
  }

}
