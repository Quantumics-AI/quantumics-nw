from pyspark.sql import SparkSession
import json

# Initialize a Spark session
spark = SparkSession.builder.appName("Quantumics").getOrCreate()

bucket = $SOURCE_BUCKET
filepath = $SOURCE_PATH
s3Path= $S3_OUTPUT_PATH

# Read the CSV file from S3 into a DataFrame
df = spark.read.csv(f"s3a://{bucket}/{filepath}", header=True, inferSchema=True)

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