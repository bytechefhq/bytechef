import {TooltipProvider} from '@/components/ui/tooltip';
import useCodeWorkflowHeaderStore from '@/pages/platform/code-workflow/stores/useCodeWorkflowHeaderStore';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import CodeWorkflowHeaderActions from './CodeWorkflowHeaderActions';

// The app mounts one TooltipProvider at the root; these buttons are tooltip triggers.
const renderActions = () =>
    render(
        <TooltipProvider>
            <CodeWorkflowHeaderActions />
        </TooltipProvider>
    );

describe('CodeWorkflowHeaderActions', () => {
    beforeEach(() => {
        useCodeWorkflowHeaderStore.getState().reset();
    });

    it('renders nothing until the editor publishes its state', () => {
        const {container} = renderActions();

        expect(container).toBeEmptyDOMElement();
    });

    it('disables test configuration until the source declares something to configure', () => {
        useCodeWorkflowHeaderStore.getState().setCodeWorkflowHeaderState({
            onSaveClick: () => {},
            onTestConfigurationClick: () => {},
            testConfigurationDisabled: true,
        });

        renderActions();

        expect(screen.getByRole('button', {name: 'Test Configuration'})).toBeDisabled();
    });
});
