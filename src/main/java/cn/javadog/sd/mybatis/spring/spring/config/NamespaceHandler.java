package cn.javadog.sd.mybatis.spring.spring.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Namespace handler for the MyBatis namespace.
 *
 * @author Lishu Luo
 *
 * @see MapperScannerBeanDefinitionParser
 * @since 1.2.0
 */
public class NamespaceHandler extends NamespaceHandlerSupport {

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() {
    registerBeanDefinitionParser("scan", new MapperScannerBeanDefinitionParser());
  }

}
