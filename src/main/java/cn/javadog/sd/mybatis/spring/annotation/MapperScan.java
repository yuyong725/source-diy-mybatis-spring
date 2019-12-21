package cn.javadog.sd.mybatis.spring.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.spring.mapper.MapperFactoryBean;
import cn.javadog.sd.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.Import;

/**
 * @author 余勇
 * @date 2019-12-21 17:07
 *
 * 指定需要扫描的包，将包中符合的 Mapper 接口，
 * 注册成 beanClass 为 MapperFactoryBean 的 BeanDefinition 对象，从而实现创建 Mapper 对象。
 *
 * 当我们使用java配置的方式操作mybatis时，就是通过该注解注册 mapper。当
 * {@link MapperScannerConfigurer} 通过  {@link MapperScannerRegistrar} 配置mapper时，就会调用此类
 *
 * <p>Configuration example:</p>
 * <pre class="code">
 * @Configuration
 * @MapperScan("org.mybatis.spring.sample.mapper")
 * public class AppConfig {
 *
 *   @Bean
 *   public DataSource dataSource() {
 *     return new EmbeddedDatabaseBuilder()
 *              .addScript("schema.sql")
 *              .build();
 *   }
 *
 *   @Bean
 *   public DataSourceTransactionManager transactionManager() {
 *     return new DataSourceTransactionManager(dataSource());
 *   }
 *
 *   @Bean
 *   public SqlSessionFactory sqlSessionFactory() throws Exception {
 *     SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
 *     sessionFactory.setDataSource(dataSource());
 *     return sessionFactory.getObject();
 *   }
 * }
 * </pre>
 *
 * @see MapperScannerRegistrar
 * @see MapperFactoryBean
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MapperScannerRegistrar.class)
@Repeatable(MapperScans.class)
public @interface MapperScan {

  /**
   * 可以直接使用 {@code @MapperScan("org.my.pkg")} 代替 {@code @MapperScan(basePackages = "org.my.pkg"})}.
   *
   * 和 {@link #basePackages()} 相同意思
   */
  String[] value() default {};

  /**
   * 扫描的包地址
   */
  String[] basePackages() default {};

  /**
   * 可以指定多个类或接口的class,扫描时会 在这些指定的类和接口所属的包进行扫面。建议指定下 markerInterface，免得瞎扫
   */
  Class<?>[] basePackageClasses() default {};

  /**
   * 用于命名被扫描到的mapper
   */
  Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

  /**
   * 指定要被扫描的类需要有什么注解，可以与 markerInterface 联合使用
   * <p>
   *     扫描器会注册 {@link #basePackages()} 下所有含有此注解的接口
   * <p>
   *
   */
  Class<? extends Annotation> annotationClass() default Annotation.class;

  /**
   * 指定要被扫描的类需要继承什么接口，可以与 annotationClass 联合使用
   * <p>
   *     扫描器会注册 {@link #basePackages()} 下所有继承接口的接口
   * <p>
   */
  Class<?> markerInterface() default Class.class;

  /**
   * 当spring容器中有多个 {@code SqlSessionTemplate} 时，指定使用哪一个，通常是有多个数据源时，才需要指定
   */
  String sqlSessionTemplateRef() default "";

  /**
   * 当spring容器中有多个 {@code SqlSessionFactory} 时，指定使用哪一个，通常是有多个数据源时，才需要指定
   */
  String sqlSessionFactoryRef() default "";

  /**
   * 可自定义 MapperFactoryBean 的实现类
   */
  Class<? extends MapperFactoryBean> factoryBean() default MapperFactoryBean.class;

}
