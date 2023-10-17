/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.constants;

public class DatasourceConstants {

  public static final String DATA_SOURCE_EXIST = "Entered Data Source Name is already used.Please enter a different name";
  public static final String DATA_SOURCE_NOT_EXIST = "Data source does not exist";
  public static final String INVALID_ACCESS_TYPE = "Invalid Access Type";
  public static final String DATA_SOURCE_DELETED = "Data source deleted successfully";
  public static final String EMPTY_BUCKET = "Oops! There is no bucket available under your account";
  public static final String PUBLIC_SCHEMA = "public";
  public static final String Files = "files";
  public static final String CONNECTION_SUCCESSFUL = "Connection Established successfully!";
  public static final String CONNECTION_FAILED = "Connection failed";
  public static final String BUCKET_NOT_EXIST = "The specified bucket does not exist";
  public static final String REGION_PATTERN = "expecting\\s+'(\\S+)'";
  public static final String CSV_EXTENSION = ".csv";
  public static final String FILE_NAME_NOT_NULL = "File name can't be null";
  public static final String CSV_FILE = "Please provide only .csv file";
  public static final String EMPTY_FILE = "File has no content";
  public static final String CORREPTED_FILE = "Please check, the selected file is corrupted";
  private DatasourceConstants() {}

}
