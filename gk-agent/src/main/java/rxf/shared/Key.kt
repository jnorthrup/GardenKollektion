package rxf.shared

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * User: jim
 * Date: 6/6/12
 * Time: 7:23 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Key 