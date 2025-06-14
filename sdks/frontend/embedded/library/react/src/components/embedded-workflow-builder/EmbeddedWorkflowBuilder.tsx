'use client';

/**
 * Props for the EmbeddedWorkflowBuilder component.
 * This interface defines all the configuration options needed to initialize and render
 * the embedded workflow builder iframe.
 */
interface EmbeddedWorkflowBuilderProps {
    /**
     * The base URL of the ByteChef application.
     * This URL is used to construct the iframe src attribute.
     * @default 'http://127.0.0.1:5173'
     */
    baseUrl?: string;

    /**
     * Whether to allow the connection dialog to be shown in the workflow builder.
     * When true, users can create and manage connections directly in the workflow builder.
     * When false, users can only use existing connections. Those existing connections can be
     * either shared connections created inside ByteChef and defined by `sharedConnectionIds`
     * or integration connections created via `ConnectDialog`.
     */
    connectionDialogAllowed: boolean;

    /**
     * The environment to use for the workflow builder.
     * This affects which environment's connections and configurations are used.
     * @default 'PRODUCTION'
     */
    environment?: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';

    /**
     * Array of component identifiers to include in the workflow builder.
     * This limits which integration components are available to the user.
     * Example: ['slack', 'googleMail', 'productboard']
     */
    includeComponents: string[];

    /**
     * JWT token for authentication with the ByteChef API.
     * This token is passed to the iframe via postMessage for API authorization.
     */
    jwtToken: string;

    /**
     * Array of connection IDs that should be shared with this workflow builder.
     * These connections will be available for use in the workflow being built.
     * Shared connections can be created via the ByteChef's '/embedded/connections' page.
     */
    sharedConnectionIds: number[];

    /**
     * The unique reference code for the workflow being edited.
     * This is used to load the correct workflow in the builder.
     */
    workflowReferenceCode: string;
}

/**
 * A component that embeds the ByteChef Workflow Builder in an iframe.
 *
 * This component creates an iframe that loads the ByteChef Workflow Builder UI and
 * initializes it with the provided configuration. When the iframe loads, it sends
 * a postMessage to the iframe with the initialization parameters.
 *
 * @param props - The configuration options for the embedded workflow builder
 * @returns A React component that renders the embedded workflow builder
 */

const EmbeddedWorkflowBuilder = ({
    baseUrl = 'https://app.bytechef.io',
    connectionDialogAllowed,
    environment = 'PRODUCTION',
    includeComponents,
    jwtToken,
    sharedConnectionIds,
    workflowReferenceCode,
}: EmbeddedWorkflowBuilderProps) => {
    /**
     * Handles the iframe load event.
     *
     * When the iframe is loaded, this function sends a postMessage to the iframe
     * with the initialization parameters needed by the ByteChef Workflow Builder.
     * This establishes communication between the parent application and the embedded iframe.
     */
    const handleIframeLoad = () => {
        const iframe = document.querySelector('iframe');

        if (iframe && iframe.contentWindow) {
            iframe.contentWindow.postMessage(
                {
                    type: 'EMBED_INIT',
                    params: {
                        connectionDialogAllowed,
                        environment,
                        includeComponents,
                        jwtToken,
                        sharedConnectionIds,
                    },
                },
                '*'
            );
        }
    };

    return (
        <div className="absolute inset-0 lg:pl-72">
            <iframe
                src={`${baseUrl}/embedded/workflow-builder/${workflowReferenceCode}`}
                width="100%"
                height="100%"
                style={{border: 'none'}}
                title="Workflow Builder"
                onLoad={handleIframeLoad}
            />
        </div>
    );
};

export default EmbeddedWorkflowBuilder;
