package ai.quantumics.api.repo;

import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.model.AWSDatasource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AwsConnectionRepo extends JpaRepository<AWSDatasource, Integer> {

	Optional<AWSDatasource> findByConnectionNameIgnoreCaseAndActive(String connectionName,boolean active);

	Optional<AWSDatasource> findByIdAndActive(Integer id,boolean active) throws DatasourceNotFoundException;

	Optional<List<AWSDatasource>> findByActiveOrderByCreatedDateDesc(boolean active);

	Optional<AWSDatasource> findByConnectionNameIgnoreCaseAndActiveTrue(String connectionName);
	List<AWSDatasource> findByActiveAndConnectionNameStartingWithIgnoreCaseOrActiveAndConnectionNameEndingWithIgnoreCase(boolean active, String connectionName, boolean active1, String connectionName1);

}
