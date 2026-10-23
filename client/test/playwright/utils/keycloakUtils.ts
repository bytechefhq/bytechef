import {Page, expect, request} from '@playwright/test';

import {AUTHENTICATION_BASE_URL, type IdentityProviderI, uniqueSuffix} from './authenticationUtils';

const KEYCLOAK_URL = process.env.BYTECHEF_E2E_KEYCLOAK_URL || 'http://localhost:18180';

const KEYCLOAK_REALM = 'bytechef-e2e';

export const KEYCLOAK_ISSUER_URI = `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}`;

export const KEYCLOAK_SAML_METADATA_URI = `${KEYCLOAK_ISSUER_URI}/protocol/saml/descriptor`;

export const KEYCLOAK_OIDC_CLIENT = {clientId: 'bytechef-e2e', clientSecret: 'bytechef-e2e-secret'} as const;

export interface KeycloakUserI {
    email: string;
    firstName: string;
    id: string;
    lastName: string;
    password: string;
    username: string;
}

/**
 * Calls the realm's admin API with a fresh admin token each time: master realm tokens live for one minute, shorter than
 * a spec file.
 */
async function keycloakAdminRequest(
    method: 'DELETE' | 'POST',
    path: string,
    data?: unknown
): Promise<{location: string | undefined}> {
    const keycloakContext = await request.newContext();

    try {
        const tokenResponse = await keycloakContext.post(
            `${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token`,
            {
                form: {client_id: 'admin-cli', grant_type: 'password', password: 'admin', username: 'admin'},
            }
        );

        expect(tokenResponse.ok(), `Keycloak admin token: ${tokenResponse.status()}`).toBeTruthy();

        const {access_token: accessToken} = await tokenResponse.json();

        const response = await keycloakContext.fetch(`${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}${path}`, {
            data,
            headers: {Authorization: `Bearer ${accessToken}`},
            method,
        });

        expect(response.ok(), `Keycloak ${method} ${path}: ${response.status()} ${await response.text()}`).toBeTruthy();

        return {location: response.headers()['location']};
    } finally {
        await keycloakContext.dispose();
    }
}

function lastPathSegment(location: string | undefined): string {
    expect(location, 'Keycloak answered without a Location header').toBeTruthy();

    return location!.substring(location!.lastIndexOf('/') + 1);
}

export async function createKeycloakUser(
    domain: string,
    options: {emailVerified?: boolean} = {}
): Promise<KeycloakUserI> {
    const suffix = uniqueSuffix();

    const user = {
        email: `kai-${suffix}@${domain}`,
        firstName: 'Kai',
        lastName: `Keycloak${suffix}`,
        password: 'Keycloak-E2E-1',
        username: `kai-${suffix}`,
    };

    const {location} = await keycloakAdminRequest('POST', '/users', {
        credentials: [{temporary: false, type: 'password', value: user.password}],
        email: user.email,
        emailVerified: options.emailVerified ?? true,
        enabled: true,
        firstName: user.firstName,
        lastName: user.lastName,
        username: user.username,
    });

    return {...user, id: lastPathSegment(location)};
}

export async function deleteKeycloakUser(user: KeycloakUserI | undefined): Promise<void> {
    if (user) {
        await keycloakAdminRequest('DELETE', `/users/${user.id}`).catch(() => undefined);
    }
}

function samlAttributeMapper(userAttribute: string, attributeName: string) {
    return {
        config: {'attribute.name': attributeName, 'attribute.nameformat': 'Basic', 'user.attribute': userAttribute},
        name: attributeName,
        protocol: 'saml',
        protocolMapper: 'saml-user-property-mapper',
    };
}

/**
 * Registers ByteChef as a SAML client of the realm. Keycloak keys SAML clients by the SP entity id, which embeds the
 * registration id the server assigns, so the client can only be created after the identity provider. Returns the
 * client's internal id.
 */
export async function createKeycloakSamlClient(identityProvider: IdentityProviderI): Promise<string> {
    const assertionConsumerServiceUrl = `${AUTHENTICATION_BASE_URL}/login/saml2/sso/${identityProvider.registrationId}`;

    const {location} = await keycloakAdminRequest('POST', '/clients', {
        attributes: {
            'saml.authnstatement': 'true',
            'saml.client.signature': 'false',
            'saml.server.signature': 'true',
            saml_assertion_consumer_url_post: assertionConsumerServiceUrl,
            saml_force_name_id_format: 'true',
            saml_name_id_format: 'email',
        },
        clientId: `${AUTHENTICATION_BASE_URL}/saml2/metadata/${identityProvider.registrationId}`,
        enabled: true,
        protocol: 'saml',
        protocolMappers: [
            samlAttributeMapper('email', 'email'),
            samlAttributeMapper('firstName', 'givenName'),
            samlAttributeMapper('lastName', 'sn'),
        ],
        redirectUris: [assertionConsumerServiceUrl],
    });

    return lastPathSegment(location);
}

export async function deleteKeycloakClient(clientId: string | undefined): Promise<void> {
    if (clientId) {
        await keycloakAdminRequest('DELETE', `/clients/${clientId}`).catch(() => undefined);
    }
}

export async function signInAtKeycloak(page: Page, user: KeycloakUserI): Promise<void> {
    await page.waitForURL((url) => url.href.startsWith(`${KEYCLOAK_ISSUER_URI}/`));

    await page.locator('#username').fill(user.username);

    await page.locator('#password').fill(user.password);

    await page.locator('#kc-login').click();
}
