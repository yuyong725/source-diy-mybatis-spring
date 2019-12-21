package cn.javadog.sd.mybatis.spring;

import static org.springframework.util.Assert.notNull;

import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.SqlSession;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * @author 余勇
 * @date 2019-12-21 22:03
 *
 * SqlSession 持有器，用于保存当前 SqlSession 对象，
 * 保存到 org.springframework.transaction.support.TransactionSynchronizationManager，
 * 使用的 KEY 为创建该 SqlSession 对象的 SqlSessionFactory 对象。 。
 */
public final class SqlSessionHolder extends ResourceHolderSupport {

  /**
   * 持有的SQL会话
   */
  private final SqlSession sqlSession;

  /**
   * 持有会话时，对应的执行器的类型，如果会话后期修改的执行器的类型，将会检测然后GG
   */
  private final ExecutorType executorType;

  /**
   * 异常转换器
   */
  private final PersistenceExceptionTranslator exceptionTranslator;

  /**
   * 持有器的构造
   */
  public SqlSessionHolder(SqlSession sqlSession,
      ExecutorType executorType,
      PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sqlSession, "SqlSession must not be null");
    notNull(executorType, "ExecutorType must not be null");

    this.sqlSession = sqlSession;
    this.executorType = executorType;
    this.exceptionTranslator = exceptionTranslator;
  }

  /*所有属性的get*/

  public SqlSession getSqlSession() {
    return sqlSession;
  }

  public ExecutorType getExecutorType() {
    return executorType;
  }

  public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
    return exceptionTranslator;
  }

}
