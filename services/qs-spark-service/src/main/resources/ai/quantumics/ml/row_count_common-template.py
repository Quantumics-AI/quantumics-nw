from pyspark.sql import SparkSession
import logging

def: row_count(appName, bucketName, filePath)
     try:
         # Initialize a Spark session
         spark = SparkSession.builder.appName(appName).getOrCreate()

         # Read the CSV file from S3 into a DataFrame
         df = spark.read.csv(f"s3a://{bucketName}/{filepath}", header=True, inferSchema=True)

         # Calculate the number of records in the DataFrame
         record_count = df.count()
         return record_count

     except Exception as e:
         # Log any exceptions
         logging.error(f"An error occurred : {str(e)}")