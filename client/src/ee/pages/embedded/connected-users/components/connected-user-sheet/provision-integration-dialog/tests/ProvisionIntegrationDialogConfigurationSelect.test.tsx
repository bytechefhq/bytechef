import ProvisionIntegrationDialogConfigurationSelect from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialogConfigurationSelect';
import {ProvisionableIntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const configurations = [
    {integrationInstanceConfiguration: {id: 1, integrationVersion: 3, name: 'Staging'}, selfServe: false},
    {integrationInstanceConfiguration: {id: 2, integrationVersion: 5, name: 'Default'}, selfServe: true},
] as ProvisionableIntegrationInstanceConfiguration[];

beforeEach(() => {
    windowResizeObserver();

    Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.setPointerCapture = vi.fn();
});

it('does not label the self-serve configuration', () => {
    render(<ProvisionIntegrationDialogConfigurationSelect configurations={configurations} onChange={vi.fn()} />);

    expect(screen.queryByText('Self-serve')).not.toBeInTheDocument();
});

it('shows the selected configuration action beside the select', () => {
    render(
        <ProvisionIntegrationDialogConfigurationSelect
            configurations={configurations}
            onChange={vi.fn()}
            selectedConfigurationAction={<button>Connect</button>}
            value={2}
        />
    );

    const configurationSelect = screen.getByRole('combobox', {name: 'Configuration'});

    expect(within(configurationSelect).getByText('Default')).toBeInTheDocument();
    expect(configurationSelect.parentElement).toContainElement(screen.getByRole('button', {name: 'Connect'}));
});

it('shows each configuration version as a badge after its name', async () => {
    const onChange = vi.fn();

    render(<ProvisionIntegrationDialogConfigurationSelect configurations={configurations} onChange={onChange} />);

    await userEvent.click(screen.getByRole('combobox', {name: 'Configuration'}));

    const defaultOption = await screen.findByRole('option', {name: /Default/});

    expect(within(defaultOption).getByText('Default')).toBeInTheDocument();
    expect(within(defaultOption).getByText('V5')).toBeInTheDocument();

    await userEvent.click(defaultOption);

    expect(onChange).toHaveBeenCalledWith(2);
});
