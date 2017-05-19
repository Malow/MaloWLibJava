package com.github.malow.malowlib.network.https;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.github.malow.malowlib.GsonSingleton;
import com.github.malow.malowlib.MaloWLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class HttpsJsonPostHandler<RequestClass extends HttpsPostRequest> implements HttpHandler
{
  public static class BadRequestException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  private Class<RequestClass> requestClass;

  @SuppressWarnings("unchecked")
  public HttpsJsonPostHandler()
  {
    try
    {
      Type genericSuperClass = this.getClass().getGenericSuperclass();
      Type type = ((ParameterizedType) genericSuperClass).getActualTypeArguments()[0];
      this.requestClass = (Class<RequestClass>) Class.forName(type.getTypeName());
    }
    catch (Exception e)
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
      HttpsPostResponse response = this.handleRequestAndGetResponse(request);
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

  public abstract HttpsPostResponse handleRequestAndGetResponse(RequestClass request) throws BadRequestException;

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
    String msg = "";
    try (BufferedReader br = new BufferedReader(new InputStreamReader(t.getRequestBody(), "utf-8")))
    {
      int b;
      StringBuilder buf = new StringBuilder();
      while ((b = br.read()) != -1)
      {
        buf.append((char) b);
      }
      msg = buf.toString();
      return msg;
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed when trying to parse request: " + msg, e);
      return null;
    }
  }
}
