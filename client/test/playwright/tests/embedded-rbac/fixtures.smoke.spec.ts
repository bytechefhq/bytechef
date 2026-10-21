import {type APIRequestContext, expect} from '@playwright/test';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {
    EMBEDDED_DATA_ENVIRONMENTS,
    embeddedInternalApi,
    fetchMyEmbeddedScopes,
    openAsEmbeddedRoleUser,
} from '../../utils/embeddedRbacUtils';
import {ENVIRONMENT_IDS, type EnvironmentType, RBAC_BASE_URL, createRoleUserApiContext} from '../../utils/rbacUtils';

const VIEWER_SCOPES = [
    'EMBEDDED_VIEW',
    'APP_EVENT_VIEW',
    'CONNECTED_USER_VIEW',
    'INTEGRATION_INSTANCE_CONFIGURATION_VIEW',
    'WORKFLOW_VIEW',
    'EXECUTION_VIEW',
    'CONNECTION_VIEW',
    'MCP_VIEW',
];

const EDITOR_ONLY_SCOPES = [
    'INTEGRATION_CREATE',
    'INTEGRATION_PUBLISH',
    'INTEGRATION_INSTANCE_CONFIGURATION_CREATE',
    'INTEGRATION_INSTANCE_CONFIGURATION_EDIT',
    'CONNECTED_USER_MANAGE',
    'WORKFLOW_EDIT',
    'CONNECTION_EDIT',
    'MCP_EDIT',
    'EXECUTION_DETAILS_VIEW',
    'API_KEY_VIEW',
];

const ADMIN_ONLY_SCOPES = ['EMBEDDED_MEMBER_MANAGE', 'INTEGRATION_DELETE', 'MCP_DELETE', 'EXECUTION_DELETE'];

const ALL_ENVIRONMENTS: EnvironmentType[] = ['DEVELOPMENT', 'STAGING', 'PRODUCTION'];

async function expectScopes(
    apiContext: APIRequestContext,
    environment: EnvironmentType,
    expected: string[],
    absent: string[]
): Promise<void> {
    const scopes = await fetchMyEmbeddedScopes(apiContext, environment);

    expect(scopes, `held in ${environment}`).toEqual(expect.arrayContaining(expected));

    for (const scope of absent) {
        expect(scopes, `${scope} in ${environment}`).not.toContain(scope);
    }
}

test.describe('Embedded RBAC fixtures', () => {
    test('every fixture user can sign in', async ({browser, embeddedRoleUsers}) => {
        for (const [key, roleUser] of Object.entries(embeddedRoleUsers)) {
            await test.step(`sign in as ${key}`, async () => {
                const roleUserApi = await createRoleUserApiContext(RBAC_BASE_URL, roleUser);

                try {
                    expect(Array.isArray(await fetchMyEmbeddedScopes(roleUserApi))).toBe(true);
                } finally {
                    await roleUserApi.dispose();
                }

                const {context, page} = await openAsEmbeddedRoleUser(
                    browser,
                    RBAC_BASE_URL,
                    roleUser,
                    ENVIRONMENT_IDS.DEVELOPMENT
                );

                await expect(page).not.toHaveURL(/\/login/);

                await context.close();
            });
        }
    });

    test('VIEWER holds the viewer scopes in every environment and nothing above', async ({embeddedRoleUsers}) => {
        const viewerApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.VIEWER);

        try {
            for (const environment of ALL_ENVIRONMENTS) {
                await expectScopes(viewerApi, environment, VIEWER_SCOPES, [
                    ...EDITOR_ONLY_SCOPES,
                    ...ADMIN_ONLY_SCOPES,
                ]);
            }
        } finally {
            await viewerApi.dispose();
        }
    });

    test('EDITOR holds the editor scopes but not the admin scopes in every environment', async ({
        embeddedRoleUsers,
    }) => {
        const editorApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.EDITOR);

        try {
            for (const environment of ALL_ENVIRONMENTS) {
                await expectScopes(
                    editorApi,
                    environment,
                    [...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES],
                    ADMIN_ONLY_SCOPES
                );
            }
        } finally {
            await editorApi.dispose();
        }
    });

    test('ADMIN holds every scope level in every environment', async ({embeddedRoleUsers}) => {
        const adminRoleApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.ADMIN);

        try {
            for (const environment of ALL_ENVIRONMENTS) {
                await expectScopes(
                    adminRoleApi,
                    environment,
                    [...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES, ...ADMIN_ONLY_SCOPES],
                    []
                );
            }
        } finally {
            await adminRoleApi.dispose();
        }
    });

    test('PRODUCTION_EDITOR holds editor scopes only in PRODUCTION', async ({embeddedRoleUsers}) => {
        const productionEditorApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.PRODUCTION_EDITOR);

        try {
            await expectScopes(
                productionEditorApi,
                'PRODUCTION',
                [...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES],
                ADMIN_ONLY_SCOPES
            );

            expect(await fetchMyEmbeddedScopes(productionEditorApi, 'DEVELOPMENT')).toEqual([]);
        } finally {
            await productionEditorApi.dispose();
        }
    });

    test('DEVELOPMENT_EDITOR holds editor scopes only in DEVELOPMENT', async ({embeddedRoleUsers}) => {
        const developmentEditorApi = await createRoleUserApiContext(
            RBAC_BASE_URL,
            embeddedRoleUsers.DEVELOPMENT_EDITOR
        );

        try {
            await expectScopes(
                developmentEditorApi,
                'DEVELOPMENT',
                [...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES],
                ADMIN_ONLY_SCOPES
            );

            expect(await fetchMyEmbeddedScopes(developmentEditorApi, 'PRODUCTION')).toEqual([]);
        } finally {
            await developmentEditorApi.dispose();
        }
    });

    test('OUTSIDER holds no embedded scope', async ({embeddedRoleUsers}) => {
        const outsiderApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.OUTSIDER);

        try {
            expect(await fetchMyEmbeddedScopes(outsiderApi)).toEqual([]);
            expect(await fetchMyEmbeddedScopes(outsiderApi, 'DEVELOPMENT')).toEqual([]);
            expect(await fetchMyEmbeddedScopes(outsiderApi, 'PRODUCTION')).toEqual([]);
        } finally {
            await outsiderApi.dispose();
        }
    });

    test('embeddedData exposes the created resources to the admin', async ({adminApi, embeddedData}) => {
        for (const environment of EMBEDDED_DATA_ENVIRONMENTS) {
            const environmentData = embeddedData.byEnvironment[environment];

            const instanceConfiguration = await embeddedInternalApi<{id: number}>(
                adminApi,
                'GET',
                `/integration-instance-configurations/${environmentData.instanceConfigurationId}`
            );

            expect(instanceConfiguration.id).toBe(environmentData.instanceConfigurationId);

            const connection = await embeddedInternalApi<{id: number}>(
                adminApi,
                'GET',
                `/connections/${environmentData.connectionId}`
            );

            expect(connection.id).toBe(environmentData.connectionId);
        }

        const workflows = await embeddedInternalApi<{id: string}[]>(
            adminApi,
            'GET',
            `/integrations/${embeddedData.integrationId}/workflows`
        );

        expect(workflows.map((workflow) => workflow.id)).toContain(embeddedData.workflowUuid);
    });
});
