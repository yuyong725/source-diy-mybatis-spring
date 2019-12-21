package cn.javadog.sd.mybatis.spring;

import static org.springframework.util.Assert.notNull;

import cn.javadog.sd.mybatis.mapping.Environment;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.transaction.SpringManagedTransactionFactory;
import cn.javadog.sd.mybatis.support.exceptions.PersistenceException;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author 余勇
 * @date 2019-12-21 22:16
 * 操作 SqlSession 的生命周期。可以从 {@code TransactionSynchronizationManager} 中获取/注册 SqlSession，即使没有事务，也可以工作
 */
public final class SqlSessionUtils {

  private static final Log LOGGER = LogFactory.getLog(SqlSessionUtils.class);

  private static final String NO_EXECUTOR_TYPE_SPECIFIED = "No ExecutorType specified";
  private static final String NO_SQL_SESSION_FACTORY_SPECIFIED = "No SqlSessionFactory specified";
  private static final String NO_SQL_SESSION_SPECIFIED = "No SqlSession specified";

  /**
   * 不对外暴露的构造
   */
  private SqlSessionUtils() {
    // do nothing
  }

  /**
   * 通过参数中的 {@code SqlSessionFactory}，获取到它的 {@code DataSource}和{@code ExecutorType}，
   * 以此来创建一个 SQL会话，
   *
   * @throws TransientDataAccessResourceException if a transaction is active and the
   *             {@code SqlSessionFactory} is not using a {@code SpringManagedTransactionFactory}
   */
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory) {
    ExecutorType executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
    return getSqlSession(sessionFactory, executorType, null);
  }

  /**
   * 从 Spring的事务管理器中拿到一个SQL会话，拿不到就创建一个。
   * 尝试从当前事务拿到会话，拿不到就去创建，
   * 然后呢，如果Spring的事务是激活状态，并且使用 SpringManagedTransactionFactory 作为事务管理器，那么对于SQL会话的方法调用，需要加上
   * 同步处理，钥匙就是事务对象
   *
   * @throws TransientDataAccessResourceException if a transaction is active and the
   *             {@code SqlSessionFactory} is not using a {@code SpringManagedTransactionFactory}
   * @see SpringManagedTransactionFactory
   */
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType, PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    notNull(executorType, NO_EXECUTOR_TYPE_SPECIFIED);
    // 获得 SqlSessionHolder 对象，这是从Spring的事务管理器拿到的
    SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
    // 获得 SqlSession 对象
    SqlSession session = sessionHolder(executorType, holder);
    if (session != null) {
      // 如果非空，直接返回
      return session;
    }

    LOGGER.debug("Creating a new SqlSession");
    // 创建 SqlSession 对象
    session = sessionFactory.openSession(executorType);
    // 注册到 TransactionSynchronizationManager 中
    registerSessionHolder(sessionFactory, executorType, exceptionTranslator, session);

    return session;
  }

  /**
   * 册 SqlSession holder 到 TransactionSynchronizationManager 中，前提是同步器是激活的(也就是说，该会话对应的事务是激活的)
   *
   * Note: 由 Environment 使用的 DataSource 必须支持同步，使用的钥匙是事务对象，不管这个事务对象是由谁创建的。进一步说，如果后面这个会话
   *  执行过程中抛出异常，这个事务对象将会关闭与此会话管理的连接，并进行回滚
   *
   * @param sessionFactory sqlSessionFactory used for registration.
   * @param executorType executorType used for registration.
   * @param exceptionTranslator persistenceExceptionTranslator used for registration.
   * @param session sqlSession used for registration.
   */
  private static void registerSessionHolder(SqlSessionFactory sessionFactory, ExecutorType executorType,
      PersistenceExceptionTranslator exceptionTranslator, SqlSession session) {
    SqlSessionHolder holder;
    // 事务必须是激活的
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      // 拿到环境对象
      Environment environment = sessionFactory.getConfiguration().getEnvironment();
      // 环境所使用的事务工厂是Spring管理的
      if (environment.getTransactionFactory() instanceof SpringManagedTransactionFactory) {
        LOGGER.debug("Registering transaction synchronization for SqlSession [" + session + "]");
        // 创建 SqlSessionHolder 对象
        holder = new SqlSessionHolder(session, executorType, exceptionTranslator);
        // 绑定到 TransactionSynchronizationManager 中
        TransactionSynchronizationManager.bindResource(sessionFactory, holder);
        // 创建 SqlSessionSynchronization 到 TransactionSynchronizationManager 中
        TransactionSynchronizationManager.registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));
        // 设置同步
        holder.setSynchronizedWithTransaction(true);
        // 增加计数
        holder.requested();
      }
      // 环境所使用的事务工厂不是Spring管理的
      else {
        // 数据源没有绑定到事务同步管理器，不会报错，但也不会注册
        if (TransactionSynchronizationManager.getResource(environment.getDataSource()) == null) {
          LOGGER.debug("SqlSession [" + session + "] was not registered for synchronization because DataSource is not transactional");
        }
        // 绑定了，那就说明数据源被非spring的事务管理着，直接GG
        else {
          throw new TransientDataAccessResourceException(
              "SqlSessionFactory must be using a SpringManagedTransactionFactory in order to use Spring transaction synchronization");
        }
      }
    } else {
      LOGGER.debug("SqlSession [" + session + "] was not registered for synchronization because synchronization is not active");
    }

}

  /**
   * 从 SqlSessionHolder 中，获得 SqlSession 对象
   */
  private static SqlSession sessionHolder(ExecutorType executorType, SqlSessionHolder holder) {
    SqlSession session = null;
    // 判断 holder 不为空，并且由事务对象同步相关操作
    if (holder != null && holder.isSynchronizedWithTransaction()) {
      // 如果执行器类型发生了变更，抛出 TransientDataAccessResourceException 异常
      if (holder.getExecutorType() != executorType) {
        throw new TransientDataAccessResourceException("Cannot change the ExecutorType when there is an existing transaction");
      }
      // 增加计数
      holder.requested();
      LOGGER.debug("Fetched SqlSession [" + holder.getSqlSession() + "] from current transaction");
      // 获得 SqlSession 对象
      session = holder.getSqlSession();
    }
    return session;
  }

  /**
   * 关闭 SqlSession 对象。先会检查会话是否由Spring的事务同步管理器管理，如果不是，直接关掉，如果是的话，只需要去释放 holder 的计数。
   * 让Spring自己在事务结束时进行回调和关闭的操作
   */
  public static void closeSqlSession(SqlSession session, SqlSessionFactory sessionFactory) {
    notNull(session, NO_SQL_SESSION_SPECIFIED);
    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    // 从 TransactionSynchronizationManager 中，获得 SqlSessionHolder 对象
    SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
    // 如果相等，说明在 Spring 托管的事务中，则释放 holder 计数
    if ((holder != null) && (holder.getSqlSession() == session)) {
      LOGGER.debug("Releasing transactional SqlSession [" + session + "]");
      holder.released();
    }
    // 如果不相等，说明不在 Spring 托管的事务中，直接关闭 SqlSession 对象
    else {
      LOGGER.debug("Closing non transactional SqlSession [" + session + "]");
      session.close();
    }
  }

  /**
   * 判断传入的 SqlSession 参数，是否在 Spring 事务中
   */
  public static boolean isSqlSessionTransactional(SqlSession session, SqlSessionFactory sessionFactory) {
    notNull(session, NO_SQL_SESSION_SPECIFIED);
    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);

    SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

    return (holder != null) && (holder.getSqlSession() == session);
  }

  /**
   * 内部类，继承 TransactionSynchronizationAdapter 抽象类，SqlSession 的 同步器，基于 Spring Transaction 体系。
   * 提供清除资源的回调，它会清空 TransactionSynchronizationManager，并且 提交/关闭 会话。
   * 它逻辑成立的前提是假设连接的生命周期是由{@code DataSourceTransactionManager}或{@code JtaTransactionManager}管理
   */
  private static final class SqlSessionSynchronization extends TransactionSynchronizationAdapter {

    /**
     * SQL会话持有器
     */
    private final SqlSessionHolder holder;

    /**
     * SQL会话工厂
     */
    private final SqlSessionFactory sessionFactory;

    /**
     * 是否开启
     */
    private boolean holderActive = true;

    /**
     * 构造
     */
    public SqlSessionSynchronization(SqlSessionHolder holder, SqlSessionFactory sessionFactory) {
      notNull(holder, "Parameter 'holder' must be not null");
      notNull(sessionFactory, "Parameter 'sessionFactory' must be not null");

      this.holder = holder;
      this.sessionFactory = sessionFactory;
    }

    /**
     * 获取前面的连接同步器的数量，或者说本同步器排在第几位
     */
    @Override
    public int getOrder() {
      return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 1;
    }

    /**
     * 当事务挂起时，取消当前线程的绑定的 SqlSessionHolder 对象
     */
    @Override
    public void suspend() {
      if (this.holderActive) {
        LOGGER.debug("Transaction synchronization suspending SqlSession [" + this.holder.getSqlSession() + "]");
        TransactionSynchronizationManager.unbindResource(this.sessionFactory);
      }
    }

    /**
     * 当事务恢复时，重新绑定当前线程的 SqlSessionHolder 对象
     */
    @Override
    public void resume() {
      if (this.holderActive) {
        LOGGER.debug("Transaction synchronization resuming SqlSession [" + this.holder.getSqlSession() + "]");
        // 因为，当前 SqlSessionSynchronization 对象中，有 holder 对象，所以可以直接恢复。
        TransactionSynchronizationManager.bindResource(this.sessionFactory, this.holder);
      }
    }

    /**
     *
     */
    @Override
    public void beforeCommit(boolean readOnly) {
      // Connection commit or rollback will be handled by ConnectionSynchronization or
      // DataSourceTransactionManager.
      // But, do cleanup the SqlSession / Executor, including flushing BATCH statements so
      // they are actually executed.
      // SpringManagedTransaction will no-op the commit over the jdbc connection
      // TODO This updates 2nd level caches but the tx may be rolledback later on! 
      if (TransactionSynchronizationManager.isActualTransactionActive()) {
        try {
          LOGGER.debug("Transaction synchronization committing SqlSession [" + this.holder.getSqlSession() + "]");
          this.holder.getSqlSession().commit();
        } catch (PersistenceException p) {
          if (this.holder.getPersistenceExceptionTranslator() != null) {
            DataAccessException translated = this.holder
                .getPersistenceExceptionTranslator()
                .translateExceptionIfPossible(p);
            if (translated != null) {
              throw translated;
            }
          }
          throw p;
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeCompletion() {
      // Issue #18 Close SqlSession and deregister it now
      // because afterCompletion may be called from a different thread
      if (!this.holder.isOpen()) {
        LOGGER.debug("Transaction synchronization deregistering SqlSession [" + this.holder.getSqlSession() + "]");
        TransactionSynchronizationManager.unbindResource(sessionFactory);
        this.holderActive = false;
        LOGGER.debug("Transaction synchronization closing SqlSession [" + this.holder.getSqlSession() + "]");
        this.holder.getSqlSession().close();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCompletion(int status) {
      if (this.holderActive) {
        // afterCompletion may have been called from a different thread
        // so avoid failing if there is nothing in this one
        LOGGER.debug("Transaction synchronization deregistering SqlSession [" + this.holder.getSqlSession() + "]");
        TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory);
        this.holderActive = false;
        LOGGER.debug("Transaction synchronization closing SqlSession [" + this.holder.getSqlSession() + "]");
        this.holder.getSqlSession().close();
      }
      this.holder.reset();
    }
  }

}
