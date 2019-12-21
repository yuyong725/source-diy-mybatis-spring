package cn.javadog.sd.mybatis.spring.config;

import java.lang.annotation.Annotation;

import cn.javadog.sd.mybatis.spring.mapper.ClassPathMapperScanner;
import cn.javadog.sd.mybatis.spring.mapper.MapperFactoryBean;
import org.w3c.dom.Element;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * @author 余勇
 * @date 2019-12-21 18:53
 *
 * 实现 BeanDefinitionParser 接口，<mybatis:scan /> 的解析器
 */
public class MapperScannerBeanDefinitionParser implements BeanDefinitionParser {

  /*<mybatis:scan />支持的所有属性*/

  private static final String ATTRIBUTE_BASE_PACKAGE = "base-package";
  private static final String ATTRIBUTE_ANNOTATION = "annotation";
  private static final String ATTRIBUTE_MARKER_INTERFACE = "marker-interface";
  private static final String ATTRIBUTE_NAME_GENERATOR = "name-generator";
  private static final String ATTRIBUTE_TEMPLATE_REF = "template-ref";
  private static final String ATTRIBUTE_FACTORY_REF = "factory-ref";
  private static final String ATTRIBUTE_MAPPER_FACTORY_BEAN_CLASS = "mapper-factory-bean-class";

  /**
   * 解析xml，扫描包，注册mapper
   */
  @Override
  public synchronized BeanDefinition parse(Element element, ParserContext parserContext) {
    // 创建 ClassPathMapperScanner 对象
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(parserContext.getRegistry());
    ClassLoader classLoader = scanner.getResourceLoader().getClassLoader();
    XmlReaderContext readerContext = parserContext.getReaderContext();
    // 扫描仪设置 resourceLoader 属性
    scanner.setResourceLoader(readerContext.getResourceLoader());
    try {
      // 解析 annotation 属性
      String annotationClassName = element.getAttribute(ATTRIBUTE_ANNOTATION);
      if (StringUtils.hasText(annotationClassName)) {
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> markerInterface = (Class<? extends Annotation>) classLoader.loadClass(annotationClassName);
        scanner.setAnnotationClass(markerInterface);
      }
      // 解析 marker-interface 属性
      String markerInterfaceClassName = element.getAttribute(ATTRIBUTE_MARKER_INTERFACE);
      if (StringUtils.hasText(markerInterfaceClassName)) {
        Class<?> markerInterface = classLoader.loadClass(markerInterfaceClassName);
        scanner.setMarkerInterface(markerInterface);
      }
      // 解析 name-generator 属性
      String nameGeneratorClassName = element.getAttribute(ATTRIBUTE_NAME_GENERATOR);
      if (StringUtils.hasText(nameGeneratorClassName)) {
        Class<?> nameGeneratorClass = classLoader.loadClass(nameGeneratorClassName);
        BeanNameGenerator nameGenerator = BeanUtils.instantiateClass(nameGeneratorClass, BeanNameGenerator.class);
        scanner.setBeanNameGenerator(nameGenerator);
      }
      // 解析 mapper-factory-bean-class
      String mapperFactoryBeanClassName = element.getAttribute(ATTRIBUTE_MAPPER_FACTORY_BEAN_CLASS);
      if (StringUtils.hasText(mapperFactoryBeanClassName)) {
        @SuppressWarnings("unchecked")
        Class<? extends MapperFactoryBean> mapperFactoryBeanClass =
            (Class<? extends MapperFactoryBean>)classLoader.loadClass(mapperFactoryBeanClassName);
        scanner.setMapperFactoryBeanClass(mapperFactoryBeanClass);
      }
    } catch (Exception ex) {
      readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
    }
    // 解析 template-ref 属性
    String sqlSessionTemplateBeanName = element.getAttribute(ATTRIBUTE_TEMPLATE_REF);
    scanner.setSqlSessionTemplateBeanName(sqlSessionTemplateBeanName);
    // 解析 factory-ref 属性
    String sqlSessionFactoryBeanName = element.getAttribute(ATTRIBUTE_FACTORY_REF);
    scanner.setSqlSessionFactoryBeanName(sqlSessionFactoryBeanName);
    // 注册 scanner 的过滤器
    scanner.registerFilters();
    // 解析 base-package
    String basePackage = element.getAttribute(ATTRIBUTE_BASE_PACKAGE);
    // 执行扫描
    scanner.scan(StringUtils.tokenizeToStringArray(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    return null;
  }

}
