from pyspark.sql import SparkSession
from pyspark.sql import functions as F
from datetime import datetime
from pyspark.sql.types import StructType, StructField, IntegerType, BooleanType, StringType
import json
import os

# Initialize a Spark session
spark = SparkSession.builder.appName("EDI-DQ-RULE).getOrCreate()

bucket1 = $SOURCE_BUCKET
filepath1 = $SOURCE_PATH
bucket2 = $TARGET_BUCKET
filepath2 = $TARGET_PATH
rule_type_name = $RULE_TYPE_NAME
level_name = $LEVEL_NAME
s3Path= $S3_OUTPUT_PATH
input_acceptance_percentage = $ACCEPTANCE_PER
input_acceptance_percentage_float = float(input_acceptance_percentage)

file_paths = [
    (filepath1, bucket1),
    (filepath2, bucket2)
]

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

# Check if neither file has records based on the record counts in the DataFrame
if count_results_df.collect()[0]["record_count"] == 0 or count_results_df.collect()[1]["record_count"] == 0:
    if count_results_df.collect()[0]["record_count"] == 0:
        print("Source file does not have records.")
    if count_results_df.collect()[1]["record_count"] == 0:
        print("Target file does not have records.")
else:
    source_count = count_results_df.collect()[0]["record_count"]
    target_count = count_results_df.collect()[1]["record_count"]
    max_count = max(source_count, target_count)
    percentage_difference = (abs(source_count - target_count) / max_count) * 100.0
    # Check if the percentage difference is higher than the input acceptance percentage
    pass_status = percentage_difference <= input_acceptance_percentage_float
    # Calculate whether the counts match or not
    match = count_results_df.collect()[0]["record_count"] == count_results_df.collect()[1]["record_count"]
    source_s3_path = f"s3a://{bucket1}/{filepath1}"
    target_s3_path = f"s3a://{bucket2}/{filepath2}"
    source_file_name = os.path.basename(s3_file_path)
    target_file_name = os.path.basename(target_s3_path)

    # Prepare job_output as a JSON string
    job_output = json.dumps({
        "source": count_results_df.collect()[0]["record_count"],
        "target": count_results_df.collect()[1]["record_count"],
        "match": match,
        "ruleTypeName": rule_type_name,
        "levelName": level_name,
        "pass": pass_status,
        "SourceFile": source_s3_path,
        "TargetFile": target_s3_path,
        "SourceFileName": source_file_name,
        "TargetFileName": target_file_name
    })

    # Print the counts and job_output
    print("Job Output:")
    print(job_output, flush=True)

    job_output_df = spark.createDataFrame([(job_output,)], ["jobOutput"])

    job_output_df.repartition(1).write.format('json').options(mode='overwrite').json(s3Path)

