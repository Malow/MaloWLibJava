package com.github.malow.malowlib.lambdainterfaces;

@FunctionalInterface
public interface CheckedFunctionWithParameterAndReturn<T, S>
{
  S apply(T resource) throws Exception;
}