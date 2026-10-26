import {ConnectionCredentialStoreType} from '@/shared/middleware/graphql';

export const connectionCredentialStoreLabels: Record<ConnectionCredentialStoreType, string> = {
    [ConnectionCredentialStoreType.AwsSecretsManager]: 'AWS Secrets Manager',
    [ConnectionCredentialStoreType.Database]: 'Database',
    [ConnectionCredentialStoreType.HashicorpVault]: 'HashiCorp Vault',
};
