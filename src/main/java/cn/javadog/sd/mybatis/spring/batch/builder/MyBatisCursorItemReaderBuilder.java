package cn.javadog.sd.mybatis.spring.batch.builder;

import java.util.Map;
import java.util.Optional;

import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.batch.MyBatisCursorItemReader;

/**
 * @author 余勇
 * @date 2019-12-22 14:47
 *
 * {@link MyBatisCursorItemReader} 的构造器，代码相当简单
 */
public class MyBatisCursorItemReaderBuilder<T> {

  private SqlSessionFactory sqlSessionFactory;
  private String queryId;
  private Map<String, Object> parameterValues;
  private Boolean saveState;
  private Integer maxItemCount;

  public MyBatisCursorItemReaderBuilder<T> sqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public MyBatisCursorItemReaderBuilder<T> queryId(String queryId) {
    this.queryId = queryId;
    return this;
  }

  public MyBatisCursorItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
    this.parameterValues = parameterValues;
    return this;
  }

  public MyBatisCursorItemReaderBuilder<T> saveState(boolean saveState) {
    this.saveState = saveState;
    return this;
  }

  public MyBatisCursorItemReaderBuilder<T> maxItemCount(int maxItemCount) {
    this.maxItemCount = maxItemCount;
    return this;
  }

  public MyBatisCursorItemReader<T> build() {
    MyBatisCursorItemReader<T> reader = new MyBatisCursorItemReader<>();
    reader.setSqlSessionFactory(this.sqlSessionFactory);
    reader.setQueryId(this.queryId);
    reader.setParameterValues(this.parameterValues);
    Optional.ofNullable(this.saveState).ifPresent(reader::setSaveState);
    Optional.ofNullable(this.maxItemCount).ifPresent(reader::setMaxItemCount);
    return reader;
  }

}
