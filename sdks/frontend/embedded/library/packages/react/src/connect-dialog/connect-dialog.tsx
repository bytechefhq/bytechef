"use client";
import * as React from "react";
import { useState, useEffect } from "react";

interface ConnectDialogProps {
  onClose?: () => void;
}

import CloseButton from "./components/close-button";
import ConnectButton from "./components/connect-button";
import TabsList from "./components/tabs/tabs-list";
import TabsItem from "./components/tabs/tabs-item";
import fetchIntegration from "../middleware/fetch-integration";
import { Integration } from "../middleware/model/Integration";
import WorkflowsList from "./components/workflows/workflows-list";
import PoweredBy from "./components/powered-by";

function ConnectDialog(props: ConnectDialogProps) {
  const [integration, setIntegration] = useState<Integration | undefined>(
    () => undefined
  );

  useEffect(() => {
    fetchIntegration().then((response) => {
      console.log(response);
      setIntegration(response);
    });
  }, []);

  return (
    <>
      <div className="div-bd60b4a4">
        <div className="div-bd60b4a4-2">
          <CloseButton onClose={(event) => props.onClose && props.onClose()} />
          <div className="div-bd60b4a4-3">
            <div className="div-bd60b4a4-4">
              <img
                src={`data:image/svg+xml;utf8,${integration?.icon}`}
                alt={integration?.title}
              />
            </div>
            <div className="div-bd60b4a4-5">{integration?.title}</div>
          </div>
          <div className="div-bd60b4a4-6">
            {integration ? (
              <>
                {integration?.workflows.length ? (
                  <TabsList
                    activeTab="tab1"
                    tabs={[
                      {
                        id: "tab1",
                        label: "Overview",
                      },
                      {
                        id: "tab2",
                        label: "Configuration",
                      },
                    ]}
                  >
                    <TabsItem id="tab1">
                      <div className="div-bd60b4a4-10">
                        {integration?.description}
                      </div>
                    </TabsItem>
                    <TabsItem id="tab2">
                      <WorkflowsList workflows={integration?.workflows} />
                    </TabsItem>
                  </TabsList>
                ) : (
                  <>
                    <div className="div-bd60b4a4-7">
                      <div className="div-bd60b4a4-8">Overview</div>
                      <div className="div-bd60b4a4-9">
                        {integration?.description}
                      </div>
                    </div>
                  </>
                )}
              </>
            ) : (
              <>Loading...</>
            )}
          </div>
          <ConnectButton />
          <PoweredBy />
        </div>
      </div>

      <style>{`.div-bd60b4a4 {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.8);
}.div-bd60b4a4-2 {
  max-width: 480px;
  min-height: 300px;
  margin: 150px auto;
  background-color: #ffff;
  border-radius: 6px;
  padding: 1rem;
  position: relative;
  display: flex;
  flex-direction: column;
  font-family: Arial, sans-serif;
}.div-bd60b4a4-3 {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}.div-bd60b4a4-4 {
  width: 24px;
}.div-bd60b4a4-5 {
  font-size: 1.3rem;
  margin-left: 0.5rem;
}.div-bd60b4a4-6 {
  display: flex;
  flex-grow: 1;
}.div-bd60b4a4-7 {
  display: flex;
  flex-direction: column;
}.div-bd60b4a4-8 {
  font-size: 1rem;
  margin-bottom: 0.5rem;
}.div-bd60b4a4-9 {
  color: #737C86;
  font-size: 0.9rem;
  line-height: 1.3;
}.div-bd60b4a4-10 {
  color: #737C86;
  font-size: 0.9rem;
  line-height: 1.3;
}`}</style>
    </>
  );
}

export default ConnectDialog;
