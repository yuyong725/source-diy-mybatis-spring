package cn.javadog.sd.mybatis.spring;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * @author 余勇
 * @date 2019-12-22 14:05
 *
 * MyBatis版本的{@code UncategorizedDataAccessException}，用于包装 {@code org.springframework.dao} 下的异常。
 * Mybatis 3 中的 PersistenceException 是一个运行时异常，通过此包装类，使得所有异常在一个level，更方便处理
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MyBatisSystemException extends UncategorizedDataAccessException {

  private static final long serialVersionUID = -5284728621670758939L;

  public MyBatisSystemException(Throwable cause) {
    super(null, cause);
  }

}
