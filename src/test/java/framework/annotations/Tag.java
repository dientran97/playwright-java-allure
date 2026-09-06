package framework.annotations;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a free form tag to a test case, i.e. {@code @Tag("smoke")}.
 *
 * <p>It is mapped onto the Allure {@code tag} label, so the tag becomes a filter in the report.
 * The annotation is repeatable.</p>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(Tags.class)
@LabelAnnotation(name = "tag")
public @interface Tag {

    /**
     * @return the tag value shown in the Allure report
     */
    String value();
}
