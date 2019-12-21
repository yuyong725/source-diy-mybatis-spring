package cn.javadog.sd.mybatis.spring.transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.transaction.TransactionFactory;
import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;

/**
 * @author 余勇
 * @date 2019-12-21 21:58
 * {@code SpringManagedTransaction}工厂
 */
public class SpringManagedTransactionFactory implements TransactionFactory {

  /**
   * 开启事务
   */
  @Override
  public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
    return new SpringManagedTransaction(dataSource);
  }

  /**
   * 使用连接开启事务，抱歉，GG 吧
   */
  @Override
  public Transaction newTransaction(Connection conn) {
    throw new UnsupportedOperationException("New Spring transactions require a DataSource");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties(Properties props) {
    // not needed in this version
  }

}
