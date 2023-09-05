package ai.quantumics.api.repo;

import ai.quantumics.api.model.AWSDatasource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwsConnectionRepo extends JpaRepository<AWSDatasource, Integer> {

	List<AWSDatasource> findByDataSourceName(String dataSource);

}
