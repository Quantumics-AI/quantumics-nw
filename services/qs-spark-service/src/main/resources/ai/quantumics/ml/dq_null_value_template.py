from pyspark.sql import SparkSession
from pyspark.sql import functions as F
import json
import os

# Initialize a Spark session
spark = SparkSession.builder.appName("EDI-DQ-RULE").getOrCreate()

s3_bucket_name = $SOURCE_BUCKET
s3_file_path = $SOURCE_PATH
rule_type_name = $RULE_TYPE_NAME
s3Path = $S3_OUTPUT_PATH
input_headers_str = $COLUMNS
input_acceptance_percentage = $ACCEPTANCE_PER
input_acceptance_percentage_float = float(input_acceptance_percentage)

# Convert the input headers string into an array of strings
input_headers = input_headers_str.split(",")

# Create a list to store header-wise results
header_results = []

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

# Check if there are no records in the DataFrame
if df.count() == 0:
    print("No records found in the file.")
else:
    # Loop through the input headers
    for header in input_headers:
        # Calculate the number of null and empty values in the column
        null_count = df.filter(df[header].isNull() | (F.trim(df[header]) == "")).count()

        # Calculate the total number of rows
        total_count = df.count()

        # Calculate the percentage of null and empty values
        percentage_null_empty = (null_count / total_count) * 100.0

        # Check if the percentage is within the acceptance criteria
        pass_status = percentage_null_empty <= input_acceptance_percentage_float
        source_s3_path = f"s3a://{s3_bucket_name}/{s3_file_path}"
        source_file_name = os.path.basename(s3_file_path)
        header_results.append({
            "source": null_count,
            "header": header,
            "match": pass_status,
            "pass": pass_status,
            "ruleTypeName": rule_type_name,
            "SourceFile": source_s3_path,
            "SourceFileName": source_file_name
        })

    # Prepare response in the specified format
    response = [{"source": r["source"], "header": r["header"], "match": r["match"], "pass": r["pass"], "ruleTypeName": r["ruleTypeName"], "SourceFile": r["SourceFile"], "SourceFileName": r["SourceFileName"]} for r in header_results]

    # Prepare job_output as a JSON string
    job_output = json.dumps(response)

    # Print the counts and job_output
    print("Job Output:")
    print(job_output, flush=True)

    job_output_df = spark.createDataFrame([(job_output,)], ["jobOutput"])

    job_output_df.repartition(1).write.format('json').options(mode='overwrite').json(s3Path)
