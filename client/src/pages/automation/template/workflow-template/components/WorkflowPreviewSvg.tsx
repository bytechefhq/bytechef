import {HTMLAttributes} from 'react';

const WorkflowPreviewSvg = ({className, ...props}: HTMLAttributes<SVGSVGElement>) => (
    <svg
        aria-labelledby="workflowPreviewTitle"
        className={className}
        role="img"
        viewBox="0 0 720 640"
        xmlns="http://www.w3.org/2000/svg"
        {...props}
    >
        <title id="workflowPreviewTitle">Workflow preview</title>

        <defs>
            <pattern height="20" id="dotGrid" patternUnits="userSpaceOnUse" width="20">
                <circle cx="2" cy="2" fill="#E5E7EB" r="1.2" />
            </pattern>

            {/* Soft drop shadow for nodes */}

            <filter height="140%" id="shadow" width="140%" x="-20%" y="-20%">
                <feDropShadow dx="0" dy="2" floodColor="#000" floodOpacity="0.08" stdDeviation="3" />
            </filter>
        </defs>

        {/* Background grid fill */}

        <rect fill="url(#dotGrid)" height="100%" opacity="0.5" width="100%" x="0" y="0" />

        {/* Vertical spine */}

        <line stroke="#CBD5E1" strokeWidth="2" x1="360" x2="360" y1="60" y2="540" />

        {/* Helper: plus icon */}

        <defs>
            <g id="plusBox">
                <rect fill="#FFFFFF" height="24" rx="6" ry="6" stroke="#E2E8F0" width="24" x="-12" y="-12" />

                <line stroke="#64748B" strokeWidth="2" x1="-6" x2="6" y1="0" y2="0" />

                <line stroke="#64748B" strokeWidth="2" x1="0" x2="0" y1="-6" y2="6" />
            </g>
        </defs>

        {/* Node: Airtable newRecord */}

        <g filter="url(#shadow)" transform="translate(270,28)">
            <rect fill="#FFFFFF" height="72" rx="12" ry="12" stroke="#E2E8F0" width="180" x="0" y="0" />

            <rect fill="#F1F5F9" height="36" rx="8" stroke="#E2E8F0" width="36" x="16" y="18" />

            <text fill="#111827" fontFamily="Inter, system-ui, Arial" fontSize="16" fontWeight="700" x="64" y="30">
                GMail
            </text>

            <text fill="#334155" fontFamily="Inter, system-ui, Arial" fontSize="13" x="64" y="50">
                newEmail
            </text>

            <text fill="#94A3B8" fontFamily="Inter, system-ui, Arial" fontSize="11" x="16" y="64">
                trigger_1
            </text>
        </g>

        {/* Plus between nodes */}

        <use href="#plusBox" x="360" y="120" />

        {/* Node: Accelo createContact */}

        <g filter="url(#shadow)" transform="translate(270,156)">
            <rect fill="#FFFFFF" height="72" rx="12" ry="12" stroke="#E2E8F0" width="180" x="0" y="0" />

            <rect fill="#F1F5F9" height="36" rx="8" stroke="#E2E8F0" width="36" x="16" y="18" />

            <text fill="#111827" fontFamily="Inter, system-ui, Arial" fontSize="16" fontWeight="700" x="64" y="30">
                Salesforce
            </text>

            <text fill="#334155" fontFamily="Inter, system-ui, Arial" fontSize="13" x="64" y="50">
                createContact
            </text>

            <text fill="#94A3B8" fontFamily="Inter, system-ui, Arial" fontSize="11" x="16" y="64">
                accelo_2
            </text>
        </g>

        {/* Plus between nodes */}

        <use href="#plusBox" x="360" y="248" />

        {/* Node: Condition */}

        <g filter="url(#shadow)" transform="translate(270,268)">
            <rect fill="#FFFFFF" height="72" rx="12" ry="12" stroke="#E2E8F0" width="180" x="0" y="0" />

            <rect fill="#F1F5F9" height="36" rx="8" stroke="#E2E8F0" width="36" x="16" y="18" />

            {/* simple condition icon */}

            <g transform="translate(34,36)">
                <circle cx="-12" cy="8" fill="#111827" r="3" />

                <circle cx="12" cy="8" fill="#111827" r="3" />

                <circle cx="0" cy="-10" fill="#111827" r="3" />

                <path d="M -12 8 L 0 -10 L 12 8" fill="none" stroke="#111827" strokeWidth="1.6" />
            </g>

            <text fill="#111827" fontFamily="Inter, system-ui, Arial" fontSize="16" fontWeight="700" x="64" y="30">
                Condition
            </text>

            <text fill="#334155" fontFamily="Inter, system-ui, Arial" fontSize="13" x="64" y="50">
                condition_1
            </text>
        </g>

        {/* Connectors on spine */}

        <g fill="none" stroke="#94A3B8" strokeWidth="2">
            <line x1="360" x2="360" y1="100" y2="156" />

            <line x1="360" x2="360" y1="228" y2="268" />

            <line x1="360" x2="360" y1="340" y2="380" />
        </g>

        {/* Branch rectangular rails */}

        {/* Top rail */}

        <line stroke="#94A3B8" strokeWidth="2" x1="190" x2="550" y1="380" y2="380" />

        {/* Left vertical rail */}

        <line stroke="#94A3B8" strokeWidth="2" x1="190" x2="190" y1="380" y2="560" />

        {/* Bottom rail */}

        <line stroke="#94A3B8" strokeWidth="2" x1="190" x2="550" y1="560" y2="560" />

        {/* Right vertical rail */}

        <line stroke="#94A3B8" strokeWidth="2" x1="550" x2="550" y1="380" y2="560" />

        {/* TRUE/FALSE labels */}

        <g fill="#64748B" fontFamily="Inter, system-ui, Arial" fontSize="18" fontWeight="700">
            <text x="220" y="370">
                TRUE
            </text>

            <text x="460" y="370">
                FALSE
            </text>
        </g>

        {/* Center drop from condition to top rail */}

        <line stroke="#94A3B8" strokeWidth="2" x1="360" x2="360" y1="340" y2="380" />

        {/* Plus icons on branch rails (top) */}

        <use href="#plusBox" x="190" y="430" />

        <use href="#plusBox" x="550" y="430" />

        {/* Left branch node: Slack sendApprovalMessage */}

        <g filter="url(#shadow)" transform="translate(60,448)">
            <rect fill="#FFFFFF" height="72" rx="12" ry="12" stroke="#E2E8F0" width="220" x="0" y="0" />

            <rect fill="#F1F5F9" height="36" rx="8" stroke="#E2E8F0" width="36" x="16" y="18" />

            <text fill="#111827" fontFamily="Inter, system-ui, Arial" fontSize="16" fontWeight="700" x="64" y="30">
                Slack
            </text>

            <text fill="#334155" fontFamily="Inter, system-ui, Arial" fontSize="13" x="64" y="50">
                sendApprovalMessage
            </text>

            <text fill="#94A3B8" fontFamily="Inter, system-ui, Arial" fontSize="11" x="16" y="64">
                slack_1
            </text>
        </g>

        {/* Right branch node: Gmail sendEmail */}

        <g filter="url(#shadow)" transform="translate(440,448)">
            <rect fill="#FFFFFF" height="72" rx="12" ry="12" stroke="#E2E8F0" width="220" x="0" y="0" />

            <rect fill="#F1F5F9" height="36" rx="8" stroke="#E2E8F0" width="36" x="16" y="18" />

            <text fill="#111827" fontFamily="Inter, system-ui, Arial" fontSize="16" fontWeight="700" x="64" y="30">
                Gmail
            </text>

            <text fill="#334155" fontFamily="Inter, system-ui, Arial" fontSize="13" x="64" y="50">
                sendEmail
            </text>

            <text fill="#94A3B8" fontFamily="Inter, system-ui, Arial" fontSize="11" x="16" y="64">
                googleMail_1
            </text>
        </g>

        {/* Plus icons below branch nodes */}

        <use href="#plusBox" x="190" y="508" />

        <use href="#plusBox" x="550" y="508" />

        {/* Dotted continuation from bottom rail to center plus */}

        <g stroke="#94A3B8" strokeWidth="2">
            <line strokeDasharray="4 6" x1="360" x2="360" y1="560" y2="584" />
        </g>

        <use href="#plusBox" x="360" y="596" />
    </svg>
);

export default WorkflowPreviewSvg;
