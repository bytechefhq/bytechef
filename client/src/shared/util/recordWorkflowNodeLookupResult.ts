import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import {parseWorkflowNodeLookupUrl} from '@/shared/util/workflowNodeLookupUrl';

interface LookupResponseI {
    clone: () => {json: () => Promise<{detail?: string; title?: string}>};
    status: number;
    url: string;
}

export default function recordWorkflowNodeLookupResult(response: LookupResponseI): boolean {
    const workflowNodeLookup = parseWorkflowNodeLookupUrl(response.url);

    if (!workflowNodeLookup) {
        return false;
    }

    const {clearLookupFailure, recordLookupFailure} = useWorkflowIssuesStore.getState();
    const {nodeName, propertyName} = workflowNodeLookup;

    if (response.status >= 200 && response.status <= 299) {
        clearLookupFailure(nodeName, propertyName);

        return true;
    }

    const fallbackMessage = `Request failed with status ${response.status}`;
    const clonedResponse = response.clone();

    clonedResponse
        .json()
        .then((data) => recordLookupFailure(nodeName, propertyName, data.detail || data.title || fallbackMessage))
        .catch(() => recordLookupFailure(nodeName, propertyName, fallbackMessage));

    return true;
}
