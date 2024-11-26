"use client";
import * as React from "react";
import { useState, useContext } from "react";

export interface TabsListProps {
  activeTab: string;
  tabs: {
    id: string;
    label: string;
  }[];
  children: any;
}

import Context from "./tabs-list.context";
import TabsListButton from "./tabs-list-button";

function TabsList(props: TabsListProps) {
  const [activeTab, setActiveTab] = useState(() => props.activeTab);

  return (
    <>
      <Context.Provider
        value={{
          activeTab: activeTab,
        }}
      >
        <div className="div-6a8636a4">
          <nav className="nav-6a8636a4">
            <ul
              role="tablist"
              aria-orientation="horizontal"
              className="ul-6a8636a4"
            >
              {props.tabs?.map((tab, index) => (
                <li className="li-6a8636a4" key={`tab-${index}`}>
                  <TabsListButton
                    activeTab={activeTab}
                    id={tab.id}
                    label={tab.label}
                    onClick={(tabId) => setActiveTab(tabId)}
                  />
                </li>
              ))}
            </ul>
          </nav>
          {props.children}
        </div>
      </Context.Provider>

      <style>{`.div-6a8636a4 {
  width: 100%;
}.nav-6a8636a4 {
  overflow-x: scroll;
  scrollbar-width: none;
  overflow-scrolling: touch;
  -webkit-overflow-scrolling: touch;
}.ul-6a8636a4 {
  width: fit-content;
  min-width: 100%;
  color: #57606f;
  display: flex;
  gap: 0.5em;
  border-bottom: 2px solid #ddd;
  margin: 0;
  padding: 0;
}.li-6a8636a4 {
  display: block;
  margin-bottom: -2px;
}`}</style>
    </>
  );
}

export default TabsList;
