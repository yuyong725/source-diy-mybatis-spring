package cn.javadog.sd.mybatis.spring.batch;

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
 * 实现 InitializingBean 接口，基于 Cursor 的 MyBatis 的读取器
 */
public class MyBatisCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {

  /**
   * 查询ID
   */
  private String queryId;

  /**
   * SQL会话工厂
   */
  private SqlSessionFactory sqlSessionFactory;

  /**
   * SQL会话
   */
  private SqlSession sqlSession;

  /**
   * 参数值映射
   */
  private Map<String, Object> parameterValues;

  /**
   * 游标
   */
  private Cursor<T> cursor;

  /**
   * 游标迭代器
   */
  private Iterator<T> cursorIterator;

  /**
   * 构造
   */
  public MyBatisCursorItemReader() {
    setName(getShortName(MyBatisCursorItemReader.class));
  }


  /**
   * 获取下一条
   */
  @Override
  protected T doRead() {
    T next = null;
    if (cursorIterator.hasNext()) {
      next = cursorIterator.next();
    }
    return next;
  }

  /**
   * 打开游标的迭代器
   */
  @Override
  protected void doOpen() {
    Map<String, Object> parameters = new HashMap<>();
    if (parameterValues != null) {
      parameters.putAll(parameterValues);
    }

    sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
    // 查询，返回 Cursor 对象
    cursor = sqlSession.selectCursor(queryId, parameters);
    cursorIterator = cursor.iterator();
  }

  /**
   * 关闭游标和会话
   */
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
   * 校验必需的属性非空
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    notNull(sqlSessionFactory, "A SqlSessionFactory is required.");
    notNull(queryId, "A queryId is required.");
  }

  /*相应的set*/

  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  public void setParameterValues(Map<String, Object> parameterValues) {
    this.parameterValues = parameterValues;
  }
}
