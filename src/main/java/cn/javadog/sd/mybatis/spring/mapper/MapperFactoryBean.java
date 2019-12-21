package cn.javadog.sd.mybatis.spring.mapper;

import static org.springframework.util.Assert.notNull;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.spring.support.SqlSessionDaoSupport;
import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author 余勇
 * @date 2019-12-21 16:41
 *
 * BeanFactory 用于注入MyBatis 的 mapper。SqlSessionFactory 和预先配置的 SqlSessionTemplate 都可以拿到它。
 * <p>
 * 简单的配置实例:
 *
 * <pre class="code">
 * {@code
 *   <bean id="baseMapper" class="org.mybatis.spring.mapper.MapperFactoryBean" abstract="true" lazy-init="true">
 *     <property name="sqlSessionFactory" ref="sqlSessionFactory" />
 *   </bean>
 *
 *   <bean id="oneMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyMapperInterface" />
 *   </bean>
 *
 *   <bean id="anotherMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyAnotherMapperInterface" />
 *   </bean>
 * }
 * </pre>
 * <p>
 *
 * 这个工厂只能通过注入接口，不能注入具体的类
 */
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {

  /**
   * mapper 接口，必须是接口
   */
  private Class<T> mapperInterface;

  /**
   * 是否注册到Config，当然要注册
   */
  private boolean addToConfig = true;

  /**
   * 空构造
   */
  public MapperFactoryBean() {
  }

  /**
   * 构造
   */
  public MapperFactoryBean(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  /**
   * 注册 mapper
   * 该方法，是在 org.springframework.dao.support.DaoSupport 定义，被 #afterPropertiesSet() 方法所调用
   */
  @Override
  protected void checkDaoConfig() {
    super.checkDaoConfig();
    notNull(this.mapperInterface, "Property 'mapperInterface' is required");
    Configuration configuration = getSqlSession().getConfiguration();
    if (this.addToConfig && !configuration.hasMapper(this.mapperInterface)) {
      try {
        // 注册
        configuration.addMapper(this.mapperInterface);
      } catch (Exception e) {
        logger.error("Error while adding the mapper '" + this.mapperInterface + "' to configuration.", e);
        throw new IllegalArgumentException(e);
      } finally {
        ErrorContext.instance().reset();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getObject() throws Exception {
    return getSqlSession().getMapper(this.mapperInterface);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getObjectType() {
    return this.mapperInterface;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSingleton() {
    return true;
  }

  //------------- 本类相对父类添加的属性的get/set --------------

  public void setMapperInterface(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  public boolean isAddToConfig() {
    return addToConfig;
  }
}
