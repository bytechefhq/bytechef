'use client';

import {useRef} from 'react';

import useEmbedInit from '../../shared/useEmbedInit';

export interface AutomationHubTabsConfig {
    automations?: boolean;

    connections?: boolean;

    newWorkflow?: boolean;
}

export interface AutomationHubTheme {
    activeBorderColor?: string;

    borderRadius?: string;

    cardColor?: string;

    cssVariables?: Record<string, string>;

    disableColor?: string;

    enableColor?: string;

    fontFamily?: string;

    mode?: 'dark' | 'light';

    onAccentColor?: string;

    primaryColor?: string;

    segmentColor?: string;

    surfaceColor?: string;
}

interface AutomationHubProps {
    baseUrl?: string;

    className?: string;

    defaultLayout?: 'grid' | 'list';

    editWorkflowAllowed?: boolean;

    connectionDialogAllowed?: boolean;

    environment?: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';

    includeComponents?: string[];

    jwtToken: string;

    layoutSwitcherAllowed?: boolean;

    tabs?: AutomationHubTabsConfig;

    theme?: AutomationHubTheme;
}

const AutomationHub = ({
    baseUrl = 'https://app.bytechef.io',
    className,
    connectionDialogAllowed = true,
    defaultLayout = 'grid',
    editWorkflowAllowed = true,
    environment = 'PRODUCTION',
    includeComponents,
    jwtToken,
    layoutSwitcherAllowed = true,
    tabs,
    theme,
}: AutomationHubProps) => {
    const iframeRef = useRef<HTMLIFrameElement>(null);

    useEmbedInit({
        baseUrl,
        iframeRef,
        params: {
            connectionDialogAllowed,
            defaultLayout,
            editWorkflowAllowed,
            environment,
            includeComponents,
            jwtToken,
            layoutSwitcherAllowed,
            tabs,
            theme,
        },
    });

    return (
        <div className={className}>
            <iframe
                ref={iframeRef}
                src={`${baseUrl}/automation-hub.html#/embedded/hub`}
                width="100%"
                height="100%"
                style={{border: 'none'}}
                title="Automation Hub"
            />
        </div>
    );
};

export default AutomationHub;
