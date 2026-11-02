import {TooltipProvider} from '@/components/ui/tooltip';
import {DEVELOPMENT_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationCreateButton from '../AutomationWorkflowProjectConfigurationCreateButton';

const mocks = vi.hoisted(() => ({
    configurations: {current: [] as Array<{environmentId: string}>},
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: mocks.configurations.current},
    }),
}));

const renderButton = (onClick = vi.fn()) =>
    render(
        <TooltipProvider>
            <AutomationWorkflowProjectConfigurationCreateButton onClick={onClick} projectId="project-1" />
        </TooltipProvider>
    );

describe('AutomationWorkflowProjectConfigurationCreateButton', () => {
    beforeEach(() => {
        windowResizeObserver();

        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
    });

    afterEach(() => {
        mocks.configurations.current = [];

        resetAll();
        vi.clearAllMocks();
    });

    it('opens the create dialog when the current environment has no configuration', async () => {
        const user = userEvent.setup();

        const onClick = vi.fn();

        renderButton(onClick);

        await user.click(screen.getByRole('button', {name: 'New Configuration'}));

        expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('is disabled and explains why when the current environment already has a configuration', async () => {
        const user = userEvent.setup();

        mocks.configurations.current = [{environmentId: String(DEVELOPMENT_ENVIRONMENT)}];

        renderButton();

        expect(screen.getByRole('button', {name: 'New Configuration'})).toBeDisabled();

        await user.hover(screen.getByRole('button', {name: 'New Configuration'}).parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent(
            'This project already has a configuration in DEVELOPMENT.'
        );
    });

    it('stays enabled when only another environment has a configuration', () => {
        mocks.configurations.current = [{environmentId: String(STAGING_ENVIRONMENT)}];

        renderButton();

        expect(screen.getByRole('button', {name: 'New Configuration'})).toBeEnabled();
    });
});
