package cn.javadog.sd.mybatis.spring.spring.config;

import java.lang.annotation.Annotation;

import cn.javadog.sd.mybatis.spring.spring.mapper.ClassPathMapperScanner;
import cn.javadog.sd.mybatis.spring.spring.mapper.MapperFactoryBean;
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
 * A {#code BeanDefinitionParser} that handles the element scan of the MyBatis.
 * namespace
 * 
 * @author Lishu Luo
 * @author Eduardo Macarron
 *
 * @since 1.2.0
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 */

public class MapperScannerBeanDefinitionParser implements BeanDefinitionParser {

  private static final String ATTRIBUTE_BASE_PACKAGE = "base-package";
  private static final String ATTRIBUTE_ANNOTATION = "annotation";
  private static final String ATTRIBUTE_MARKER_INTERFACE = "marker-interface";
  private static final String ATTRIBUTE_NAME_GENERATOR = "name-generator";
  private static final String ATTRIBUTE_TEMPLATE_REF = "template-ref";
  private static final String ATTRIBUTE_FACTORY_REF = "factory-ref";
  private static final String ATTRIBUTE_MAPPER_FACTORY_BEAN_CLASS = "mapper-factory-bean-class";

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized BeanDefinition parse(Element element, ParserContext parserContext) {
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(parserContext.getRegistry());
    ClassLoader classLoader = scanner.getResourceLoader().getClassLoader();
    XmlReaderContext readerContext = parserContext.getReaderContext();
    scanner.setResourceLoader(readerContext.getResourceLoader());
    try {
      String annotationClassName = element.getAttribute(ATTRIBUTE_ANNOTATION);
      if (StringUtils.hasText(annotationClassName)) {
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> markerInterface = (Class<? extends Annotation>) classLoader.loadClass(annotationClassName);
        scanner.setAnnotationClass(markerInterface);
      }
      String markerInterfaceClassName = element.getAttribute(ATTRIBUTE_MARKER_INTERFACE);
      if (StringUtils.hasText(markerInterfaceClassName)) {
        Class<?> markerInterface = classLoader.loadClass(markerInterfaceClassName);
        scanner.setMarkerInterface(markerInterface);
      }
      String nameGeneratorClassName = element.getAttribute(ATTRIBUTE_NAME_GENERATOR);
      if (StringUtils.hasText(nameGeneratorClassName)) {
        Class<?> nameGeneratorClass = classLoader.loadClass(nameGeneratorClassName);
        BeanNameGenerator nameGenerator = BeanUtils.instantiateClass(nameGeneratorClass, BeanNameGenerator.class);
        scanner.setBeanNameGenerator(nameGenerator);
      }
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
    String sqlSessionTemplateBeanName = element.getAttribute(ATTRIBUTE_TEMPLATE_REF);
    scanner.setSqlSessionTemplateBeanName(sqlSessionTemplateBeanName);
    String sqlSessionFactoryBeanName = element.getAttribute(ATTRIBUTE_FACTORY_REF);
    scanner.setSqlSessionFactoryBeanName(sqlSessionFactoryBeanName);
    scanner.registerFilters();
    String basePackage = element.getAttribute(ATTRIBUTE_BASE_PACKAGE);
    scanner.scan(StringUtils.tokenizeToStringArray(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    return null;
  }

}
