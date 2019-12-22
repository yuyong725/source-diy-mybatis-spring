package cn.javadog.sd.mybatis.spring.batch;

import static org.springframework.util.Assert.notNull;
import static org.springframework.util.ClassUtils.getShortName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.item.database.AbstractPagingItemReader;

/**
 * @author 余勇
 * @date 2019-12-22 14:19
 *
 * 基于分页的 MyBatis 的读取器
 */
public class MyBatisPagingItemReader<T> extends AbstractPagingItemReader<T> {

  /**
   * 查询编号
   */
  private String queryId;

  /**
   * SQL会话工厂
   */
  private SqlSessionFactory sqlSessionFactory;

  /**
   * SQL会话模板
   */
  private SqlSessionTemplate sqlSessionTemplate;

  /**
   * 参数值的映射
   */
  private Map<String, Object> parameterValues;

  /**
   * 构造
   */
  public MyBatisPagingItemReader() {
    // 设置执行上下文的名称
    setName(getShortName(MyBatisPagingItemReader.class));
  }

  /*几个属性的set*/

  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  public void setParameterValues(Map<String, Object> parameterValues) {
    this.parameterValues = parameterValues;
  }

  /**
   * 检查下属性非空
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    // 父类的处理
    super.afterPropertiesSet();
    notNull(sqlSessionFactory, "A SqlSessionFactory is required.");
    sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    notNull(queryId, "A queryId is required.");
  }

  /**
   * 执行每一次分页的读取
   */
  @Override
  protected void doReadPage() {
    // 创建 parameters 参数
    Map<String, Object> parameters = new HashMap<>();
    // 设置原有参数
    if (parameterValues != null) {
      parameters.putAll(parameterValues);
    }
    // 设置分页参数
    parameters.put("_page", getPage());
    parameters.put("_pagesize", getPageSize());
    parameters.put("_skiprows", getPage() * getPageSize());
    //清空目前的 results 结果
    if (results == null) {
      // 使用 CopyOnWriteArrayList 的原因是，可能存在并发读取的问题。
      results = new CopyOnWriteArrayList<>();
    } else {
      results.clear();
    }
    // 查询结果
    results.addAll(sqlSessionTemplate.selectList(queryId, parameters));
  }

  /**
   * 跳到指定页面
   */
  @Override
  protected void doJumpToPage(int itemIndex) {
  }

}
