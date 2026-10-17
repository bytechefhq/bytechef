import {TEST_USER} from '../constants';

export const ENVIRONMENT_IDS = {
    DEVELOPMENT: 0,
    PRODUCTION: 2,
    STAGING: 1,
} as const;

export type EnvironmentNameType = keyof typeof ENVIRONMENT_IDS;

export interface UserCredentialsI {
    email: string;
    password: string;
}

function trimTrailingSlash(url: string): string {
    return url.replace(/\/+$/, '');
}

function readCredentials(emailVariable: string, passwordVariable: string): UserCredentialsI | undefined {
    const email = process.env[emailVariable];
    const password = process.env[passwordVariable];

    if (!email || !password) {
        return undefined;
    }

    return {email, password};
}

const baseUrl = trimTrailingSlash(process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173');

export const MCP_TEST_CONFIGURATION = {
    admin: {
        email: process.env.BYTECHEF_E2E_ADMIN_EMAIL || TEST_USER.email,
        password: process.env.BYTECHEF_E2E_ADMIN_PASSWORD || TEST_USER.password,
    } satisfies UserCredentialsI,
    baseUrl,
    editor: readCredentials('BYTECHEF_E2E_EDITOR_EMAIL', 'BYTECHEF_E2E_EDITOR_PASSWORD'),
    memberWorkspaceId: process.env.BYTECHEF_E2E_MEMBER_WORKSPACE_ID,
    publicUrl: trimTrailingSlash(process.env.BYTECHEF_E2E_PUBLIC_URL || baseUrl),
    viewer: readCredentials('BYTECHEF_E2E_VIEWER_EMAIL', 'BYTECHEF_E2E_VIEWER_PASSWORD'),
};

export const MEMBER_CREDENTIALS_MISSING_REASON =
    'Set BYTECHEF_E2E_EDITOR_*, BYTECHEF_E2E_VIEWER_* and BYTECHEF_E2E_MEMBER_WORKSPACE_ID to run the workspace member cases';

export function hasWorkspaceMembers(): boolean {
    return Boolean(
        MCP_TEST_CONFIGURATION.editor && MCP_TEST_CONFIGURATION.viewer && MCP_TEST_CONFIGURATION.memberWorkspaceId
    );
}
