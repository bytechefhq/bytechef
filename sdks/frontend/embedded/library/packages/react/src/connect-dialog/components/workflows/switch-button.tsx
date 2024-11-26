import * as React from "react";

interface SwitchButtonProps {}

function SwitchButton(props: SwitchButtonProps) {
  return (
    <>
      <label className="switch">
        <input type="checkbox" />
        <span className="slider round" />
      </label>

      <style>{`
        /* The switch - the box around the slider */
        .switch {
          position: relative;
          display: inline-block;
          width: 44px;
          height: 24px;
        }

        /* Hide default HTML checkbox */
        .switch input {
          opacity: 0;
          width: 0;
          height: 0;
        }

        /* The slider */
        .slider {
          position: absolute;
          cursor: pointer;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background-color: #ccc;
          -webkit-transition: .4s;
          transition: .4s;
        }

        .slider:before {
          position: absolute;
          content: "";
          height: 16px;
          width: 16px;
          left: 4px;
          bottom: 4px;
          background-color: white;
          -webkit-transition: .4s;
          transition: .4s;
        }

        input:checked + .slider {
          background-color: #000;
        }

        input:focus + .slider {
          box-shadow: 0 0 1px #000;
        }

        input:checked + .slider:before {
          -webkit-transform: translateX(18px);
          -ms-transform: translateX(18px);
          transform: translateX(18px);
        }

        /* Rounded sliders */
        .slider.round {
          border-radius: 34px;
        }

        .slider.round:before {
          border-radius: 50%;
        }

`}</style>
    </>
  );
}

export default SwitchButton;
