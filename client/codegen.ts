import type {CodegenConfig} from '@graphql-codegen/cli';

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
            plugins: ['typescript', 'typescript-operations', 'typescript-react-query'],
            config: {
                fetcher: {
                    endpoint: 'http://localhost:9555/graphql',
                    fetchParams: {
                        headers: {
                            'Content-Type': 'application/json',
                        },
                    },
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
