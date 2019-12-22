package cn.javadog.sd.mybatis.spring;

import java.util.Properties;

import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.plugin.Interceptor;
import cn.javadog.sd.mybatis.plugin.Intercepts;
import cn.javadog.sd.mybatis.plugin.Invocation;
import cn.javadog.sd.mybatis.plugin.Plugin;
import cn.javadog.sd.mybatis.plugin.Signature;

/**
 * @author 余勇
 * @date 2019-12-22 15:13
 * 拦截器走一个
 */
@Intercepts({
    @Signature(type = Executor.class, method = "commit", args = { boolean.class }),
    @Signature(type = Executor.class, method = "rollback", args = { boolean.class }),
    @Signature(type = Executor.class, method = "close", args = { boolean.class })
})
final class ExecutorInterceptor implements Interceptor {

  /**
   * 会话提交的次数
   */
  private int commitCount;

  /**
   * 会话回滚的次数
   */
  private int rollbackCount;

  /**
   * 会话是否关闭
   */
  private boolean closed;

  /**
   * 根据操作类型记录数量
   */
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    if ("commit".equals(invocation.getMethod().getName())) {
      ++this.commitCount;
    } else if ("rollback".equals(invocation.getMethod().getName())) {
      ++this.rollbackCount;
    } else if ("close".equals(invocation.getMethod().getName())) {
      this.closed = true;
    }

    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
  }

  /**
   * 重置拦截器
   */
  void reset() {
    this.commitCount = 0;
    this.rollbackCount = 0;
    this.closed = false;
  }

  int getCommitCount() {
    return this.commitCount;
  }

  int getRollbackCount() {
    return this.rollbackCount;
  }

  boolean isExecutorClosed() {
    return this.closed;
  }

}
