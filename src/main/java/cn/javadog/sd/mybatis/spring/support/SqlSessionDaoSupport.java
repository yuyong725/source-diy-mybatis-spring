package cn.javadog.sd.mybatis.spring.support;

import static org.springframework.util.Assert.notNull;

import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.SqlSessionTemplate;
import org.springframework.dao.support.DaoSupport;

/**
 * @author 余勇
 * @date 2019-12-21 21:14
 *
 * @see #setSqlSessionFactory
 * @see #setSqlSessionTemplate
 * @see SqlSessionTemplate
 *
 * 继承 DaoSupport 抽象类，SqlSession 的 DaoSupport 抽象类。
 * 通过此类可以操作sqlSessionTemplate，进而操作数据库
 */
public abstract class SqlSessionDaoSupport extends DaoSupport {

  /**
   * SQL会话模板
   */
  private SqlSessionTemplate sqlSessionTemplate;

  /**
   * 设置 SqlSessionFactory，然后使用该工厂创建 sqlSessionTemplate
   */
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    if (this.sqlSessionTemplate == null || sqlSessionFactory != this.sqlSessionTemplate.getSqlSessionFactory()) {
      this.sqlSessionTemplate = createSqlSessionTemplate(sqlSessionFactory);
    }
  }

  /**
   * 通过 SqlSessionFactory 创建 SqlSessionTemplate。
   * 只有当给DAO设置SqlSessionFactory时，才会调用此方法。
   * 如果子类使用其他的方式提供 SqlSessionTemplate 实例，就会覆盖这个实例
   */
  @SuppressWarnings("WeakerAccess")
  protected SqlSessionTemplate createSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionTemplate(sqlSessionFactory);
  }

  /**
   * 获取 SqlSessionFactory
   */
  public final SqlSessionFactory getSqlSessionFactory() {
    return (this.sqlSessionTemplate != null ? this.sqlSessionTemplate.getSqlSessionFactory() : null);
  }


  /**
   * 显式的设置 sqlSessionTemplate，替代 setSqlSessionFactory 创建的
   */
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  /**
   * 获取 sqlSessionTemplate。
   * 程序员应该使用此方法获取 SqlSession 去调用操作 statement 的方法。这个 SqlSession 由Spring管理，
   * 码农们不应该手动的去调用 commit/rollback/close 方法，因为Spring会自动完成相关操作
   */
  public SqlSession getSqlSession() {
    return this.sqlSessionTemplate;
  }

  /**
   * 获取当前DAO的 SqlSessionTemplate，该值来自于 SessionFactory 创建，或者显式的赋值
   * note 返回的SQL会话模板是个共享的单例。
   * 关于内省：https://www.jianshu.com/p/604d411067c8
   * 你可以内省它的配置，但切勿去修改。(顶多在{@link #initDao}方法去更改)
   *
   * 如果你有权限去自定义Spring生成的实例的配置，可以考虑自定义一个 SqlSessionTemplate 实例，通过
   * {@code new SqlSessionTemplate(getSqlSessionFactory())} 的方式
   */
  public SqlSessionTemplate getSqlSessionTemplate() {
    return this.sqlSessionTemplate;
  }

  /**
   * 检查 sqlSessionTemplate 是否注入
   */
  @Override
  protected void checkDaoConfig() {
    notNull(this.sqlSessionTemplate, "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required");
  }

}
