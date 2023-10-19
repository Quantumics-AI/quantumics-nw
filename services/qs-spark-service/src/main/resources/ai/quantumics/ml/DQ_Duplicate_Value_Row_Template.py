from pyspark.sql import SparkSession
from pyspark.sql import functions as F
import json

# Initialize a Spark session
spark = SparkSession.builder.appName("Quantumics").getOrCreate()

s3_bucket_name = $SOURCE_BUCKET
s3_file_path = $SOURCE_PATH
rule_type_name = $RULE_TYPE_NAME
level_name = $LEVEL_NAME
s3Path = $S3_OUTPUT_PATH
input_acceptance_percentage = $ACCEPTANCE_PER
input_acceptance_percentage_float = float(input_acceptance_percentage)

# Read the CSV file from S3 into a DataFrame
df = spark.read.csv(f"s3a://{s3_bucket_name}/{s3_file_path}", header=True, inferSchema=True)

df = df.na.drop()

# Check if there are no records in the DataFrame
if df.count() == 0:
    print("No records found in the file.")
else:
    # Group the DataFrame by all columns and count the number of rows in each group
    grouped_df = df.groupBy(df.columns).count()

    # Filter for rows with a count greater than 1 to identify duplicates
    duplicate_df = grouped_df.filter(grouped_df["count"] > 1)

    # Calculate the number of duplicate rows
    duplicate_count = duplicate_df.count()

    # Collect and list the duplicate rows
    # duplicate_rows = duplicate_df.join(df, df.columns, "inner").select(df.columns)

    # Calculate the total number of rows in the DataFrame
    total_count = df.count()

    # Calculate the percentage of duplicate rows
    duplicate_percentage = (duplicate_count / total_count) * 100.0

    # Check if the duplicate percentage is within the acceptance criteria
    pass_status = duplicate_percentage <= input_acceptance_percentage_float

    source_s3_path = f"s3a://{s3_bucket_name}/{s3_file_path}"

    # Prepare response in the specified format
    response = {
        "source": duplicate_count,
        "match": pass_status,
        "ruleTypeName": rule_type_name,
        "levelName": level_name,
        "pass": pass_status,
        "SourceFile": source_s3_path
    }

    # Prepare job_output as a JSON string
    job_output = json.dumps(response)

    # Print the counts and job_output
    print("Job Output:")
    print(job_output, flush=True)

    job_output_df = spark.createDataFrame([(job_output,)], ["jobOutput"])

    job_output_df.repartition(1).write.format('json').options(mode='overwrite').json(s3Path)
