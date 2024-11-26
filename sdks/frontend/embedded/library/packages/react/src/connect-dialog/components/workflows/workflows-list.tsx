import * as React from "react";

interface WorkflowsListProps {
  workflows?: Workflow[];
}

import { Workflow } from "../../../middleware/model/Workflow";
import WorkflowsItem from "./workflows-item";

function WorkflowsList(props: WorkflowsListProps) {
  return (
    <>
      <div className="div-f9f2c908">
        {props.workflows?.map((workflow) => (
          <WorkflowsItem
            key={workflow.workflowReferenceCode}
            workflow={workflow}
          />
        ))}
      </div>

      <style>{`.div-f9f2c908 {
  overflow-y: auto;
  max-height: 600px;
}`}</style>
    </>
  );
}

export default WorkflowsList;
