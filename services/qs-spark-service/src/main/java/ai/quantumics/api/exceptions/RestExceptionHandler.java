/*
 * Copyright (c) 2020. Quantumics.ai, http://quantumics.ai.
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 */

package ai.quantumics.api.exceptions;

import javax.persistence.EntityNotFoundException;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import static ai.quantumics.api.constants.DatasourceConstants.CONNECTION_FAILED;
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		String error = "Malformed JSON request";
		return buildResponseEntity(new ApiError(HttpStatus.BAD_REQUEST, error));
	}

	private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
		return new ResponseEntity<>(apiError, apiError.getStatus());
	}

	// other exception handlers below

	@ExceptionHandler(EntityNotFoundException.class)
	protected ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(ex.getMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(NullPointerException.class)
	protected ResponseEntity<Object> handleNullPointer(NullPointerException ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(ProjectNotFoundException.class)
	protected ResponseEntity<Object> projectNotFoundException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(QuantumsparkUserNotFound.class)
	protected ResponseEntity<Object> userNotFoundException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(BadRequestException.class)
	protected ResponseEntity<Object> badRequestException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
	  org.springframework.web.bind.MethodArgumentNotValidException ex, 
	  HttpHeaders headers, 
	  HttpStatus status, 
	  WebRequest request) {
		List<String> errorList = new ArrayList<>();
		ex.getBindingResult().getFieldErrors().forEach(error -> {
		errorList.add(error.getDefaultMessage());
	});

	    ApiError apiError = 
	      new ApiError(HttpStatus.BAD_REQUEST, errorList.toString());
	    return handleExceptionInternal(
	      ex, apiError, headers, apiError.getStatus(), request);
	}
	
	
	@ExceptionHandler(InvalidAccessTypeException.class)
	protected ResponseEntity<Object> invalidAccessTypeException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(DatasourceNotFoundException.class)
	protected ResponseEntity<Object> datasourceNotFoundException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(BucketNotFoundException.class)
	protected ResponseEntity<Object> bucketNotFoundException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(AmazonS3Exception.class)
	protected ResponseEntity<Object> amazonS3xception(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(AWSSecurityTokenServiceException.class)
	protected ResponseEntity<Object> securityTokenException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.FORBIDDEN);
		apiError.setMessage(CONNECTION_FAILED);
		return buildResponseEntity(apiError);
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<Object> genericException(Exception ex) {
		log.error(ex.getLocalizedMessage());
		ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR);
		apiError.setMessage(ex.getLocalizedMessage());
		return buildResponseEntity(apiError);
	}
}
