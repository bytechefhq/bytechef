"use client";
import * as React from "react";
import { useContext } from "react";

export interface TabsItemProps {
  id: string;
  children: any;
}

import Context from "./tabs-list.context";

function TabsItem(props: TabsItemProps) {
  const context = useContext(Context);

  return (
    <>
      {context.activeTab === props.id ? (
        <>
          <div
            role="tabpanel"
            className="div-4bdb9df6"
            aria-labelledby={`tab-${props.id}`}
            id={`panel-${props.id}`}
          >
            {props.children}
          </div>
        </>
      ) : null}

      <style>{`.div-4bdb9df6 {
  margin-top: 0.7rem;
  margin-bottom: 0.7rem;
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  border-radius: 0.5rem;
}`}</style>
    </>
  );
}

export default TabsItem;
