package cn.javadog.sd.mybatis.spring;

import static cn.javadog.sd.mybatis.support.util.ExceptionUtil.unwrapThrowable;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.springframework.util.Assert.notNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.BatchResult;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.support.exceptions.PersistenceException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * @author 余勇
 * @date 2019-12-21 20:14
 *
 * @see SqlSessionFactory
 * @see MyBatisExceptionTranslator
 *
 * 线程安全，由Spring管理的 {@code SqlSession}。
 * Spring的事务管理，确保实际使用的SqlSession与Spring的事务保持一致。
 * 除此之外，它还管理了会话的生命周期，包括 关闭、提交、回滚等，这些都是基于Spring的事务配置
 * <p>
 * 这个模板需要一个 SqlSessionFactory 来创建 SqlSession，通过方法参数的方式的传递给构造方法就行了。
 * 当然，构造时你也可以指定执行器的类型，不指定的会，就使用会话工厂默认的配置
 * <p>
 * 这个模板会将mybatis 的 PersistenceExceptions 转换成 DataAccessExceptions，转换工具默认使用的是 {@code MyBatisExceptionTranslator}.
 * <p>
 * 由于 SqlSessionTemplate 是线程安全的，因此所有的DAO，也就是mapper可以共享这一个单例，这也能节约不少内存
 *
 * demo走一个：
 *
 * <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
 *   <constructor-arg ref="sqlSessionFactory" />
 * </bean>
 *
 */
public class SqlSessionTemplate implements SqlSession, DisposableBean {

  /**
   * 会话工厂
   */
  private final SqlSessionFactory sqlSessionFactory;

  /**
   * 执行器类型
   */
  private final ExecutorType executorType;

  /**
   * 委托对象
   */
  private final SqlSession sqlSessionProxy;

  /**
   * 异常转换器
   */
  private final PersistenceExceptionTranslator exceptionTranslator;

