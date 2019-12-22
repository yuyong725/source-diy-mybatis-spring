package cn.javadog.sd.mybatis.spring.batch.builder;

import java.util.Optional;

import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.SqlSessionTemplate;
import cn.javadog.sd.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author 余勇
 * @date 2019-12-22 14:46
 *
 * {@link MyBatisBatchItemWriter} 的构造器，代码相当简单
 */
public class MyBatisBatchItemWriterBuilder<T> {

  private SqlSessionTemplate sqlSessionTemplate;
  private SqlSessionFactory sqlSessionFactory;
  private String statementId;
  private Boolean assertUpdates;
  private Converter<T, ?> itemToParameterConverter;

  public MyBatisBatchItemWriterBuilder<T> sqlSessionTemplate(
      SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
    return this;
  }

  public MyBatisBatchItemWriterBuilder<T> sqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  public MyBatisBatchItemWriterBuilder<T> statementId(String statementId) {
    this.statementId = statementId;
    return this;
  }

  public MyBatisBatchItemWriterBuilder<T> assertUpdates(boolean assertUpdates) {
    this.assertUpdates = assertUpdates;
    return this;
  }

  public MyBatisBatchItemWriterBuilder<T> itemToParameterConverter(Converter<T, ?> itemToParameterConverter) {
    this.itemToParameterConverter = itemToParameterConverter;
    return this;
  }

  public MyBatisBatchItemWriter<T> build() {
    MyBatisBatchItemWriter<T> writer = new MyBatisBatchItemWriter<>();
    writer.setSqlSessionTemplate(this.sqlSessionTemplate);
    writer.setSqlSessionFactory(this.sqlSessionFactory);
    writer.setStatementId(this.statementId);
    Optional.ofNullable(this.assertUpdates).ifPresent(writer::setAssertUpdates);
    Optional.ofNullable(this.itemToParameterConverter).ifPresent(writer::setItemToParameterConverter);
    return writer;
  }

}
