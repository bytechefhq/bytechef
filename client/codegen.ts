import type {CodegenConfig} from '@graphql-codegen/cli';
import {getCookie} from './src/shared/util/cookie-utils';

const config: CodegenConfig = {
    schema: [
        {
            'http://localhost:9555/graphql': {
                headers: {
                    Authorization: 'Basic YWRtaW5AbG9jYWxob3N0LmNvbTphZG1pbg==',
                    'Content-Type': 'application/json',
                },
            },
        },
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
