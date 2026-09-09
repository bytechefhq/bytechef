const NODE_PATH = String.raw`/workflows/[^/]+/workflow-nodes/([^/]+)(?:/cluster-elements/[^/]+/([^/]+))?`;

const PROPERTY_LOOKUP_URL_PATTERN = new RegExp(`${NODE_PATH}/(?:options|dynamic-properties)/([^/?]+)`);
const NODE_LOOKUP_URL_PATTERN = new RegExp(`${NODE_PATH}/(?:outputs|display-conditions)(?:[/?]|$)`);
const WORKFLOW_OUTPUTS_URL_PATTERN = /\/workflows\/[^/]+\/outputs(?:[/?]|$)/;
const LAST_WORKFLOW_NODE_NAME_PATTERN = /[?&]lastWorkflowNodeName=([^&]+)/;

export interface WorkflowNodeLookupI {
    nodeName: string;
    propertyName?: string;
}

export function parseWorkflowNodeLookupUrl(url: string): WorkflowNodeLookupI | undefined {
    const propertyMatch = PROPERTY_LOOKUP_URL_PATTERN.exec(url);

    if (propertyMatch) {
        return {nodeName: propertyMatch[2] ?? propertyMatch[1], propertyName: propertyMatch[3]};
    }

    const nodeMatch = NODE_LOOKUP_URL_PATTERN.exec(url);

    if (nodeMatch) {
        return {nodeName: nodeMatch[2] ?? nodeMatch[1]};
    }

    if (WORKFLOW_OUTPUTS_URL_PATTERN.test(url)) {
        const lastNodeNameMatch = LAST_WORKFLOW_NODE_NAME_PATTERN.exec(url);

        if (lastNodeNameMatch) {
            return {nodeName: decodeURIComponent(lastNodeNameMatch[1])};
        }
    }

    return undefined;
}
