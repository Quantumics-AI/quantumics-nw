package ai.quantumics.api.repo;

import ai.quantumics.api.exceptions.DatasourceNotFoundException;
import ai.quantumics.api.model.AWSDatasource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AwsConnectionRepo extends JpaRepository<AWSDatasource, Integer> {

	Optional<AWSDatasource> findByConnectionNameIgnoreCaseAndActive(String connectionName,boolean active);

	Optional<AWSDatasource> findByIdAndActive(Integer id,boolean active) throws DatasourceNotFoundException;

	Page<AWSDatasource> findByActiveTrueOrderByCreatedDateDesc(Pageable pageable);

	Optional<AWSDatasource> findByConnectionNameIgnoreCaseAndActiveTrue(String connectionName);
	List<AWSDatasource> findByActiveAndConnectionNameStartingWithIgnoreCaseOrActiveAndConnectionNameEndingWithIgnoreCase(boolean active, String connectionName, boolean active1, String connectionName1);
	Page<AWSDatasource> findByActiveTrueAndConnectionNameContainingIgnoreCaseOrderByCreatedDateDesc(String connectionName, Pageable pageable);
	Page<AWSDatasource> findByConnectionNameIgnoreCaseAndActiveTrueOrderByCreatedDateDesc(String connectionName, Pageable pageable);
	Optional<AWSDatasource> findByIdAndConnectionNameIgnoreCaseAndActiveTrue(Integer id, String connectionName);

}
