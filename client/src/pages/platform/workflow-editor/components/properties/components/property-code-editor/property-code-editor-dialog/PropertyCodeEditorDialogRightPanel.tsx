import PropertyCodeEditorDialogRightPanelConnections from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnections';
import PropertyCodeEditorDialogRightPanelInput from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelInput';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    useClusterElementComponentConnectionsQuery,
    useClusterElementScriptInputQuery,
    useWorkflowNodeComponentConnectionsQuery,
    useWorkflowNodeScriptInputQuery,
} from '@/shared/middleware/graphql';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useShallow} from 'zustand/react/shallow';

interface PropertyCodeEditorDialogConnectionsSheetRightPanelProps {
    workflow: Workflow;
    workflowNodeName: string;
}

const PropertyCodeEditorDialogRightPanel = ({
    workflow,
    workflowNodeName,
}: PropertyCodeEditorDialogConnectionsSheetRightPanelProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const rootClusterElementNodeData = useWorkflowEditorStore(useShallow((state) => state.rootClusterElementNodeData));

    const isClusterElement = currentNode?.clusterElementType && rootClusterElementNodeData?.workflowNodeName;

    const {data: clusterElementScriptInputData} = useClusterElementScriptInputQuery(
        {
            clusterElementType: currentNode?.clusterElementType ?? '',
            clusterElementWorkflowNodeName: currentNode?.name ?? '',
            environmentId: currentEnvironmentId!,
            workflowId: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName ?? '',
        },
        {
            enabled: !!isClusterElement && currentEnvironmentId != null && !!workflow.id,
        }
    );

    const {data: workflowNodeScriptInputData} = useWorkflowNodeScriptInputQuery(
        {
            environmentId: currentEnvironmentId!,
            workflowId: workflow.id!,
            workflowNodeName,
        },
        {
            enabled: !isClusterElement && currentEnvironmentId != null && !!workflow.id,
        }
    );

    const clusterElementConnectionsQueryEnabled = !!isClusterElement && !!workflow.id;
    const workflowNodeConnectionsQueryEnabled = !isClusterElement && !!workflow.id;

    const {data: clusterElementComponentConnectionsData} = useClusterElementComponentConnectionsQuery(
        {
            clusterElementType: currentNode?.clusterElementType ?? '',
            clusterElementWorkflowNodeName: currentNode?.name ?? '',
            workflowId: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName ?? '',
        },
        {
            enabled: clusterElementConnectionsQueryEnabled,
        }
    );

    const {data: workflowNodeComponentConnectionsData} = useWorkflowNodeComponentConnectionsQuery(
        {
            workflowId: workflow.id!,
            workflowNodeName,
        },
        {
            enabled: workflowNodeConnectionsQueryEnabled,
        }
    );

    const input = isClusterElement
        ? (clusterElementScriptInputData?.clusterElementScriptInput ?? {})
        : (workflowNodeScriptInputData?.workflowNodeScriptInput ?? {});

    const componentConnections = isClusterElement
        ? (clusterElementComponentConnectionsData?.clusterElementComponentConnections ?? [])
        : (workflowNodeComponentConnectionsData?.workflowNodeComponentConnections ?? []);

    return (
        <div className="flex w-96 flex-col divide-y divide-solid divide-muted border-l border-l-border/50">
            <div className="flex-1">
                <PropertyCodeEditorDialogRightPanelInput input={input} />
            </div>

            <div className="flex-1">
                <PropertyCodeEditorDialogRightPanelConnections
                    componentConnections={componentConnections}
                    workflow={workflow}
                    workflowNodeName={workflowNodeName}
                />
            </div>
        </div>
    );
};

export default PropertyCodeEditorDialogRightPanel;
