import {expect} from '@playwright/test';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {
    EMBEDDED_DATA_ENVIRONMENTS,
    EMBEDDED_ENVIRONMENT_IDS,
    embeddedInternalApi,
    fetchMyEmbeddedScopes,
    openAsEmbeddedRoleUser,
} from '../../utils/embeddedRbacUtils';
import {RBAC_BASE_URL, createRoleUserApiContext} from '../../utils/rbacUtils';

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

test.describe('Embedded RBAC fixtures', () => {
    test('every fixture user can sign in', async ({browser, embeddedRoleUsers}) => {
        for (const [key, roleUser] of Object.entries(embeddedRoleUsers)) {
            await test.step(`sign in as ${key}`, async () => {
                const roleUserApi = await createRoleUserApiContext(RBAC_BASE_URL, roleUser);

                await roleUserApi.dispose();

                const {context, page} = await openAsEmbeddedRoleUser(
                    browser,
                    RBAC_BASE_URL,
                    roleUser,
                    EMBEDDED_ENVIRONMENT_IDS.DEVELOPMENT
                );

                await expect(page).not.toHaveURL(/\/login/);

                await context.close();
            });
        }
    });

    test('VIEWER holds the viewer scopes in every environment and nothing above', async ({embeddedRoleUsers}) => {
        const viewerApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.VIEWER);

        try {
            for (const environment of ['DEVELOPMENT', 'PRODUCTION'] as const) {
                const scopes = await fetchMyEmbeddedScopes(viewerApi, environment);

                expect(scopes).toEqual(expect.arrayContaining(VIEWER_SCOPES));

                for (const scope of [...EDITOR_ONLY_SCOPES, ...ADMIN_ONLY_SCOPES]) {
                    expect(scopes, `${scope} in ${environment}`).not.toContain(scope);
                }
            }
        } finally {
            await viewerApi.dispose();
        }
    });

    test('EDITOR holds the editor scopes but not the admin scopes', async ({embeddedRoleUsers}) => {
        const editorApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.EDITOR);

        try {
            const scopes = await fetchMyEmbeddedScopes(editorApi, 'DEVELOPMENT');

            expect(scopes).toEqual(expect.arrayContaining([...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES]));

            for (const scope of ADMIN_ONLY_SCOPES) {
                expect(scopes, scope).not.toContain(scope);
            }
        } finally {
            await editorApi.dispose();
        }
    });

    test('ADMIN holds every scope level', async ({embeddedRoleUsers}) => {
        const adminRoleApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.ADMIN);

        try {
            const scopes = await fetchMyEmbeddedScopes(adminRoleApi, 'PRODUCTION');

            expect(scopes).toEqual(
                expect.arrayContaining([...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES, ...ADMIN_ONLY_SCOPES])
            );
        } finally {
            await adminRoleApi.dispose();
        }
    });

    test('PRODUCTION_EDITOR holds editor scopes only in PRODUCTION', async ({embeddedRoleUsers}) => {
        const productionEditorApi = await createRoleUserApiContext(RBAC_BASE_URL, embeddedRoleUsers.PRODUCTION_EDITOR);

        try {
            expect(await fetchMyEmbeddedScopes(productionEditorApi, 'PRODUCTION')).toEqual(
                expect.arrayContaining([...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES])
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
            expect(await fetchMyEmbeddedScopes(developmentEditorApi, 'DEVELOPMENT')).toEqual(
                expect.arrayContaining([...VIEWER_SCOPES, ...EDITOR_ONLY_SCOPES])
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
