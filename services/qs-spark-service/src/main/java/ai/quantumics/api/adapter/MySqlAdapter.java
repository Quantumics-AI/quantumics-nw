/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.adapter;

import ai.quantumics.api.req.DataBaseRequest;
import ai.quantumics.api.util.ResultSetToJsonMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
//@NoArgsConstructor
//@AllArgsConstructor
public class MySqlAdapter {
  /*private String hostname;
  private String username;
  private String password;
  private String defaultDB;
  private String serviceName;
  private int port;

  public MySqlAdapter(DataBaseRequest request) {
    hostname = request.getHostName();
    username = request.getUserName();
    password = request.getPassword();
    defaultDB = request.getDbName();
    port = request.getPort();
  }

  public Connection getConnection() {
    Connection mySqlConn;
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      mySqlConn = DriverManager.getConnection(getConnectionString(), username, password);
    } catch (final Exception e) {
      log.error("Exception while establishing connection with MYSQL DB {}", e.getMessage());
      mySqlConn = null;
    }
    return mySqlConn;
  }

  private String getConnectionString() {
    return String.format("jdbc:mysql://%s:%d/%s", hostname, port, defaultDB);
  }

  public JSONArray getTables() throws SQLException {
    JSONArray tableNames = new JSONArray();
    try (Connection connection = getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(getSqlQuery())) {
      tableNames = ResultSetToJsonMapper.mapResultSet(resultSet);
    } catch (final Exception e) {
      log.error("Exception occurred while executing statement {}", e.getMessage());
    }
    return tableNames;
  }

  private String getSqlQuery() {
    return "SELECT table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' ORDER BY table_name;";
  }

  public boolean testConnection() {
    return getConnection() != null;
  }*/
}
