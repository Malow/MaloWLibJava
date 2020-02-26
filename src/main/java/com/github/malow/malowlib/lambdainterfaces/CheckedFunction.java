package com.github.malow.malowlib.lambdainterfaces;

@FunctionalInterface
public interface CheckedFunction
{
  void apply() throws Exception;
}
