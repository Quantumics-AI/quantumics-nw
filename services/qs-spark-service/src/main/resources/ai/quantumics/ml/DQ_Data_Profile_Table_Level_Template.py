from pyspark.sql import SparkSession
from pyspark.sql.functions import col, to_date, expr, udf, when
from pyspark.sql.types import BooleanType
import json
import re
import boto3

# Initialize a Spark session
spark = SparkSession.builder.appName("Quantumics").getOrCreate()

# Define your S3 bucket and file paths here
bucket1 = $SOURCE_BUCKET
filepath1 = $SOURCE_PATH
bucket2 = $TARGET_BUCKET
filepath2 = $TARGET_PATH
rule_type_name = $RULE_TYPE_NAME
level_name = $LEVEL_NAME
s3Path = $S3_OUTPUT_PATH

# Read the CSV files without specifying a date format
source_df = spark.read.csv(f"s3a://{bucket1}/{filepath1}", header=True, inferSchema=True)
target_df = spark.read.csv(f"s3a://{bucket2}/{filepath2}", header=True, inferSchema=True)

if source_df.count() == 0 and target_df.count() == 0:
    print("Neither file has records.")
else:
    date_format_regex = r"^\d{1,2}/\d{1,2}/\d{4}"

    # Create a mapping from the original data types to the desired types
    data_type_mapping = {
        "StringType": "String",
        "IntegerType": "Integer",
        "DoubleType": "Double",
        "Date": "Date",
    }

    # Function to get the file size from S3 using boto3
    def get_file_size_boto3(bucket_name, s3_key):
        s3 = boto3.client('s3')
        response = s3.head_object(Bucket=bucket_name, Key=s3_key)
        return response['ContentLength']

    def get_size(size):
        if size < 1024:
            return f"{size} bytes"
        elif size < pow(1024, 2):
            return f"{round(size / 1024, 2)} KB"
        elif size < pow(1024, 3):
            return f"{round(size / (pow(1024, 2)), 2)} MB"
        elif size < pow(1024, 4):
            return f"{round(size / (pow(1024, 3)), 2)} GB"

    def is_date_column(df, column, date_format_regex):
        if column in df.columns:
            return when(col(column).rlike(date_format_regex), True).otherwise(False)
        return False

    def map_data_type(data_type):
        return data_type_mapping.get(str(data_type), "-")

    source_file_size = get_size(get_file_size_boto3(bucket1, filepath1))
    target_file_size = get_size(get_file_size_boto3(bucket2, filepath2))

    # Calculate the number of rows and columns for source and target
    source_rows = source_df.count()
    target_rows = target_df.count()
    source_cols = len(source_df.columns)
    target_cols = len(target_df.columns)

    # Create column-level statistics for source and target
    column_stats = []

    for column in source_df.columns:
        source_dtype = source_df.schema[column].dataType
        isSourceDateColumn = is_date_column(source_df, column, date_format_regex)
        source_df_filtered = source_df.filter(isSourceDateColumn)
        if source_df_filtered.count() > 0:
            source_dtype = "Date"

        if column in target_df.columns:
            target_dtype = target_df.schema[column].dataType
            isTargetDateColumn = is_date_column(target_df, column, date_format_regex)
            target_df_filtered = target_df.filter(isTargetDateColumn)
            if target_df_filtered.count() > 0:
                target_dtype = "Date"
            match = source_dtype == target_dtype
        else:
            target_dtype = "-"
            match = False
        column_stats.append({
            "columnName": column,
            "source": map_data_type(str(source_dtype)),
            "target": map_data_type(str(target_dtype)),
            "match": match
        })

    # Calculate the number of matching columns in the target
    matching_columns_count = sum(col["match"] for col in column_stats)

    # Determine if the individual checks are true
    total_columns_match = source_cols == target_cols
    total_rows_match = source_rows == target_rows
    file_size_match = source_file_size == target_file_size
    matching_data_type_columns_match = all(col["match"] for col in column_stats)

    # Determine if the root level "match" is false
    root_match = total_columns_match and total_rows_match and matching_data_type_columns_match and file_size_match

    # Prepare the JSON structure
    output_json = {
        "match": root_match,
        "pass": root_match,
        "ruleTypeName": rule_type_name,
        "levelName": level_name,
        "sourceFile": f"s3a://{bucket1}/{filepath1}",
        "targetFile": f"s3a://{bucket2}/{filepath2}",
        "data": [
            {
                "dataset": "Total Columns",
                "source": source_cols,
                "target": target_cols,
                "match": total_columns_match
            },
            {
                "dataset": "Total Rows",
                "source": source_rows,
                "target": target_rows,
                "match": total_rows_match
            },
            {
                "dataset": "Matching Data Type Columns",
                "source": source_cols,
                "target": matching_columns_count,
                "match": matching_data_type_columns_match,
                "columns": column_stats
            },
            {
                "dataset": "Size",
                "source": source_file_size,
                "target": target_file_size,
                "match": file_size_match
            }
        ]
    }

    # Prepare job_output as a JSON string
    job_output = json.dumps(output_json)

    # Print the counts and job_output
    print("Job Output:")
    print(job_output, flush=True)

    job_output_df = spark.createDataFrame([(job_output,)], ["jobOutput"])

    job_output_df.repartition(1).write.format('json').options(mode='overwrite').json(s3Path)
