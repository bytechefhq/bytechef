<?php
/**
 * Registers ByteChef as a SAML service provider under every registration id the specs can produce, for each base URL
 * in SAML_SP_BASE_URLS (comma separated): saml-{id} in single-tenant mode and saml-{tenantId}-{id} in multi-tenant mode.
 * Identity provider ids are assigned by the server, so the specs cannot know them before the IdP container starts.
 */

$baseUrls = array_filter(array_map('trim', explode(',', getenv('SAML_SP_BASE_URLS') ?: 'http://127.0.0.1:5173')));

$registrationIds = array();

for ($id = 1; $id <= 500; $id++) {
    $registrationIds[] = 'saml-' . $id;
}

for ($tenant = 1; $tenant <= 20; $tenant++) {
    for ($id = 1; $id <= 100; $id++) {
        $registrationIds[] = sprintf('saml-%06d-%d', $tenant, $id);
    }
}

foreach ($baseUrls as $baseUrl) {
    $baseUrl = rtrim($baseUrl, '/');

    foreach ($registrationIds as $registrationId) {
        $metadata[$baseUrl . '/saml2/metadata/' . $registrationId] = array(
            'AssertionConsumerService' => $baseUrl . '/login/saml2/sso/' . $registrationId,
        );
    }
}
