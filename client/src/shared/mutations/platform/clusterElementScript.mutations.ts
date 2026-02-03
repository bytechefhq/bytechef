import {ScriptTestExecution} from '@/shared/middleware/platform/configuration';

export type TestClusterElementScriptRequestType = {
    clusterElementType: string;
    clusterElementWorkflowNodeName: string;
    environmentId: number;
    workflowId: string;
    workflowNodeName: string;
};

const TEST_CLUSTER_ELEMENT_SCRIPT_MUTATION = `
    mutation TestClusterElementScript(
        $workflowId: String!
        $workflowNodeName: String!
        $clusterElementType: String!
        $clusterElementWorkflowNodeName: String!
        $environmentId: Long!
    ) {
        testClusterElementScript(
            workflowId: $workflowId
            workflowNodeName: $workflowNodeName
            clusterElementType: $clusterElementType
            clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
            environmentId: $environmentId
        ) {
            error {
                message
                stackTrace
            }
            output
        }
    }
`;

export async function testClusterElementScript(
    request: TestClusterElementScriptRequestType
): Promise<ScriptTestExecution> {
    const response = await fetch('/graphql', {
        body: JSON.stringify({
            query: TEST_CLUSTER_ELEMENT_SCRIPT_MUTATION,
            variables: request,
        }),
        headers: {
            'Content-Type': 'application/json',
        },
        method: 'POST',
    });

    const json = await response.json();

    if (json.errors) {
        const {message} = json.errors[0];

        throw new Error(message);
    }

    return json.data.testClusterElementScript;
}
