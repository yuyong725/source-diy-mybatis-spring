package cn.javadog.sd.mybatis.spring.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * The Container annotation that aggregates several {@link MapperScan} annotations.
 *
 * <p>Can be used natively, declaring several nested {@link MapperScan} annotations.
 * Can also be used in conjunction with Java 8's support for repeatable annotations,
 * where {@link MapperScan} can simply be declared several times on the same method,
 * implicitly generating this container annotation.
 *
 * @author Kazuki Shimizu
 * @since 2.0.0
 * @see MapperScan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MapperScannerRegistrar.RepeatingRegistrar.class)
public @interface MapperScans {
  MapperScan[] value();
}
