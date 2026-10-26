import type {ConnectionCredentialStoreType} from '@/shared/middleware/graphql';

export const connectionCredentialStoreLabels: Record<ConnectionCredentialStoreType, string> = {
    AWS_SECRETS_MANAGER: 'AWS Secrets Manager',
    DATABASE: 'Database',
    HASHICORP_VAULT: 'HashiCorp Vault',
};
