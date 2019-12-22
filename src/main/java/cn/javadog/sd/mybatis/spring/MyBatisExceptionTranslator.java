package cn.javadog.sd.mybatis.spring;

import javax.sql.DataSource;
import java.sql.SQLException;

import cn.javadog.sd.mybatis.support.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.TransactionException;

/**
 * @author 余勇
 * @date 2019-12-22 14:10
 *
 * 异常转换器。
 * 将 MyBatis SqlSession 的异常转换成Spring的异常
 */
public class MyBatisExceptionTranslator implements PersistenceExceptionTranslator {

  /**
   * 数据源
   */
  private final DataSource dataSource;

  /**
   * 异常翻译器
   */
  private SQLExceptionTranslator exceptionTranslator;

  /**
   * 构造函数
   * @param exceptionTranslatorLazyInit 是否懒加载
   */
  public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
    this.dataSource = dataSource;

    if (!exceptionTranslatorLazyInit) {
      this.initExceptionTranslator();
    }
  }

  /**
   * 转换异常
   */
  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException e) {
    if (e instanceof PersistenceException) {
      if (e.getCause() instanceof PersistenceException) {
        e = (PersistenceException) e.getCause();
      }
      if (e.getCause() instanceof SQLException) {
        this.initExceptionTranslator();
        return this.exceptionTranslator.translate(e.getMessage() + "\n", null, (SQLException) e.getCause());
      } else if (e.getCause() instanceof TransactionException) {
        throw (TransactionException) e.getCause();
      }
      return new MyBatisSystemException(e);
    } 
    return null;
  }

  /**
   * 初始化异常转换器的真正实现
   */
  private synchronized void initExceptionTranslator() {
    if (this.exceptionTranslator == null) {
      this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
    }
  }

}
