from pyspark.sql import SparkSession
from pyspark.sql import functions as F
from datetime import datetime
from pyspark.sql.types import StructType, StructField, IntegerType, BooleanType, StringType
import json

# Initialize a Spark session
spark = SparkSession.builder.appName("Quantumics").getOrCreate()


# AWS S3 configurations
#aws_access_key_id = "your_access_key_id"
#aws_secret_access_key = "your_secret_access_key"
bucket1 = $SOURCE_BUCKET
filepath1 = $SOURCE_PATH

bucket2 = $TARGET_BUCKET
filepath2 = $TARGET_PATH

job_id = $JOB_ID  # Replace with your job ID
rule_id = $RULE_ID # Replace with your rule ID
modified_by = $MODIFIED_BY
db_schema = $DB_SCHEMA
rule_type_name = $RULE_TYPE_NAME
level_name = $LEVEL_NAME

file_paths = [
    (filepath1, bucket1),
    (filepath2, bucket2)
]

# Set AWS credentials
#spark.conf.set("spark.hadoop.fs.s3a.access.key", aws_access_key_id)
#spark.conf.set("spark.hadoop.fs.s3a.secret.key", aws_secret_access_key)

# Create a list to store counts, filenames, and bucket names
file_counts = []

# Loop through the file paths
for s3_file_path, s3_bucket_name in file_paths:
    # Read the CSV file from S3 into a DataFrame
    df = spark.read.csv(f"s3a://{s3_bucket_name}/{s3_file_path}", header=True, inferSchema=True)

    # Calculate the number of records in the DataFrame
    record_count = df.count()

    # Append the count, filename, and bucket name to the list
    file_counts.append((s3_bucket_name, s3_file_path, record_count))

# Create a DataFrame for count results
schema = StructType([
    StructField("bucket_name", StringType(), True),
    StructField("file_path", StringType(), True),
    StructField("record_count", IntegerType(), True),
])
count_results_df = spark.createDataFrame(file_counts, schema)

# Calculate whether the counts match or not
match = count_results_df.collect()[0]["record_count"] == count_results_df.collect()[1]["record_count"]

# Get current UTC date and time
current_utc_datetime = datetime.utcnow()

# Prepare job_output as a JSON string
job_output = json.dumps({
    "source": count_results_df.collect()[0]["record_count"],
    "target": count_results_df.collect()[1]["record_count"],
    "match": match,
    "ruleTypeName": rule_type_name,
    "levelName": level_name
})

# Define job_status (for demonstration purposes)
job_status = "Completed"

# Create a DataFrame for the RuleJob table
if job_id:
    rule_job_data = spark.createDataFrame([(job_id, rule_id, job_status, job_output, current_utc_datetime, modified_by)],
                                          ["job_id", "rule_id", "job_status", "job_output", "modified_date", "modified_by"])
else:
    rule_job_data = spark.createDataFrame([(rule_id, job_status, job_output, current_utc_datetime, modified_by)],
                                          ["rule_id", "job_status", "job_output", "modified_date", "modified_by"])

# Print the counts and job_output
count_results_df.show()
print("Job Output:")
print(job_output)


# Configure JDBC properties for PostgreSQL
database_url = "jdbc:postgresql://qsdev-db.carnmyut5cka.us-east-1.rds.amazonaws.com:5432/opensource"
properties = {
    "user": "rahul",
    "password": "Qsai@1234567890",
    "driver": "org.postgresql.Driver"
}
table_name = f"{db_schema}.qsp_rule_job"
# Write the data to the RuleJob table in PostgreSQL
rule_job_data.write.mode("update").jdbc(database_url, table_name, properties=properties)
