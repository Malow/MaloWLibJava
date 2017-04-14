package com.github.malow.malowlib.database;

public class DatabaseExceptions
{
  public static class UniqueException extends Exception
  {
    private static final long serialVersionUID = 1L;
    public String fieldName;
    public String value;

    public UniqueException(String fieldName, String value)
    {
      super();
      this.fieldName = fieldName;
      this.value = value;
    }

    @Override
    public String toString()
    {
      return "FieldConstraintException " + this.getMessage();
    }

    @Override
    public String getMessage()
    {
      return "A row already exists with the field " + this.fieldName + " containing the unique value " + this.value;
    }
  }

  public static class MissingMandatoryFieldException extends Exception
  {
    private static final long serialVersionUID = 1L;
    public String fieldName;

    public MissingMandatoryFieldException(String fieldName)
    {
      super();
      this.fieldName = fieldName;
    }

    @Override
    public String toString()
    {
      return "MissingMandatoryFieldException " + this.getMessage();
    }

    @Override
    public String getMessage()
    {
      return "The field " + this.fieldName + " is mandatory but it was null.";
    }
  }

  public static class ForeignKeyException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  public static class ZeroRowsReturnedException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  public static class MultipleRowsReturnedException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  public static class UnexpectedException extends Exception
  {
    private static final long serialVersionUID = 1L;

    public UnexpectedException(String error, Exception e)
    {
      super(error, e);
    }
  }
}
