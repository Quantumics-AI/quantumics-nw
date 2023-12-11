package ai.quantumics.api.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "qsp_aws_datasource")
@Data
public class AWSDatasource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name = "connection_name")
	private String connectionName;

	@Column(name = "sub_data_source")
	private String subDataSource;

	@Column(name = "access_type")
	private String accessType;

	@Column(name = "bucket_name")
	private String bucketName;

	@Column(name = "region")
	private String region;

	private int userId;
	private int projectId;
	private boolean active;

	private String createdBy;
	private Date createdDate;
	private String modifiedBy;
	private Date modifiedDate;
}
