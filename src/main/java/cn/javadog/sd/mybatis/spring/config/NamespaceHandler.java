package cn.javadog.sd.mybatis.spring.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author 余勇
 * @date 2019-12-21 18:50
 * 继承 NamespaceHandlerSupport 抽象类，MyBatis 的 XML Namespace 的处理器
 */
public class NamespaceHandler extends NamespaceHandlerSupport {

  /**
   * 用于扫描xml 的 scan的内容
   * <mybatis:scan base-package="org.mybatis.spring.sample.mapper" />
   */
  @Override
  public void init() {
    registerBeanDefinitionParser("scan", new MapperScannerBeanDefinitionParser());
  }

}
