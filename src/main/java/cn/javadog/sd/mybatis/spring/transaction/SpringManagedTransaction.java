package cn.javadog.sd.mybatis.spring.transaction;

import static org.springframework.util.Assert.notNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.transaction.Transaction;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@code SpringManagedTransaction} handles the lifecycle of a JDBC connection.
 * It retrieves a connection from Spring's transaction manager and returns it back to it
 * when it is no longer needed.
 * <p>
 * If Spring's transaction handling is active it will no-op all commit/rollback/close calls
 * assuming that the Spring transaction manager will do the job.
 * <p>
 * If it is not it will behave like {@code JdbcTransaction}.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 */
/**
 * @author 余勇
 * @date 2019-12-21 21:44
 *
 * spring的事务管理，用于处理JDBC连接的生命周期。
 * 你可以通过它检索到一个连接，然后在不用的时候还给它。
 * 如果说，Spring的事务操作被激活了，那么码农将不需要手动的去调用 commit/rollback/close 这些方法，而是由
 * Spring的事务管理器去完成相应的操作。
 * 没被激活的话，就和 {@code JdbcTransaction} 一样
 *
 */
public class SpringManagedTransaction implements Transaction {

  /**
   * 日志打印器
   */
  private static final Log LOGGER = LogFactory.getLog(SpringManagedTransaction.class);

  /**
   * DataSource 对象
   */
  private final DataSource dataSource;

  /**
   * 连接对象
   */
  private Connection connection;

  /**
   * 当前连接是否处于事务中
   */
  private boolean isConnectionTransactional;

  /**
   * 是否自动提交
   */
  private boolean autoCommit;

  /**
   * 构造方法
   */
  public SpringManagedTransaction(DataSource dataSource) {
    notNull(dataSource, "No DataSource specified");
    this.dataSource = dataSource;
  }

  /**
   * 获取连接
   */
  @Override
  public Connection getConnection() throws SQLException {
    if (this.connection == null) {
      openConnection();
    }
    return this.connection;
  }

  /**
   * 开启连接
   * 从Spring的事务管理器中获取一个连接，检测这个连接是否关联事务。
   * 当然，还需要检测自动提交属性，因为Spring管理mybatis的事务默认自动提交始终为不，将会尤它一直调用 commit/rollback，而不需要我们手动的去调用
   *
   * 比较有趣的是，此处获取连接，不是通过 DataSource#getConnection() 方法，
   * 而是通过 org.springframework.jdbc.datasource.DataSourceUtils#getConnection(DataSource dataSource) 方法，获得 Connection 对象。
   * 而实际上，基于 Spring Transaction 体系，如果此处正在事务中时，已经有和当前线程绑定的 Connection 对象，就是存储在 ThreadLocal 中。
   */
  private void openConnection() throws SQLException {
    this.connection = DataSourceUtils.getConnection(this.dataSource);
    this.autoCommit = this.connection.getAutoCommit();
    this.isConnectionTransactional = DataSourceUtils.isConnectionTransactional(this.connection, this.dataSource);

    LOGGER.debug("JDBC Connection ["
            + this.connection
            + "] will"
            + (this.isConnectionTransactional ? " " : " not ")
            + "be managed by Spring");
  }

  /**
   * 提交事务
   */
  @Override
  public void commit() throws SQLException {
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      LOGGER.debug("Committing JDBC Connection [" + this.connection + "]");
      this.connection.commit();
    }
  }

  /**
   * 回滚事务
   */
  @Override
  public void rollback() throws SQLException {
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      LOGGER.debug("Rolling back JDBC Connection [" + this.connection + "]");
      this.connection.rollback();
    }
  }

  /**
   * 关闭会话
   */
  @Override
  public void close() {
    DataSourceUtils.releaseConnection(this.connection, this.dataSource);
  }
    
  /**
   * 获取连接超时时间
   */
  @Override
  public Integer getTimeout() {
    ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    if (holder != null && holder.hasTimeout()) {
      return holder.getTimeToLiveInSeconds();
    } 
    return null;
  }

}
