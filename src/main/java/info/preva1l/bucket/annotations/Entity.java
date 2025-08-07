package info.preva1l.bucket.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an Entity.
 * These classes will be (de)serialised between BSON and the document class,
 * and subsequently stored in the named repository.
 *
 * @author Preva1l
 * @since 9/07/2025
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    /**
     * The name of the repository this document belongs to.
     *
     * @return the collection.
     */
    String value();
}