from pyspark.sql import SparkSession
import json
import logging
import os

try:
    # Initialize a Spark session
    spark = SparkSession.builder.appName($APP_RULE_DETAILS).getOrCreate()

    bucket = $SOURCE_BUCKET
    filepath = $SOURCE_PATH
    s3Path= $S3_OUTPUT_PATH

    # Extract the file extension from the file path
    file_extension = os.path.splitext(filepath)[1].lower()

    base_name, file_extension = os.path.splitext(filepath)

    print("Base Name:", base_name)
    print("File Extension:", file_extension)

    # Read the file based on the file extension
    if file_extension == '.csv':
        df = spark.read.csv(f"s3a://{bucket}/{filepath}", header=True, inferSchema=True)
    elif file_extension == '.parquet':
        df = spark.read.parquet(f"s3a://{bucket}/{filepath}")
    else:
        raise ValueError(f"Unsupported file format: {file_extension}")
    # Calculate the number of records in the DataFrame
    record_count = df.count()

    # Check if there are no records in the DataFrame
    if record_count == 0:
        print("No records found in the file.")

    # Prepare job_output as a JSON string
    job_output = json.dumps({
        "rowCount": record_count
    })

    # Print the counts and job_output
    print("Job Output:")
    print(job_output, flush=True)

    job_output_df = spark.createDataFrame([(job_output,)], ["jobOutput"])
    job_output_df.repartition(1).write.format('json').options(mode='overwrite').json(s3Path)

except Exception as e:
    # Log any exceptions
    logging.error(f"An error occurred : {str(e)}")