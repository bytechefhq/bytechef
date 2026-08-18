import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {useGetComponentConnectionsQuery} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import HubConnectionDialog from '@/ee/pages/embedded/automation-hub/views/components/HubConnectionDialog';
import {ActivationActionType, ActivationStateI} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {
    AutomationWorkflowProjectComponent,
    AutomationWorkflowProjectWorkflowTemplate,
} from '@/ee/shared/middleware/embedded/public';
import {Dispatch, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

/**
 * One component's account picker. A vendor that sets `connectionDialogAllowed: false` wants its
 * users picking from the connections it shared rather than creating their own, so the Connect
 * button is omitted entirely — leaving the select, which is where a shared connection appears.
 */
interface ConnectAccountRowProps {
    component: AutomationWorkflowProjectComponent;
    highlighted: boolean;
    onSelect: (connectionId: number) => void;
    selectedConnectionId?: number;
}

const ConnectAccountRow = ({component, highlighted, onSelect, selectedConnectionId}: ConnectAccountRowProps) => {
    const [connectionDialogOpen, setConnectionDialogOpen] = useState(false);

    const connectionDialogAllowed = useAutomationHubStore((state) => state.connectionDialogAllowed);

    const {data: connections} = useGetComponentConnectionsQuery(component.name!);

    const componentName = component.name!;
    const componentTitle = component.title || componentName;

    return (
        <div
            className={twMerge('flex flex-col gap-2 rounded-md border p-3', highlighted && 'border-destructive')}
            data-testid={`connect-account-row-${componentName}`}
        >
            <div className="flex items-center gap-2">
                {component.icon && <InlineSVG className="size-4 flex-none" src={component.icon} />}

                <span className="text-sm font-medium">{componentTitle}</span>
            </div>

            <div className="flex items-center gap-2">
                <Select
                    onValueChange={(value) => onSelect(Number(value))}
                    value={selectedConnectionId === undefined ? undefined : String(selectedConnectionId)}
                >
                    <SelectTrigger
                        aria-invalid={highlighted}
                        aria-label={`${componentTitle} connection`}
                        className="w-full"
                    >
                        <SelectValue placeholder={`Select a ${componentTitle} account`} />
                    </SelectTrigger>

                    <SelectContent>
                        {(connections || []).map((connection) => (
                            <SelectItem key={connection.id} value={String(connection.id)}>
                                {connection.name}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>

                {connectionDialogAllowed && (
                    <Button
                        aria-label={`Add a new ${componentTitle} connection`}
                        label="Connect"
                        onClick={() => setConnectionDialogOpen(true)}
                        variant="outline"
                    />
                )}
            </div>

            {highlighted && <p className="text-sm text-destructive">Connect {componentTitle} to continue</p>}

            {connectionDialogOpen && (
                <HubConnectionDialog
                    componentName={componentName}
                    onClose={() => setConnectionDialogOpen(false)}
                    onCreated={(connectionId) => {
                        onSelect(connectionId);

                        setConnectionDialogOpen(false);
                    }}
                />
            )}
        </div>
    );
};

interface ConnectAccountsStepProps {
    dispatch: Dispatch<ActivationActionType>;
    state: ActivationStateI;
    template: AutomationWorkflowProjectWorkflowTemplate;
}

/**
 * Step 1: one row per template component that needs an account. A row highlighted by a
 * missing-connection failure blocks the wizard until that component is selected again — the
 * reducer's `canProceed` owns that rule, this component only renders it.
 */
const ConnectAccountsStep = ({dispatch, state, template}: ConnectAccountsStepProps) => {
    const componentsByName = useMemo(() => {
        const byName = new Map<string, AutomationWorkflowProjectComponent>();

        for (const component of template.components || []) {
            if (component.name) {
                byName.set(component.name, component);
            }
        }

        return byName;
    }, [template.components]);

    return (
        <fieldset className="flex flex-col gap-3 border-0">
            <legend className="pb-2 text-sm text-muted-foreground">Connect the accounts this automation needs.</legend>

            {state.requiredComponents.map((componentName) => (
                <ConnectAccountRow
                    component={componentsByName.get(componentName) || {name: componentName}}
                    highlighted={state.highlightedComponent === componentName}
                    key={componentName}
                    onSelect={(connectionId) => dispatch({componentName, connectionId, type: 'SELECT_CONNECTION'})}
                    selectedConnectionId={state.selections[componentName]}
                />
            ))}
        </fieldset>
    );
};

export default ConnectAccountsStep;
