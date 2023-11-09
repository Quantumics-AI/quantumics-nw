from pyspark.sql import SparkSession
from pyspark.sql.functions import col
import json

# Initialize a Spark session
spark = SparkSession.builder.appName("EDI-DQ-RULE_DATA-PROFILE-COLUMN-LEVEL").getOrCreate()

# Replace these with actual values
bucket1 = $SOURCE_BUCKET
filepath1 = $SOURCE_PATH
bucket2 = $TARGET_BUCKET
filepath2 = $TARGET_PATH
rule_type_name = $RULE_TYPE_NAME
level_name = $LEVEL_NAME
s3Path = $S3_OUTPUT_PATH
column = $COLUMNS

# Read the CSV files without specifying a date format
source_df = spark.read.csv(f"s3a://{bucket1}/{filepath1}", header=True, inferSchema=True).select(column)
target_df = spark.read.csv(f"s3a://{bucket2}/{filepath2}", header=True, inferSchema=True).select(column)

# Check if either source_df or target_df has no records
if source_df.count() == 0 or target_df.count() == 0:
    if source_df.count() == 0:
        print("Source file column does not have records.")
    if target_df.count() == 0:
        print("Target file column does not have records.")
else:
    # Create a mapping from the original data types to the desired types
    data_type_mapping = {
        "IntegerType": "Integer",
        "DoubleType": "Double"
    }

    def round_value(value):
        return round(value, 2)

    def map_data_type(data_type):
        return data_type_mapping.get(str(data_type), "-")

    # Define a function to calculate the median for a column in a DataFrame
    def calculate_median(df, column_name):
        # Extract and collect the values from the specified column, filtering out None values
        values = df.select(column_name).filter(col(column_name).isNotNull()).rdd.flatMap(lambda x: x).collect()

        # Check if there are no valid values
        if not values:
            return 0

        # Sort the values
        sorted_values = sorted(values)

        # Calculate the median
        n = len(sorted_values)
        if n % 2 == 0:
            # For an even number of values, take the average of the middle two values
            median = (sorted_values[n // 2 - 1] + sorted_values[n // 2]) / 2
        else:
            # For an odd number of values, take the middle value
            median = sorted_values[n // 2]

        return median

    # Define a function to calculate the mean for a column in a DataFrame
    def calculate_mean(df, column_name):
        # Extract and collect the values from the specified column, filtering out None values
        values = df.select(column_name).filter(col(column_name).isNotNull()).rdd.flatMap(lambda x: x).collect()

        # Check if there are no valid values
        if not values:
            return 0

        # Calculate the mean using PySpark's mean function
        mean = df.selectExpr(f"avg(`{column_name}`)").collect()[0][0]
        return mean

    # Get DataType of column
    source_dtype = map_data_type(str(source_df.schema[column].dataType))
    target_dtype = map_data_type(str(target_df.schema[column].dataType))

    source_total_count = source_df.count()
    target_total_count = target_df.count()

    # missing values and percentage count
    source_missing_count = source_df.filter(col(column).isNull()).count()
    source_missing_percentage = round_value((source_missing_count / source_total_count) * 100)
    target_missing_count = target_df.filter(col(column).isNull()).count()
    target_missing_percentage = round_value((target_missing_count / target_total_count) * 100)

    # Calculate the count of unique values and percentage of unique values
    source_unique_count = source_df.filter(col(column).isNotNull()).select(column).distinct().count()
    source_unique_percentage = round_value((source_unique_count / source_total_count) * 100)
    target_unique_count = target_df.filter(col(column).isNotNull()).select(column).distinct().count()
    target_unique_percentage = round_value((target_unique_count / target_total_count) * 100)

    # Calculate the minimum value in the specified column
    source_min_row = source_df.selectExpr(f"min(`{column}`)").first()
    source_min_value = source_min_row[0] if source_min_row is not None and source_min_row[0] is not None else 0
    target_min_row = target_df.selectExpr(f"min(`{column}`)").first()
    target_min_value = target_min_row[0] if target_min_row is not None and target_min_row[0] is not None else 0

    # Calculate the maximum value in the specified column
    source_max_row = source_df.selectExpr(f"max(`{column}`)").first()
    source_max_value = source_max_row[0] if source_max_row is not None and source_max_row[0] is not None else 0
    target_max_row = target_df.selectExpr(f"max(`{column}`)").first()
    target_max_value = target_max_row[0] if target_max_row is not None and target_max_row[0] is not None else 0

    # Calculate the median for the specified column
    source_median = round_value(calculate_median(source_df, column))
    target_median = round_value(calculate_median(target_df, column))

    # Calculate the mean for the specified column
    source_mean = round_value(calculate_mean(source_df, column))
    target_mean = round_value(calculate_mean(target_df, column))

    # Determine if the individual checks are true
    data_type_match = source_dtype == target_dtype
    column_name_match = column == column
    missing_count_match = source_missing_count == target_missing_count
    missing_percentage_match = source_missing_percentage == target_missing_percentage
    unique_count_match = source_unique_count == target_unique_count
    unique_percentage_match = source_unique_percentage == target_unique_percentage
    min_match = source_min_value == target_min_value
    max_match = source_max_value == target_max_value
    median_match = source_median == target_median
    mean_match = source_mean == target_mean

    # Determine if the root level "match" is false
    root_match = data_type_match and column_name_match and missing_count_match and missing_percentage_match and unique_count_match and unique_percentage_match and min_match and max_match and median_match and mean_match

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
                "dataset": "Date Type",
                "source": source_dtype,
                "target": target_dtype,
                "match": data_type_match
            },
            {
                "dataset": "Column",
                "source": column,
                "target": column,
                "match": column_name_match
            },
            {
                "dataset": "Missing",
                "source": source_missing_count,
                "target": target_missing_count,
                "match": missing_count_match
            },
            {
                "dataset": "Missing %",
                "source": source_missing_percentage,
                "target": target_missing_percentage,
                "match": missing_percentage_match
            },
            {
                "dataset": "Unique",
                "source": source_unique_count,
                "target": target_unique_count,
                "match": unique_count_match
            },
            {
                "dataset": "Unique %",
                "source": source_unique_percentage,
                "target": target_unique_percentage,
                "match": unique_percentage_match
            },
            {
                "dataset": "Minimum",
                "source": source_min_value,
                "target": target_min_value,
                "match": min_match
            },
            {
                "dataset": "Maximum",
                "source": source_max_value,
                "target": target_max_value,
                "match": max_match
            },
            {
                "dataset": "Median",
                "source": source_median,
                "target": target_median,
                "match": median_match
            },
            {
                "dataset": "Mean",
                "source": source_mean,
                "target": target_mean,
                "match": mean_match
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