  /**
   * 构造方法
   */
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
  }

  /**
   * 构造方法
   */
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
    this(sqlSessionFactory, executorType,
        new MyBatisExceptionTranslator(
            sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(), true));
  }

  /**
   * 构造，exceptionTranslator 可以为null，到时会将mybatis的异常直接丢出来
   */
  public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
      PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
    notNull(executorType, "Property 'executorType' is required");

    this.sqlSessionFactory = sqlSessionFactory;
    this.executorType = executorType;
    this.exceptionTranslator = exceptionTranslator;
    // 这忒么是实打实的代理类，不想mybatis里面闹着玩的用DefaultSqlSession
    this.sqlSessionProxy = (SqlSession) newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[] { SqlSession.class },
        new SqlSessionInterceptor());
  }

  /*几个属性的get*/

  public SqlSessionFactory getSqlSessionFactory() {
    return this.sqlSessionFactory;
  }

  public ExecutorType getExecutorType() {
    return this.executorType;
  }

  public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
    return this.exceptionTranslator;
  }

  /*所有数据库的操作交给sqlSessionProxy去完成*/

  @Override
  public <T> T selectOne(String statement) {
    return this.sqlSessionProxy.selectOne(statement);
  }

  @Override
  public <T> T selectOne(String statement, Object parameter) {
    return this.sqlSessionProxy.selectOne(statement, parameter);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return this.sqlSessionProxy.selectMap(statement, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return this.sqlSessionProxy.selectMap(statement, parameter, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return this.sqlSessionProxy.selectMap(statement, parameter, mapKey, rowBounds);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement) {
    return this.sqlSessionProxy.selectCursor(statement);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return this.sqlSessionProxy.selectCursor(statement, parameter);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    return this.sqlSessionProxy.selectCursor(statement, parameter, rowBounds);
  }

  @Override
  public <E> List<E> selectList(String statement) {
    return this.sqlSessionProxy.selectList(statement);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return this.sqlSessionProxy.selectList(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return this.sqlSessionProxy.selectList(statement, parameter, rowBounds);
  }

  @Override
  public void select(String statement, ResultHandler handler) {
    this.sqlSessionProxy.select(statement, handler);
  }

  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    this.sqlSessionProxy.select(statement, parameter, handler);
  }

  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    this.sqlSessionProxy.select(statement, parameter, rowBounds, handler);
  }

  @Override
  public int insert(String statement) {
    return this.sqlSessionProxy.insert(statement);
  }

  @Override
  public int insert(String statement, Object parameter) {
    return this.sqlSessionProxy.insert(statement, parameter);
  }

  @Override
  public int update(String statement) {
    return this.sqlSessionProxy.update(statement);
  }

  @Override
  public int update(String statement, Object parameter) {
    return this.sqlSessionProxy.update(statement, parameter);
  }

  @Override
  public int delete(String statement) {
    return this.sqlSessionProxy.delete(statement);
  }

  @Override
  public int delete(String statement, Object parameter) {
    return this.sqlSessionProxy.delete(statement, parameter);
  }

  @Override
  public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
  }

  /*事务操作直接GG，也即是说不允许直接调用*/

  @Override
  public void commit() {
    throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
  }

  @Override
  public void commit(boolean force) {
    throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
  }

  @Override
  public void rollback() {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
  }

  @Override
  public void rollback(boolean force) {
    throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("Manual close is not allowed over a Spring managed SqlSession");
  }

  /**
   * 情况缓存
   */
  @Override
  public void clearCache() {
    this.sqlSessionProxy.clearCache();
  }

  /**
   * 拿到全局配置
   */
  @Override
  public Configuration getConfiguration() {
    return this.sqlSessionFactory.getConfiguration();
  }

  /**
   * 拿到连接对象
   */
  @Override
  public Connection getConnection() {
    return this.sqlSessionProxy.getConnection();
  }

  /**
   * 输入批处理
   * @since 1.0.2
   */
  @Override
  public List<BatchResult> flushStatements() {
    return this.sqlSessionProxy.flushStatements();
  }

 /**
  * 生命周期结束时调用
  * <bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
  *  <constructor-arg index="0" ref="sqlSessionFactory" />
  * </bean>
  *
  * 这个实现会强制Spring上下文使用{@link DisposableBean#destroy()}方法代替{@link SqlSessionTemplate#close()}，更加优雅的关闭会话
  *  @see SqlSessionTemplate#close()
  * @see "org.springframework.beans.factory.support.DisposableBeanAdapter#inferDestroyMethodIfNecessary(Object, RootBeanDefinition)"
  * @see "org.springframework.beans.factory.support.DisposableBeanAdapter#CLOSE_METHOD_NAME"
  */
  @Override
  public void destroy() {
    // 避免调用 SqlSessionTemplate.close()，从而抛出 UnsupportedOperationException，其实啥也没干，只是不让人家close
  }

  /**
   * SqlSessionInterceptor ，是 SqlSessionTemplate 的内部类，实现 InvocationHandler 接口，将 SqlSession 的操作，路由到 Spring 托管的事务管理器中。
   * 遇到异常时，会小小的包装一下
   */
  private class SqlSessionInterceptor implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // 获得 SqlSession 对象，此处，和 Spring 事务托管的事务已经相关。
      SqlSession sqlSession = SqlSessionUtils.getSqlSession(
          SqlSessionTemplate.this.sqlSessionFactory,
          SqlSessionTemplate.this.executorType,
          SqlSessionTemplate.this.exceptionTranslator);
      try {
        // 执行 SQL 操作
        Object result = method.invoke(sqlSession, args);
        // 如果非 Spring 托管的 SqlSession 对象，则提交事务
        if (!SqlSessionUtils.isSqlSessionTransactional(sqlSession, SqlSessionTemplate.this.sqlSessionFactory)) {
          // 强制提交，哪怕是一个干净的会话(也就是没有对数据库的更新操作), 因为某些数据库要求会话在关闭之前，必须调用 commit/rollback
          sqlSession.commit(true);
        }
        return result;
      } catch (Throwable t) {
        Throwable unwrapped = unwrapThrowable(t);
        // 如果是 PersistenceException 异常，则进行转换
        if (SqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
          // 如果异常翻译器没有加载，就直接释放连接，避免死锁，TODO 这个this是个啥
          // 根据情况，关闭 SqlSession 对象
          // 如果非 Spring 托管的 SqlSession 对象，则关闭 SqlSession 对象
          // 如果是 Spring 托管的 SqlSession 对象，则减少其 SqlSessionHolder 的计数。也就是说，Spring 托管事务的情况下，最终是在“外部”执行最终的事务处理。
          SqlSessionUtils.closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
          // 置空，避免下面 finally 又做处理
          sqlSession = null;
          // 进行转换
          Throwable translated = SqlSessionTemplate.this.exceptionTranslator.translateExceptionIfPossible((PersistenceException) unwrapped);
          if (translated != null) {
            unwrapped = translated;
          }
        }
        throw unwrapped;
      } finally {
        if (sqlSession != null) {
          SqlSessionUtils.closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
        }
      }
    }
  }

}
