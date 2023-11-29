from pyspark.sql import SparkSession
from pyspark.sql import functions as F
import json
import os

# Initialize a Spark session
spark = SparkSession.builder.appName("EDI-DQ-ZERO-ROW-COUNT").getOrCreate()

s3_bucket_name = $SOURCE_BUCKET
s3_file_path = $SOURCE_PATH
rule_type_name = $RULE_TYPE_NAME
level_name = $LEVEL_NAME
s3Path = $S3_OUTPUT_PATH

# Extract the file extension from the file path
file_extension = os.path.splitext(s3_file_path)[1].lower()

base_name, file_extension = os.path.splitext(s3_file_path)

print("Base Name:", base_name)
print("File Extension:", file_extension)

# Read the file based on the file extension
if file_extension == '.csv':
    df = spark.read.csv(f"s3a://{s3_bucket_name}/{s3_file_path}", header=True, inferSchema=True)
elif file_extension == '.parquet':
    df = spark.read.parquet(f"s3a://{s3_bucket_name}/{s3_file_path}")
else:
    raise ValueError(f"Unsupported file format: {file_extension}")

count = 0
pass_status = True
# Check if there are no records in the DataFrame
if df.count() == 0:
    pass_status = False
    print("No records found in the file.")
else:
    count = df.count()

source_s3_path = f"s3a://{s3_bucket_name}/{s3_file_path}"
source_file_name = os.path.basename(s3_file_path)
# Prepare response in the specified format
response = {
    "source": count,
    "match": pass_status,
    "ruleTypeName": rule_type_name,
    "levelName": level_name,
    "pass": pass_status,
    "SourceFile": source_s3_path,
    "SourceFileName": source_file_name
    }

# Prepare job_output as a JSON string
job_output = json.dumps(response)

# Print the counts and job_output
print("Job Output:")
print(job_output, flush=True)

job_output_df = spark.createDataFrame([(job_output,)], ["jobOutput"])

job_output_df.repartition(1).write.format('json').options(mode='overwrite').json(s3Path)
