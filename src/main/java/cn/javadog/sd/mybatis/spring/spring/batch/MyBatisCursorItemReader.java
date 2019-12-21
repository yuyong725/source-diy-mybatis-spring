package cn.javadog.sd.mybatis.spring.spring.batch;

import static org.springframework.util.Assert.notNull;
import static org.springframework.util.ClassUtils.getShortName;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Guillaume Darmont / guillaume@dropinocean.com
 */
public class MyBatisCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {

  private String queryId;

  private SqlSessionFactory sqlSessionFactory;
  private SqlSession sqlSession;

  private Map<String, Object> parameterValues;

  private Cursor<T> cursor;
  private Iterator<T> cursorIterator;

  public MyBatisCursorItemReader() {
    setName(getShortName(MyBatisCursorItemReader.class));
  }

  @Override
  protected T doRead() {
    T next = null;
    if (cursorIterator.hasNext()) {
      next = cursorIterator.next();
    }
    return next;
  }

  @Override
  protected void doOpen() {
    Map<String, Object> parameters = new HashMap<>();
    if (parameterValues != null) {
      parameters.putAll(parameterValues);
    }

    sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
    cursor = sqlSession.selectCursor(queryId, parameters);
    cursorIterator = cursor.iterator();
  }

  @Override
  protected void doClose() throws Exception {
    if (cursor != null) {
      cursor.close();
    }
    if (sqlSession != null) {
      sqlSession.close();
    }
    cursorIterator = null;
  }

  /**
   * Check mandatory properties.
   *
   * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    notNull(sqlSessionFactory, "A SqlSessionFactory is required.");
    notNull(queryId, "A queryId is required.");
  }

  /**
   * Public setter for {@link SqlSessionFactory} for injection purposes.
   *
   * @param sqlSessionFactory a factory object for the {@link SqlSession}.
   */
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  /**
   * Public setter for the statement id identifying the statement in the SqlMap
   * configuration file.
   *
   * @param queryId the id for the statement
   */
  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  /**
   * The parameter values to be used for the query execution.
   *
   * @param parameterValues the values keyed by the parameter named used in
   *                        the query string.
   */
  public void setParameterValues(Map<String, Object> parameterValues) {
    this.parameterValues = parameterValues;
  }
}
