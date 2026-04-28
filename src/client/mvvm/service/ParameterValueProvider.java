package client.mvvm.service;

import core.ParameterRequest;

/**
 * Functional interface for collecting parameter values during interactive commands.
 *
 * <p>Implementers must provide a way to prompt the user for input and return the value.
 * Can be used for text input or password masking depending on the parameter type.
 */
@FunctionalInterface
public interface ParameterValueProvider {
    String provide(ParameterRequest parameterRequest, boolean passwordInput);
}
