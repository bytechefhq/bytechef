import {describe, expect, it} from 'vitest';

import {isMissingDefinitionLookup} from '../missingDefinitionLookup';

describe('isMissingDefinitionLookup', () => {
    it.each([
        'http://localhost/api/platform/internal/component-definitions/viber/versions/1',
        'http://localhost/api/platform/internal/component-definitions/infobip/versions/1/action-definitions/sendViberBotTextMessage',
        'http://localhost/api/platform/internal/component-definitions/viber/versions/1/trigger-definitions/newMessage',
        'http://localhost/api/platform/internal/component-definitions/ai/versions/1/cluster-element-definition/model',
        'http://localhost/api/platform/internal/component-definitions/viber/versions/1?environmentId=0',
    ])('matches a 400 definition lookup: %s', (url) => {
        expect(isMissingDefinitionLookup(url, 400)).toBe(true);
    });

    it('does not match a server error on a definition lookup', () => {
        expect(
            isMissingDefinitionLookup(
                'http://localhost/api/platform/internal/component-definitions/viber/versions/1',
                500
            )
        ).toBe(false);
    });

    it.each([
        'http://localhost/api/platform/internal/component-definitions/viber/versions',
        'http://localhost/api/platform/internal/component-definitions/viber/versions/1/action-definitions',
        'http://localhost/api/platform/internal/component-definitions/viber/connection-definition',
        'http://localhost/api/platform/internal/workflow-nodes/viber_1/test-outputs',
    ])('does not match other requests: %s', (url) => {
        expect(isMissingDefinitionLookup(url, 400)).toBe(false);
    });
});
