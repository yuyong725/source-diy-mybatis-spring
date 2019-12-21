package cn.javadog.sd.mybatis.spring.logging;


import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * LoggerFactory is a wrapper around {@link LogFactory} to support {@link Logger}.
 *
 * @author Putthiphong Boonphong
 */
public class LoggerFactory {

  private LoggerFactory() {
    // NOP
  }

  public static Logger getLogger(Class<?> aClass) {
    return new Logger(LogFactory.getLog(aClass));
  }

  public static Logger getLogger(String logger) {
    return new Logger(LogFactory.getLog(logger));
  }

}
