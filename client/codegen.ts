import type {CodegenConfig} from '@graphql-codegen/cli';

const config: CodegenConfig = {
    documents: ['src/graphql/**/*.graphql'],
    generates: {
        'src/shared/middleware/graphql.ts': {
            config: {
                fetcher: {
                    endpoint: 'endpointUrl',
                    fetchParams: 'fetchParams',
                },
                reactQueryVersion: 5,
            },
            plugins: [
                {
                    add: {
                        content: "import { endpointUrl, fetchParams } from './config';",
                    },
                },
                'typescript',
                'typescript-operations',
                'typescript-react-query',
            ],
        },
    },
    hooks: {
        afterAllFileWrite: ['prettier --write'],
    },
    schema: [
        '../server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-data-table/**/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-knowledge-base/automation-knowledge-base-graphql/src/main/resources/graphql/**/*.graphqls',
        '../server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-mcp/automation-mcp-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-search/automation-search-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-task/automation-task-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/platform/platform-security/platform-security-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/platform/platform-component/platform-component-log/platform-component-log-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/ai/mcp/mcp-server-configuration/mcp-server-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/platform/platform-user/platform-user-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/platform/platform-custom-component/platform-custom-component-configuration/platform-custom-component-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/embedded/embedded-connected-user/embedded-connected-user-graphql/src/main/resources/graphql/*.graphqls',
    ],
};

export default config;
