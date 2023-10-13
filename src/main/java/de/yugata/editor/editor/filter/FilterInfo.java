package de.yugata.editor.editor.filter;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FilterInfo {

    String name();

    String description() default "";

    String value() default "";

    FilterType filterType() default FilterType.VIDEO;

}
