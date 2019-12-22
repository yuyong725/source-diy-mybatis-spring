package cn.javadog.sd.mybatis.spring;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;

import com.mockrunner.mock.jdbc.MockDataSource;

final class PooledMockDataSource extends MockDataSource {

  private int connectionCount = 0;

  private LinkedList<Connection> connections = new LinkedList<>();

  @Override
  public Connection getConnection() throws SQLException {
    if (connections.isEmpty()) {
      throw new SQLException("Sorry, I ran out of connections");
    }
    ++this.connectionCount;
    return this.connections.removeLast();
  }

  int getConnectionCount() {
    return this.connectionCount;
  }

  void reset() {
    this.connectionCount = 0;
    this.connections.clear();
  }

  @Override
  public void setupConnection(Connection connection) {
    throw new UnsupportedOperationException("used addConnection() instead");
  }

  public void addConnection(Connection c) {
    this.connections.add(c);
  }

}
