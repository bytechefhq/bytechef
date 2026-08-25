import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';
import {useCallback, useMemo} from 'react';

export const SKILLS_TOOL_OPERATION_NAME = 'skillsTool';

const SKILLS_TOOL_TYPE = 'aiAgentUtils/v1/skillsTool';
const SKILLS_TOOL_NODE_NAME = 'skillsTool_1';

// AiAgentUtilsSkillsTool declares array("skills").items(integer("skillId")), so the stored value is a flat
// array of skill ids — both AiAgentUtilsSkillsTool.perform and SkillComponentConnectionFactory read it with
// getList(SKILLS, Long.class). Writing [{skillId}] objects here makes the connection factory throw
// "Cannot deserialize value of type java.lang.Long from Object value" on the next workflow save.
type SkillsToolElementType = ClusterElementItemType & {
    operationName?: string;
    parameters?: {skills?: Array<number | {skillId?: number}>};
};

// Reads tolerate the {skillId} object form because AiAgentWorkflowGenerator emits it, but writes always use
// the flat form the component and the connection factory actually accept.
const toSkillId = (entry: number | {skillId?: number}) => (typeof entry === 'number' ? entry : entry?.skillId);

interface UseAiAgentSkillsI {
    canEdit: boolean;
    skillIds: number[];
    updateSkillIds: (skillIds: number[]) => void;
}

const isSkillsTool = (element: SkillsToolElementType) =>
    (element.operationName || element.type?.split('/')[2]) === SKILLS_TOOL_OPERATION_NAME;

export default function useAiAgentSkills(): UseAiAgentSkillsI {
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const setRootClusterElementNodeData = useWorkflowEditorStore((state) => state.setRootClusterElementNodeData);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {updateWorkflowMutation} = useWorkflowEditor();

    // The cluster elements are read from the workflow definition, never from rootClusterElementNodeData, which is
    // seeded once per root and so still shows whatever was attached when the AI Agent editor was opened.
    // saveWorkflowDefinition replaces a cluster root's clusterElements wholesale, so basing the write on that
    // stale copy would drop the model, the memory and every tool added since.
    const rootClusterElements = useMemo<ClusterElementsType | undefined>(() => {
        if (!workflow.definition || !rootClusterElementNodeData?.workflowNodeName) {
            return undefined;
        }

        try {
            const definition = JSON.parse(workflow.definition);

            const rootTask = getTask({
                tasks: definition.tasks ?? [],
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            });

            return rootTask?.clusterElements as ClusterElementsType | undefined;
        } catch {
            return undefined;
        }
    }, [rootClusterElementNodeData?.workflowNodeName, workflow.definition]);

    const toolElements = useMemo<SkillsToolElementType[]>(() => {
        if (!rootClusterElements || Array.isArray(rootClusterElements)) {
            return [];
        }

        const tools = rootClusterElements['tools'];

        return Array.isArray(tools) ? (tools as SkillsToolElementType[]) : [];
    }, [rootClusterElements]);

    const skillIds = useMemo(() => {
        const skillsToolElement = toolElements.find(isSkillsTool);

        return (skillsToolElement?.parameters?.skills ?? [])
            .map(toSkillId)
            .filter((skillId): skillId is number => skillId != null);
    }, [toolElements]);

    const updateSkillIds = useCallback(
        (updatedSkillIds: number[]) => {
            if (!rootClusterElementNodeData?.workflowNodeName || !rootClusterElementNodeData?.componentName) {
                return;
            }

            const existingElement = toolElements.find(isSkillsTool);
            const otherToolElements = toolElements.filter((element) => !isSkillsTool(element));

            const baseElement: SkillsToolElementType = existingElement ?? {
                label: 'Skills',
                metadata: {ui: {nodePosition: {x: 0, y: 0}}},
                name: SKILLS_TOOL_NODE_NAME,
                operationName: SKILLS_TOOL_OPERATION_NAME,
                type: SKILLS_TOOL_TYPE,
            };

            const skillsToolElement: SkillsToolElementType = {
                ...baseElement,
                parameters: {
                    ...(baseElement.parameters ?? {}),
                    skills: updatedSkillIds,
                },
            };

            const updatedToolElements: SkillsToolElementType[] = updatedSkillIds.length
                ? [...otherToolElements, skillsToolElement]
                : otherToolElements;

            const updatedClusterElements: ClusterElementsType = {
                ...((rootClusterElements && !Array.isArray(rootClusterElements)
                    ? rootClusterElements
                    : {}) as ClusterElementsType),
                tools: updatedToolElements,
            };

            setRootClusterElementNodeData({
                ...rootClusterElementNodeData,
                clusterElements: updatedClusterElements,
            });

            saveWorkflowDefinition({
                nodeData: {
                    ...rootClusterElementNodeData,
                    clusterElements: updatedClusterElements,
                },
                updateWorkflowMutation: updateWorkflowMutation!,
            });
        },
        [
            rootClusterElementNodeData,
            rootClusterElements,
            setRootClusterElementNodeData,
            toolElements,
            updateWorkflowMutation,
        ]
    );

    return {
        canEdit: !!rootClusterElementNodeData?.workflowNodeName && !!workflow.id,
        skillIds,
        updateSkillIds,
    };
}
