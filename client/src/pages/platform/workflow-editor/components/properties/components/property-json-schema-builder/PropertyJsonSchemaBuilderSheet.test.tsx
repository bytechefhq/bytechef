import {i18n} from '@lingui/core';
import {I18nProvider} from '@lingui/react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

const {handleCopilotOpen} = vi.hoisted(() => ({handleCopilotOpen: vi.fn()}));

vi.mock('./hooks/usePropertyJsonSchemaBuilderCopilot', () => ({
    usePropertyJsonSchemaBuilderCopilot: () => ({
        copilotPanelOpen: false,
        handleCopilotClose: vi.fn(),
        handleCopilotOpen,
    }),
}));
vi.mock('@/shared/components/copilot/CopilotPanel', () => ({default: () => <div data-testid="copilot-panel" />}));
vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (s: unknown) => unknown) => selector({ai: {copilot: {enabled: true}}}),
}));
vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({useFeatureFlagsStore: () => () => true}));
vi.mock('../property-copilot/useGeneratePropertyValue', () => ({
    useGeneratePropertyValue: () => ({generate: vi.fn(), isPending: false}),
}));
vi.mock('@/components/JsonSchemaBuilder/JsonSchemaBuilder', () => ({
    default: () => <div data-testid="json-schema-builder" />,
}));

import PropertyJsonSchemaBuilderSheet from './PropertyJsonSchemaBuilderSheet';

i18n.load('en', {});
i18n.activate('en');

const wrapper = ({children}: {children: ReactNode}) => <I18nProvider i18n={i18n}>{children}</I18nProvider>;

describe('PropertyJsonSchemaBuilderSheet copilot toggle', () => {
    it('opens the copilot when the toggle is clicked', async () => {
        const user = userEvent.setup();

        render(
            <PropertyJsonSchemaBuilderSheet
                environmentId={1}
                propertyPath="output"
                title="Response Schema"
                workflowId="w1"
                workflowNodeName="node1"
            />,
            {wrapper}
        );

        await user.click(screen.getByRole('button', {name: /copilot/i}));

        expect(handleCopilotOpen).toHaveBeenCalled();
    });
});
