package com.github.malow.malowlib;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;

public class PersistedDataTest
{
  public static class TestDataClass
  {
    String testString = "asd";
  }

  @Test
  public void test() throws Exception
  {
    PersistedData.useData(TestDataClass.class, "test.json", data ->
    {
      data.testString = "dsa";
    });

    PersistedData.useData(TestDataClass.class, "test.json", data ->
    {
      assertThat(data.testString).isEqualTo("dsa");
    });

    Files.write(Paths.get("test.json"), "{\"testString\":\"qwe\"}".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.CREATE);

    PersistedData.useData(TestDataClass.class, "test.json", data ->
    {
      assertThat(data.testString).isEqualTo("qwe");
      data.testString = "dsa";
    });

    PersistedData.useData(TestDataClass.class, "test.json", data ->
    {
      assertThat(data.testString).isEqualTo("dsa");
    });
  }
}
