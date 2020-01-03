package org.granite.rest.handler.serialzation;

import java.time.format.DateTimeFormatter;

public class TimeTools {

  public static DateTimeFormatter getDateFormatter() {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd");
  }

  public static DateTimeFormatter getDateTimeFormatter() {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  }
}
