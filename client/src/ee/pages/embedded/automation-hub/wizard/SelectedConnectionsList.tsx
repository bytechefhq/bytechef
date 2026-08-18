import {useGetComponentConnectionsQuery} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {ActivationStateI} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {AutomationWorkflowProjectWorkflowTemplate} from '@/ee/shared/middleware/embedded/public';
import {useMemo} from 'react';

interface SelectedConnectionItemProps {
    componentName: string;
    componentTitle: string;
    connectionId?: number;
}

const SelectedConnectionItem = ({componentName, componentTitle, connectionId}: SelectedConnectionItemProps) => {
    const {data: connections} = useGetComponentConnectionsQuery(componentName);

    const connectionName = useMemo(
        () => (connections || []).find((connection) => connection.id === connectionId)?.name,
        [connectionId, connections]
    );

    return (
        <li className="flex items-center gap-2 text-sm">
            <span className="font-medium">{componentTitle}</span>

            <span aria-hidden="true" className="text-muted-foreground">
                →
            </span>

            <span className="text-muted-foreground">{connectionName || 'No account selected'}</span>
        </li>
    );
};

interface SelectedConnectionsListProps {
    state: ActivationStateI;
    template: AutomationWorkflowProjectWorkflowTemplate;
}

/**
 * The "Connected accounts" summary shared by the configure and activate steps: one line per
 * required component naming the connection the user picked for it.
 */
const SelectedConnectionsList = ({state, template}: SelectedConnectionsListProps) => {
    const componentTitlesByName = useMemo(() => {
        const titlesByName = new Map<string, string>();

        for (const component of template.components || []) {
            if (component.name) {
                titlesByName.set(component.name, component.title || component.name);
            }
        }

        return titlesByName;
    }, [template.components]);

    return (
        <ul className="flex flex-col gap-1">
            {state.requiredComponents.map((componentName) => (
                <SelectedConnectionItem
                    componentName={componentName}
                    componentTitle={componentTitlesByName.get(componentName) || componentName}
                    connectionId={state.selections[componentName]}
                    key={componentName}
                />
            ))}
        </ul>
    );
};

export default SelectedConnectionsList;
