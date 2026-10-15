import {Form} from '@/components/ui/form';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {Authorization, AuthorizationType} from '@/shared/middleware/platform/configuration';
import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {useForm, useWatch} from 'react-hook-form';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialogBasicStep from '../IntegrationInstanceConfigurationDialogBasicStep';

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect',
    () => ({default: () => <div data-testid="integration-versions-select" />})
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox',
    () => ({default: () => <div data-testid="integrations-combo-box" />})
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepTagsSelect',
    () => ({default: () => <div data-testid="tags-select" />})
);

vi.mock('@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore', () => ({
    useWorkflowsEnabledStore: (selector: (state: Record<string, unknown>) => unknown) => selector({reset: vi.fn()}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 1}),
}));

vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: () => <span data-testid="environment-badge" />,
}));

mockScrollIntoView();

const authorizations: Authorization[] = [
    {title: 'API Key', type: AuthorizationType.ApiKey},
    {type: AuthorizationType.BearerToken},
];

interface BasicStepHarnessPropsI {
    authorizations?: Authorization[];
    curIntegrationId?: number;
    integrationInstanceConfiguration?: IntegrationInstanceConfiguration;
}

const BasicStepHarness = ({
    authorizations,
    curIntegrationId,
    integrationInstanceConfiguration,
}: BasicStepHarnessPropsI) => {
    const form = useForm<IntegrationInstanceConfiguration>({
        defaultValues: {authorizationType: integrationInstanceConfiguration?.authorizationType},
    });

    const authorizationType = useWatch({control: form.control, name: 'authorizationType'});

    return (
        <Form {...form}>
            <IntegrationInstanceConfigurationDialogBasicStep
                authorizations={authorizations}
                control={form.control}
                curIntegrationId={curIntegrationId}
                getValues={form.getValues}
                integrationInstanceConfiguration={integrationInstanceConfiguration}
                setCurIntegrationId={vi.fn()}
                setCurIntegrationVersion={vi.fn()}
                setValue={form.setValue}
            />

            <span data-testid="selected-authorization-type">{authorizationType}</span>
        </Form>
    );
};

beforeAll(() => {
    Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
    Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
});

beforeEach(() => {
    windowResizeObserver();
});

describe('IntegrationInstanceConfigurationDialogBasicStep', () => {
    describe('authorization select', () => {
        it('should list the connection authorizations by title, falling back to the type', async () => {
            render(<BasicStepHarness authorizations={authorizations} curIntegrationId={2} />);

            await userEvent.click(screen.getByRole('combobox', {name: 'Authorization'}));

            expect(screen.getByRole('option', {name: 'API Key'})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: AuthorizationType.BearerToken})).toBeInTheDocument();
        });

        it('should store the selected authorization type in the form', async () => {
            render(<BasicStepHarness authorizations={authorizations} curIntegrationId={2} />);

            await userEvent.click(screen.getByRole('combobox', {name: 'Authorization'}));
            await userEvent.click(screen.getByRole('option', {name: AuthorizationType.BearerToken}));

            expect(screen.getByTestId('selected-authorization-type')).toHaveTextContent(AuthorizationType.BearerToken);
        });

        it('should hide the select when the connection has a single authorization', () => {
            render(<BasicStepHarness authorizations={[authorizations[0]]} curIntegrationId={2} />);

            expect(screen.queryByRole('combobox', {name: 'Authorization'})).not.toBeInTheDocument();
        });

        it('should hide the select until an integration is chosen', () => {
            render(<BasicStepHarness authorizations={authorizations} />);

            expect(screen.queryByRole('combobox', {name: 'Authorization'})).not.toBeInTheDocument();
        });

        it('should make the select read-only when editing an existing configuration', () => {
            render(
                <BasicStepHarness
                    authorizations={authorizations}
                    curIntegrationId={2}
                    integrationInstanceConfiguration={{
                        authorizationType: AuthorizationType.ApiKey,
                        id: 1,
                        integrationId: 2,
                        integrationVersion: 1,
                        name: 'Existing',
                    }}
                />
            );

            const authorizationSelect = screen.getByRole('combobox', {name: 'Authorization'});

            expect(authorizationSelect).toBeDisabled();
            expect(authorizationSelect).toHaveTextContent('API Key');
        });
    });
});
