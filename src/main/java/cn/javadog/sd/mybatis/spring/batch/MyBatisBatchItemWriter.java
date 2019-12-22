package cn.javadog.sd.mybatis.spring.batch;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

import java.util.List;

import cn.javadog.sd.mybatis.executor.BatchResult;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.SqlSessionTemplate;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * @author 余勇
 * @date 2019-12-22 14:38
 *
 * MyBatis 批量写入器
 */
public class MyBatisBatchItemWriter<T> implements ItemWriter<T>, InitializingBean {

  /**
   * 日志打印器
   */
  private static final Log LOGGER = LogFactory.getLog(MyBatisBatchItemWriter.class);

  /**
   * SQL会话模板
   */
  private SqlSessionTemplate sqlSessionTemplate;

  /**
   * 会话ID，与 queryId 一个性质
   */
  private String statementId;

  /**
   * 是否校验更新操作，因为是批量写入，每条插入语句必定有影响的行数，咱校验的就是这
   */
  private boolean assertUpdates = true;

  /**
   * 参数转换器
   */
  private Converter<T, ?> itemToParameterConverter = new PassThroughConverter<>();

  /*一些set*/

  public void setAssertUpdates(boolean assertUpdates) {
    this.assertUpdates = assertUpdates;
  }

  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    if (sqlSessionTemplate == null) {
      this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }
  }

  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  public void setStatementId(String statementId) {
    this.statementId = statementId;
  }

  public void setItemToParameterConverter(Converter<T, ?> itemToParameterConverter) {
    this.itemToParameterConverter = itemToParameterConverter;
  }

  /**
   * 强制校验必需的属性
   */
  @Override
  public void afterPropertiesSet() {
    notNull(sqlSessionTemplate, "A SqlSessionFactory or a SqlSessionTemplate is required.");
    isTrue(ExecutorType.BATCH == sqlSessionTemplate.getExecutorType(), "SqlSessionTemplate's executor type must be BATCH");
    notNull(statementId, "A statementId is required.");
    notNull(itemToParameterConverter, "A itemToParameterConverter is required.");
  }

  /**
   * 批量写入
   */
  @Override
  public void write(final List<? extends T> items) {

    if (!items.isEmpty()) {
      LOGGER.debug("Executing batch with " + items.size() + " items.");

      // 遍历 items 数组，提交到 sqlSessionTemplate 中
      for (T item : items) {
        sqlSessionTemplate.update(statementId, itemToParameterConverter.convert(item));
      }
      // 刷入批处理
      List<BatchResult> results = sqlSessionTemplate.flushStatements();

      if (assertUpdates) {
        // 如果有多个返回结果集，也就是存储过程的情况，抛出 InvalidDataAccessResourceUsageException 异常
        if (results.size() != 1) {
          throw new InvalidDataAccessResourceUsageException("Batch execution returned invalid results. " +
              "Expected 1 but number of BatchResult objects returned was " + results.size());
        }

        int[] updateCounts = results.get(0).getUpdateCounts();

        // 遍历执行结果，若存在未更新的情况，则抛出 EmptyResultDataAccessException 异常
        for (int i = 0; i < updateCounts.length; i++) {
          int value = updateCounts[i];
          if (value == 0) {
            throw new EmptyResultDataAccessException("Item " + i + " of " + updateCounts.length
                + " did not update any rows: [" + items.get(i) + "]", 1);
          }
        }
      }
    }
  }

  private static class PassThroughConverter<T> implements Converter<T, T> {

    @Override
    public T convert(T source) {
      return source;
    }

  }

}
