import type {CodegenConfig} from '@graphql-codegen/cli';
import {getCookie} from './src/shared/util/cookie-utils';

const config: CodegenConfig = {
    schema: [
        '../server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-data-table/**/src/main/resources/graphql/*.graphqls',
        '../server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-mcp/automation-mcp-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/automation/automation-task/automation-task-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/platform/platform-security/platform-security-graphql/src/main/resources/graphql/*.graphqls',
        '../server/libs/ai/mcp/mcp-server-configuration/mcp-server-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/platform/platform-user/platform-user-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/embedded/embedded-connected-user/embedded-connected-user-graphql/src/main/resources/graphql/*.graphqls',
    ],
    documents: ['src/graphql/**/*.graphql'],
    generates: {
        'src/shared/middleware/graphql.ts': {
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
            config: {
                fetcher: {
                    endpoint: 'endpointUrl',
                    fetchParams: 'fetchParams',
                },
                reactQueryVersion: 5,
            },
        },
    },
    hooks: {
        afterAllFileWrite: ['prettier --write'],
    },
};

export default config;
