package com.d4dl.handler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class RDSUtil {


  public static Connection getConnection() {
    Connection connection;

    Properties connectionProps = new Properties();
    connectionProps.put("user", System.getenv("RDS_USERNAME"));
    connectionProps.put("password", System.getenv("RDS_PASSWORD"));
    String port = System.getenv("RDS_PORT");
    if (port == null) {
      port = "3306";
    }

    String jdbcUrl = "jdbc:mysql://"
        + System.getenv("RDS_HOSTNAME")
        + ":"
        + port
        + "/"
        + System.getenv("RDS_DB_NAME")
        + "?serverTimezone=UTC";
    try {
      System.out.println("Connecting to: " + jdbcUrl);
      connection = DriverManager.getConnection(jdbcUrl, connectionProps);
      connection.setAutoCommit(false);
      System.out.println("Connection");
    } catch (Exception e) {
      System.err.println("Initial connection creation failed." + e);
      throw new ExceptionInInitializerError(e);
    }

    return connection;
  }
}
