package cn.javadog.sd.mybatis.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.SQLException;

import cn.javadog.sd.mybatis.plugin.Interceptor;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;

public abstract class AbstractMyBatisSpringTest {

  /**
   * 假数据源
   */
  protected static PooledMockDataSource dataSource = new PooledMockDataSource();

  /**
   * 会话工厂
   */
  protected static SqlSessionFactory sqlSessionFactory;

  /**
   * 拦截器
   */
  protected static ExecutorInterceptor executorInterceptor = new ExecutorInterceptor();

  /**
   * 事务管理器
   */
  protected static DataSourceTransactionManager txManager;

  /**
   * 异常转换器
   */
  protected static PersistenceExceptionTranslator exceptionTranslator;

  /**
   * 假连接
   */
  protected MockConnection connection;

  /**
   * 另一个假连接
   */
  protected MockConnection connectionTwo;

  @BeforeAll
  public static void setupBase() throws Exception {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setMapperLocations(new Resource[] { new ClassPathResource("cn/javadog/sd/mybatis/spring/TestMapper.xml") });
    factoryBean.setDataSource(dataSource);
    factoryBean.setPlugins(new Interceptor[] { executorInterceptor });
    exceptionTranslator = new MyBatisExceptionTranslator(dataSource, true);
    // 初始化会话工厂
    sqlSessionFactory = factoryBean.getObject();
    // 初始化事务管理器
    txManager = new DataSourceTransactionManager(dataSource);
  }

  /**
   * 未提交/未回滚
   */
  protected void assertNoCommit() {
    assertNoCommitJdbc();
    assertNoCommitSession();
  }

  /**
   * 连接 未提交/未回滚
   */
  protected void assertNoCommitJdbc() {
    assertThat(connection.getNumberCommits()).as("should not call commit on Connection").isEqualTo(0);
    assertThat(connection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);
  }

  /**
   * 会话 未提交/未回滚
   */
  protected void assertNoCommitSession() {
    assertThat(executorInterceptor.getCommitCount()).as("should not call commit on SqlSession").isEqualTo(0);
    assertThat(executorInterceptor.getRollbackCount()).as("should not call rollback on SqlSession").isEqualTo(0);
  }

  /**
   * 未提交/未回滚
   */
  protected void assertCommit() {
    assertCommitJdbc();
    assertCommitSession();
  }

  /**
   * 连接 已提交/未回滚
   */
  protected void assertCommitJdbc() {
    assertThat(connection.getNumberCommits()).as("should call commit on Connection").isEqualTo(1);
    assertThat(connection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);
  }

  /**
   * 会话 已提交/未回滚
   */
  protected void assertCommitSession() {
    assertThat(executorInterceptor.getCommitCount()).as("should call commit on SqlSession").isEqualTo(1);
    assertThat(executorInterceptor.getRollbackCount()).as("should not call rollback on SqlSession").isEqualTo(0);
  }

  /**
   * 未提交/已回滚
   */
  protected void assertRollback() {
    assertThat(connection.getNumberCommits()).as("should not call commit on Connection").isEqualTo(0);
    assertThat(connection.getNumberRollbacks()).as("should call rollback on Connection").isEqualTo(1);
    assertThat(executorInterceptor.getCommitCount()).as("should not call commit on SqlSession").isEqualTo(0);
    assertThat(executorInterceptor.getRollbackCount()).as("should call rollback on SqlSession").isEqualTo(1);
  }

  /**
   * 连接池活跃连接数量=1
   */
  protected void assertSingleConnection() {
    assertThat(dataSource.getConnectionCount()).as("should only call DataSource.getConnection() once").isEqualTo(1);
  }

  /**
   * 正在执行的会话数量=count
   */
  protected void assertExecuteCount(int count) {
    assertThat(connection.getPreparedStatementResultSetHandler().getExecutedStatements().size()).as(
        "should have executed %d SQL statements", count).isEqualTo(count);
  }

  /**
   * 连接必需GG
   */
  protected void assertConnectionClosed(MockConnection connection) {
    try {
      if ((connection != null) && !connection.isClosed()) {
        fail("Connection is not closed");
      }
    } catch (SQLException sqle) {
      fail("cannot call Connection.isClosed() " + sqle.getMessage());
    }
  }

  /**
   * 创建连接，啥玩意这是
   */
  protected MockConnection createMockConnection() {
    // this query must be the same as the query in TestMapper.xml？
    MockResultSet rs = new MockResultSet("SELECT 1");
    rs.addRow(new Object[] { 1 });

    MockConnection con = new MockConnection();
    con.getPreparedStatementResultSetHandler().prepareResultSet("SELECT 1", rs);

    return con;
  }

  /**
   * 初始化。重置连接
   */
  @BeforeEach
  public void setupConnection() throws SQLException {
    dataSource.reset();
    connection = createMockConnection();
    connectionTwo = createMockConnection();
    dataSource.addConnection(connectionTwo);
    dataSource.addConnection(connection);
  }

  /**
   * 重置拦截器
   */
  @BeforeEach
  public void resetExecutorInterceptor() {
    executorInterceptor.reset();
  }

  /**
   * 验证连接关闭
   */
  @AfterEach
  public void validateConnectionClosed() {
    assertConnectionClosed(connection);

    connection = null;
  }

}
