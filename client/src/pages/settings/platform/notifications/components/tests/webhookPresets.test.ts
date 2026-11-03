import {describe, expect, it} from 'vitest';

import {WEBHOOK_PRESETS} from '../webhookPresets';

const PLACEHOLDER_PATTERN = /\{\{\s*[^}]+?\s*\}\}/g;

function render(template: string, otherPlaceholderValue: string): string {
    return template.replace(PLACEHOLDER_PATTERN, (placeholder) =>
        placeholder.replace(/\s/g, '') === '{{summary}}' ? '{}' : otherPlaceholderValue
    );
}

describe('webhookPresets', () => {
    it('testEveryPresetTemplateRendersToValidJson', () => {
        for (const preset of WEBHOOK_PRESETS) {
            if (!preset.template) {
                continue;
            }

            expect(() => JSON.parse(render(preset.template, 'x'))).not.toThrow();
            expect(() => JSON.parse(render(preset.template, ''))).not.toThrow();
        }
    });

    it('testPresetHeaderValuesAreEmpty', () => {
        for (const preset of WEBHOOK_PRESETS) {
            for (const header of preset.headers) {
                expect(header.value).toBe('');
            }
        }
    });

    it('testPresetOrder', () => {
        expect(WEBHOOK_PRESETS.map((preset) => preset.id)).toEqual(['CUSTOM', 'DATADOG', 'NEW_RELIC']);
    });
});
