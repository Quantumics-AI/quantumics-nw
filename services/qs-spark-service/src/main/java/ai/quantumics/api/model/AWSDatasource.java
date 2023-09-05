package ai.quantumics.api.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

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

	@Column(name = "connection_type")
	private String connectionType;

	@Column(name = "credential_role", columnDefinition = "TEXT")
	private String credentialOrRole;

	private int projectId;
	private int userId;
	private boolean active;

	@Temporal(TemporalType.TIMESTAMP)
	@CreationTimestamp
	private Date createdDate;

	@Temporal(TemporalType.TIMESTAMP)
	@UpdateTimestamp
	private Date modifiedDate;
	private String createdBy;
	private String modifiedBy;
}
