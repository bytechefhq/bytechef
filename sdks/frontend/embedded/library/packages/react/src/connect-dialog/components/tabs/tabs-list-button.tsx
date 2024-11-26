"use client";
import * as React from "react";

export interface TabsListButtonProps {
  activeTab: string;
  id: string;
  label: string;
  onClick: (tabId: string) => void;
}

function TabsListButton(props: TabsListButtonProps) {
  return (
    <>
      <button
        role="tab"
        className="button-07b505a2"
        key={`tab-btn-${props.id}`}
        id={`tab-${props.id}`}
        aria-controls={`panel-${props.id}`}
        aria-selected={props.activeTab === props.id}
        onClick={(event) => props.onClick(props.id)}
        style={{
          color: props.activeTab === props.id ? "#2563EB" : "inherit",
          borderColor: props.activeTab === props.id ? "#2563EB" : "transparent",
        }}
      >
        {props.label}
      </button>

      <style>{`.button-07b505a2 {
  font-size: 0.9rem;
  padding: 0.75em 1em;
  background-color: transparent;
  border: 2px solid #ddd;
  border-width: 0 0 2px;
  cursor: pointer;
  white-space: nowrap;
}`}</style>
    </>
  );
}

export default TabsListButton;
