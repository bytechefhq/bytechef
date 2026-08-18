'use client';

import {useEffect, useRef} from 'react';

/**
 * Which tabs of the Automation Hub are shown to the end user. All keys default to `true`; set a
 * key to `false` to hide that tab. If only one tab remains enabled the tab strip itself is hidden.
 */
export interface AutomationHubTabsConfig {
    /**
     * The Automations tab -- the template catalog plus the user's own automations.
     * @default true
     */
    automations?: boolean;

    /**
     * The Connections tab -- the connections the user owns, with reconnect/delete. Connections
     * shared by the vendor through `sharedConnectionIds` are deliberately NOT listed here: the
     * user may select them when activating an automation, but may not reconnect or delete them.
     * @default true
     */
    connections?: boolean;

    /**
     * The "New automation" button on the Automations tab.
     * @default true
     */
    newWorkflow?: boolean;
}

/**
 * Theme applied to the Automation Hub iframe's content. `fontFamily` must be a font the iframe
 * can load on its own (system/web-safe, or otherwise loadable by the ByteChef application) --
 * host-page `@font-face` declarations do not cross the iframe boundary.
 */
export interface AutomationHubTheme {
    /**
     * CSS length applied to the hub's border radius token, e.g. '0.5rem'.
     */
    borderRadius?: string;

    /**
     * CSS font-family value applied to the hub's font token. Must be loadable inside the iframe --
     * host-page `@font-face` does not cross the iframe boundary.
     */
    fontFamily?: string;

    /**
     * Light or dark mode.
     * @default 'light'
     */
    mode?: 'dark' | 'light';

    /**
     * Any CSS color, applied to the hub's primary color token. The contrasting foreground color is
     * computed automatically.
     */
    primaryColor?: string;
}

/**
 * Props for the AutomationHub component.
 * This interface defines all the configuration options needed to initialize and render
 * the embedded Automation Hub iframe.
 */
interface AutomationHubProps {
    /**
     * The base URL of the ByteChef application.
     * This URL is used to construct the iframe src attribute.
     * @default 'https://app.bytechef.io'
     */
    baseUrl?: string;

    /**
     * Additional CSS classes applied to the wrapping element. The component renders no layout
     * classes of its own -- the host controls sizing and positioning entirely through this prop.
     */
    className?: string;

    /**
     * Whether to allow the connection dialog to be shown in the workflow builder view of the hub.
     * When true, users can create and manage connections directly. When false, users can only use
     * existing connections -- either shared connections defined by `sharedConnectionIds` or
     * integration connections created via `ConnectDialog`.
     * @default true
     */
    connectionDialogAllowed?: boolean;

    /**
     * The environment to use for the Automation Hub.
     * This affects which environment's connections and configurations are used.
     * @default 'PRODUCTION'
     */
    environment?: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';

    /**
     * Array of component identifiers to include in the hub's workflow builder view.
     * This limits which integration components are available to the user.
     * Example: ['slack', 'googleMail', 'productboard']
     */
    includeComponents?: string[];

    /**
     * JWT token for authentication with the ByteChef API.
     * This token is passed to the iframe via postMessage for API authorization.
     */
    jwtToken: string;

    /**
     * Array of connection IDs that should be shared with this Automation Hub.
     * These connections will be available for use in the automations the user activates.
     * Shared connections can be created via the ByteChef's '/embedded/connections' page.
     * @default []
     */
    sharedConnectionIds?: number[];

    /**
     * Which tabs of the Automation Hub are shown to the end user.
     * @default {automations: true, connections: true, newWorkflow: true}
     */
    tabs?: AutomationHubTabsConfig;

    /**
     * Theme applied to the Automation Hub iframe's content.
     */
    theme?: AutomationHubTheme;
}

/**
 * A component that embeds the ByteChef Automation Hub in an iframe.
 *
 * The Automation Hub gives end users an Automations view -- a self-serve catalog of published
 * templates alongside their own activated automations -- plus a Connections view and the workflow
 * builder for automations they own, all behind one iframe. When the iframe signals it is ready via
 * a postMessage, the parent sends the initialization parameters back.
 *
 * @param props - The configuration options for the embedded Automation Hub
 * @returns A React component that renders the embedded Automation Hub
 */
const AutomationHub = ({
    baseUrl = 'https://app.bytechef.io',
    className,
    connectionDialogAllowed = true,
    environment = 'PRODUCTION',
    includeComponents,
    jwtToken,
    sharedConnectionIds = [],
    tabs,
    theme,
}: AutomationHubProps) => {
    const iframeRef = useRef<HTMLIFrameElement>(null);
    const propsRef = useRef({
        connectionDialogAllowed,
        environment,
        includeComponents,
        jwtToken,
        sharedConnectionIds,
        tabs,
        theme,
    });

    // Kept up to date via an effect (rather than assigned during render) so that a late prop
    // change is still visible to the next EMBED_READY handshake, without mutating the ref while
    // rendering -- postMessage delivery is always async relative to React's render/effect cycle,
    // so this remains observably identical to an in-render assignment.
    useEffect(() => {
        propsRef.current = {
            connectionDialogAllowed,
            environment,
            includeComponents,
            jwtToken,
            sharedConnectionIds,
            tabs,
            theme,
        };
    }, [connectionDialogAllowed, environment, includeComponents, jwtToken, sharedConnectionIds, tabs, theme]);

    useEffect(() => {
        const targetOrigin = new URL(baseUrl).origin;

        const sendInitMessage = () => {
            if (iframeRef.current && iframeRef.current.contentWindow) {
                iframeRef.current.contentWindow.postMessage(
                    {
                        type: 'EMBED_INIT',
                        params: propsRef.current,
                    },
                    targetOrigin
                );
            }
        };

        const handleMessage = (event: MessageEvent) => {
            if (event.origin === targetOrigin && event.data.type === 'EMBED_READY') {
                sendInitMessage();
            }
        };

        window.addEventListener('message', handleMessage);

        return () => {
            window.removeEventListener('message', handleMessage);
        };
    }, [baseUrl]);

    return (
        <div className={className}>
            <iframe
                ref={iframeRef}
                src={`${baseUrl}/embedded/hub`}
                width="100%"
                height="100%"
                style={{border: 'none'}}
                title="Automation Hub"
            />
        </div>
    );
};

export default AutomationHub;
