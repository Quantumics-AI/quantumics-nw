export interface AwsData {
    userId: number;
    projectId: number;
    connectionName: string;
    accessType: string;
    subDataSource: string;
    bucketName: string;
}

export interface RoleData {
    iam: string;
}