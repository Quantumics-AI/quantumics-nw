from pyspark.sql import SparkSession
from pyspark.sql import functions as F
import json
import os

# Initialize a Spark session
spark = SparkSession.builder.appName("EDI-DQ-RULE_DUPLICATE-VALUE-COLUMN").getOrCreate()

s3_bucket_name = $SOURCE_BUCKET
s3_file_path = $SOURCE_PATH
rule_type_name = $RULE_TYPE_NAME
level_name = $LEVEL_NAME
s3Path = $S3_OUTPUT_PATH
input_headers_str = $COLUMNS
input_acceptance_percentage = $ACCEPTANCE_PER
input_acceptance_percentage_float = float(input_acceptance_percentage)

# Convert the input headers string into an array of strings
input_headers = input_headers_str.split(",")

# Read the CSV file from S3 into a DataFrame
df = spark.read.csv(f"s3a://{s3_bucket_name}/{s3_file_path}", header=True, inferSchema=True)

if input_headers_str.strip().lower() == "all":
    df = df.na.drop()
else:
    df = df.select(*input_headers)
    df = df.na.drop()

# Check if there are no records in the DataFrame
if df.count() == 0:
    print("No records found in the file.")
else:
    if input_headers_str.strip().lower() == "all":
        # Group the DataFrame by all columns and count the number of rows in each group
        grouped_df = df.groupBy(df.columns).count()
    else:
        # Group the DataFrame by the specified list of columns and count the number of rows in each group
        grouped_df = df.groupBy(*input_headers).count()

    # Filter for rows with a count greater than 1 to identify duplicates
    duplicate_df = grouped_df.filter(grouped_df["count"] > 1)

    # Calculate the number of duplicate rows
    duplicate_count = duplicate_df.count()

    # Calculate the total number of rows in the DataFrame
    total_count = df.count()

    # Calculate the percentage of duplicate rows
    duplicate_percentage = (duplicate_count / total_count) * 100.0

    # Check if the duplicate percentage is within the acceptance criteria
    pass_status = duplicate_percentage <= input_acceptance_percentage_float

    source_s3_path = f"s3a://{s3_bucket_name}/{s3_file_path}"
    source_file_name = os.path.basename(s3_file_path)
    # Prepare response in the specified format
    response = {
        "source": duplicate_count,
        "match": pass_status,
        "ruleTypeName": rule_type_name,
        "levelName": level_name,
        "pass": pass_status,
        "SourceFile": source_s3_path,
        "SourceFileName": source_file_name,
        "headers": input_headers_str
    }

    # Prepare job_output as a JSON string
    job_output = json.dumps(response)

    # Print the counts and job_output
    print("Job Output:")
    print(job_output, flush=True)

    job_output_df = spark.createDataFrame([(job_output,)], ["jobOutput"])

    job_output_df.repartition(1).write.format('json').options(mode='overwrite').json(s3Path)
