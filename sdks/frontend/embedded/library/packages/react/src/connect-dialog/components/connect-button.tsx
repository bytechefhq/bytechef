"use client";
import * as React from "react";

interface CloseButtonProps {
  onClose?: (e: any) => void;
}

function ConnectButton(props: CloseButtonProps) {
  return (
    <>
      <button
        className="button-26b64d18"
        onClick={(e) => props.onClose && props.onClose(e)}
      >
        Connect
      </button>

      <style>{`.button-26b64d18 {
  background-color: #000;
  border-radius: 10px;
  cursor: pointer;
  font-size: 1rem;
  padding: 0.7rem;
  right: 0.4rem;
  top: 0.4rem;
  outline: none;
  border: none;
  font-weight: 500;
  color: #fff;
  width: 100%;
}`}</style>
    </>
  );
}

export default ConnectButton;
