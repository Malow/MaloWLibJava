package com.github.malow.malowlib.confighandler;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigHandlerTest
{
  public static final String FILE_PATH = "testConfig.tmp";
  public static final String JSON = "{\"a\": \"olda\",\"configHeader\": {\"name\": \"TestConfig\",\"version\": \"1.0\"}}";

  @Before
  public void createOldVersionFile() throws Exception
  {
    new File(FILE_PATH).getAbsoluteFile().getParentFile().mkdirs();
    Files.write(Paths.get(FILE_PATH), JSON.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
  }

  @After
  public void deleteOldVersionFile() throws Exception
  {
    new File(FILE_PATH).delete();
  }

  @Test
  public void testLoadConfig() throws Exception
  {
    TestConfig cfg = ConfigHandler.loadConfig(FILE_PATH, TestConfig.class);
    assertEquals("ewq", cfg.b);
    assertEquals("OLDA", cfg.c);
  }
}
