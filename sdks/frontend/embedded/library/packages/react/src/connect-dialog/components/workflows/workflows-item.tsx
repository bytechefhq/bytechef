import * as React from "react";

interface WorkflowsItemProps {
  workflow?: Workflow;
}

import { Workflow } from "../../../middleware/model/Workflow";
import SwitchButton from "./switch-button";

function WorkflowsItem(props: WorkflowsItemProps) {
  return (
    <>
      <div className="div-68a0c0b4">
        <div className="div-68a0c0b4-2">
          <div className="div-68a0c0b4-3">{props.workflow?.label}</div>
          <div className="div-68a0c0b4-4">{props.workflow?.description}</div>
        </div>
        <div className="div-68a0c0b4-5">
          <SwitchButton />
        </div>
      </div>

      <style>{`.div-68a0c0b4 {
  display: flex;
  padding-top: 0.8rem;
  padding-bottom: 0.8rem;
  align-items: center;
}.div-68a0c0b4:first-child {
  padding-top: 0;
}.div-68a0c0b4:last-child {
  padding-top: 0;
}.div-68a0c0b4-2 {
  display: flex;
  flex-direction: column;
  justify-content: between;
  flex-grow: 1;
}.div-68a0c0b4-3 {
  font-size: 0.9rem;
  padding-bottom: 0.3rem;
}.div-68a0c0b4-4 {
  color: #737C86;
  font-size: 0.9rem;
  line-height: 1.3;
}.div-68a0c0b4-5 {
  display: flex;
  justify-content: end;
  margin-left: 1rem;
}`}</style>
    </>
  );
}

export default WorkflowsItem;
