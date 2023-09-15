package ai.quantumics.api.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "qsp_aws_datasource")
@Data
public class AWSDatasource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name = "data_source_name")
	private String dataSourceName;

	@Column(name = "access_type")
	private String accessType;

	@Column(name = "connection_data", columnDefinition = "TEXT")
	private String connectionData;

	private int projectId;
	private int userId;
	private boolean active;

	private Date createdDate;
	private Date modifiedDate;
	private String createdBy;
	private String modifiedBy;
}
