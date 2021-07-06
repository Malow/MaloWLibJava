package com.github.malow.malowlib.network.https;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.github.malow.malowlib.GsonSingleton;
import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.MaloWUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class HttpRequestHandler<RequestClass extends JsonHttpRequest> implements HttpHandler
{
  public static class BadRequestException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  private Class<RequestClass> requestClass;

  public HttpRequestHandler()
  {
    try
    {
      this.requestClass = MaloWUtils.getGenericClassForParent(this);
    }
    catch (ClassNotFoundException e)
    {
      MaloWLogger.error("Failed to get RequestClass for HttpsJsonPostHandler", e);
    }
  }

  @Override
  public void handle(HttpExchange t)
  {
    String stringRequest = getStringRequest(t);
    try
    {
      RequestClass request = this.createValidJsonRequest(stringRequest);
      JsonHttpResponse response = this.handleRequestAndGetResponse(request);
      String stringResponse = GsonSingleton.toJson(response);
      if (stringResponse != null)
      {
        sendResponse(t, 200, stringResponse);
      }
      else
      {
        sendResponse(t, 500, "Unexpected internal error");
      }
    }
    catch (BadRequestException e)
    {
      MaloWLogger.info("Bad request received at " + t.getRequestURI().toString() + ": " + stringRequest);
      sendResponse(t, 400, "{\"result\":false,\"error\":\"400: Bad Request\"}");
    }
  }

  public abstract JsonHttpResponse handleRequestAndGetResponse(RequestClass request) throws BadRequestException;

  private static void sendResponse(HttpExchange t, int code, String response)
  {
    try
    {
      t.sendResponseHeaders(code, response.getBytes(StandardCharsets.UTF_8).length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes(StandardCharsets.UTF_8));
      os.close();
    }
    catch (IOException e)
    {
      MaloWLogger.error("HttpsJsonPostHandler failed when trying to send response: " + response, e);
    }
  }

  private RequestClass createValidJsonRequest(String request) throws BadRequestException
  {
    RequestClass req = GsonSingleton.fromJson(request, this.requestClass);
    if (req != null && req.isValid())
    {
      return req;
    }
    throw new BadRequestException();
  }

  private static String getStringRequest(HttpExchange t)
  {
    StringBuilder buffer = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(t.getRequestBody(), "utf-8")))
    {
      int b;
      while ((b = br.read()) != -1)
      {
        buffer.append((char) b);
      }
      return buffer.toString();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed when trying to parse request: " + buffer.toString(), e);
      return null;
    }
  }
}
