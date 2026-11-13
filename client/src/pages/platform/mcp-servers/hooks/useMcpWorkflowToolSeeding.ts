import useToolSeeding from '@/pages/platform/mcp-servers/hooks/useToolSeeding';
import {
    collectPropertyDefaults,
    computeAiFillValues,
    loadNoPropertyOptions,
} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import {PropertyAllType} from '@/shared/types';

export type LoadWorkflowToolPropertiesType = (workflowId: string) => Promise<PropertyAllType[]>;

interface UseMcpWorkflowToolSeedingParamsI {
    loadWorkflowProperties: LoadWorkflowToolPropertiesType;
    newWorkflowIds: string[];
    seedKey: string;
}

export default function useMcpWorkflowToolSeeding({
    loadWorkflowProperties,
    newWorkflowIds,
    seedKey,
}: UseMcpWorkflowToolSeedingParamsI) {
    const seedWorkflowTool = async (workflowId: string): Promise<Record<string, string>> => {
        const properties = await loadWorkflowProperties(workflowId);

        return computeAiFillValues({
            includeAdvanced: false,
            includeConditional: true,
            loadOptions: loadNoPropertyOptions,
            properties,
            values: collectPropertyDefaults(properties),
        });
    };

    const {getFailedSeedKeys, seededValuesByKey, waitForSeeding} = useToolSeeding({
        keys: newWorkflowIds,
        seed: seedWorkflowTool,
        seedKey,
    });

    return {
        aiValuesByWorkflowId: seededValuesByKey,
        getFailedSeedKeys,
        waitForSeeding,
    };
}
