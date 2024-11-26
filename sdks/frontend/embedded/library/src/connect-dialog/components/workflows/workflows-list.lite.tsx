import { For } from '@builder.io/mitosis';
import {Workflow} from "../../../middleware/model/Workflow";
import WorkflowsItem from "./workflows-item.lite";

interface WorkflowsListProps {
    workflows?: Workflow[];
}

export default function WorkflowsList(props: WorkflowsListProps) {
    return  <div css={{
        overflowY: 'auto',
        maxHeight: '600px',
    }}>
        <For each={props.workflows}>
            {workflow => <WorkflowsItem key={workflow.workflowReferenceCode} workflow={workflow} />}
        </For>
    </div>;
}
