package client.mvvm.service;

import core.ParameterRequest;

/** Supplies values when server asks for interactive parameters. */
@FunctionalInterface
public interface ParameterValueProvider {
    String provide(ParameterRequest parameterRequest, boolean passwordInput);
}
