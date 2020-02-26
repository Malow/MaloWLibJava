package com.github.malow.malowlib.lambdainterfaces;

@FunctionalInterface
public interface CheckedFunctionWithParameter<T>
{
  void apply(T data) throws Exception;
}