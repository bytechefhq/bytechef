import type {CodegenConfig} from '@graphql-codegen/cli';

const config: CodegenConfig = {
    documents: ['src/graphql/**/*.graphql'],
    generates: {
        // Schema types (objects, inputs, enums, scalars) live in their own file. Since
        // @graphql-codegen/typescript-operations v6 re-emits every input/enum it references,
        // combining it with the `typescript` plugin in a single file produces duplicate
        // declarations. The split lets operations import the base types via
        // `importSchemaTypesFrom` instead of re-declaring them.
        'src/shared/middleware/graphql-types.ts': {
            config: {
                scalars: {
                    Any: 'any',
                    Long: 'any',
                    Map: 'any',
                },
            },
            plugins: ['typescript'],
        },
        // Operation types + React Query hooks. Re-exports the schema types so existing
        // imports from `graphql.ts` keep resolving.
        'src/shared/middleware/graphql.ts': {
            config: {
                fetcher: {
                    func: './graphqlFetcher#fetcher',
                },
                // Resolved relative to cwd (not the output file), then made relative to
                // the output dir — so this must be the full cwd-relative path to land on
                // `./graphql-types` next to graphql.ts.
                importSchemaTypesFrom: 'src/shared/middleware/graphql-types',
                reactQueryVersion: 5,
                scalars: {
                    Any: 'any',
                    Long: 'any',
                    Map: 'any',
                },
            },
            plugins: [
                {
                    add: {
                        content: [
                            "export * from './graphql-types';",
                            // The `KnowledgeBase` query generates a `KnowledgeBaseDocument` document
                            // const here, which shadows the same-named schema type re-exported via
                            // `export *` (a local value declaration drops a star-exported entry
                            // entirely, including its type meaning). A local type alias merges with
                            // the value const (separate type/value namespaces), restoring the type.
                            // Add any future collisions here.
                            "import type {KnowledgeBaseDocument as KnowledgeBaseDocumentSchemaType} from './graphql-types';",
                            'export type KnowledgeBaseDocument = KnowledgeBaseDocumentSchemaType;',
                        ].join('\n'),
                    },
                },
                {
                    add: {
                        content: [
                            'export class TypedDocumentString<TResult, TVariables> extends String {',
                            '  __apiType?: { result: TResult; variables: TVariables };',
                            '  __meta__?: Record<string, unknown>;',
                            '',
                            '  constructor(private value: string, __meta__?: Record<string, unknown>) {',
                            '    super(value);',
                            '    this.__meta__ = __meta__;',
                            '  }',
                            '',
                            '  override toString(): string {',
                            '    return this.value;',
                            '  }',
                            '}',
                        ].join('\n'),
                    },
                },
                'typescript-operations',
                'typescript-react-query',
            ],
        },
    },
    hooks: {
        afterAllFileWrite: ['prettier --write'],
    },
    schema: [
        '../server/libs/platform/platform-workflow/platform-workflow-validator/platform-workflow-validator-graphql/src/main/resources/graphql/*.graphqls',
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
        '../server/ee/libs/embedded/embedded-mcp/embedded-mcp-graphql/src/main/resources/graphql/*.graphqls',
        '../server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-eval/platform-ai-agent-eval-graphql/src/main/resources/graphql/**/*.graphqls',
        '../server/libs/platform/platform-ai/platform-ai-skill/platform-ai-skill-graphql/src/main/resources/graphql/**/*.graphqls',
    ],
};

export default config;
