package org.ethereum.validator;

/**
 * Holds errors list to share between all rules
 *
 * @author Mikhail Kalinin
 * @since 02.09.2015
 */
abstract class AbstractValidationRule {

    abstract public Class getEntityClass();
}
