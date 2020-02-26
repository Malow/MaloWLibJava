package com.github.malow.malowlib.lambdainterfaces;

@FunctionalInterface
public interface CheckedFunctionWithReturn<S>
{
  S apply() throws Exception;
}