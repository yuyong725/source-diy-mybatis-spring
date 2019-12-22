package cn.javadog.sd.mybatis.spring.batch.builder;

import java.util.Map;
import java.util.Optional;

import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.batch.MyBatisPagingItemReader;

/**
 * @author 余勇
 * @date 2019-12-22 14:47
 *
 * {@link MyBatisPagingItemReader} 的构造器，代码相当简单
 */
public class MyBatisPagingItemReaderBuilder<T> {

  private SqlSessionFactory sqlSessionFactory;
  private String queryId;
  private Map<String, Object> parameterValues;
  private Integer pageSize;
  private Boolean saveState;
  private Integer maxItemCount;

  public MyBatisPagingItemReaderBuilder<T> sqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public MyBatisPagingItemReaderBuilder<T> queryId(String queryId) {
    this.queryId = queryId;
    return this;
  }

  public MyBatisPagingItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
    this.parameterValues = parameterValues;
    return this;
  }

  public MyBatisPagingItemReaderBuilder<T> pageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public MyBatisPagingItemReaderBuilder<T> saveState(boolean saveState) {
    this.saveState = saveState;
    return this;
  }

  public MyBatisPagingItemReaderBuilder<T> maxItemCount(int maxItemCount) {
    this.maxItemCount = maxItemCount;
    return this;
  }

  public MyBatisPagingItemReader<T> build() {
    MyBatisPagingItemReader<T> reader = new MyBatisPagingItemReader<>();
    reader.setSqlSessionFactory(this.sqlSessionFactory);
    reader.setQueryId(this.queryId);
    reader.setParameterValues(this.parameterValues);
    Optional.ofNullable(this.pageSize).ifPresent(reader::setPageSize);
    Optional.ofNullable(this.saveState).ifPresent(reader::setSaveState);
    Optional.ofNullable(this.maxItemCount).ifPresent(reader::setMaxItemCount);
    return reader;
  }

}
