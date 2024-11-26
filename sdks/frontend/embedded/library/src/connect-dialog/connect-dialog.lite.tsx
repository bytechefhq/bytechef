import { Show, onMount, useState } from '@builder.io/mitosis';
import CloseButton from "./components/close-button.lite";
import ConnectButton from "./components/connect-button.lite";
import TabsList from "./components/tabs/tabs-list.lite";
import TabsItem from "./components/tabs/tabs-item.lite";
import fetchIntegration from "../middleware/fetch-integration";
import {Integration} from "../middleware/model/Integration";
import WorkflowsList from "./components/workflows/workflows-list.lite";
import PoweredBy from "./components/powered-by.lite";

interface ConnectDialogProps {
    onClose?: () => void;
}

export default function ConnectDialog(props: ConnectDialogProps) {
    const [integration, setIntegration] = useState<Integration | undefined>(undefined);

    onMount(() => {
        fetchIntegration().then((response) => {
            console.log(response);
            setIntegration(response)
        });
    });

    return <div css={{
        position: 'fixed',
        top: '0',
        left: '0',
        width: '100%',
        height: '100%',
        background: 'rgba(0,0,0,0.8)'
    }}>
        <div css={{
            maxWidth: '480px',
            minHeight: '300px',
            margin: '150px auto',
            backgroundColor: '#ffff',
            borderRadius: '6px',
            padding: '1rem',
            position: 'relative',
            display: 'flex',
            flexDirection: 'column',
            fontFamily: 'Arial, sans-serif',
        }}>
            <CloseButton onClose={() => props.onClose && props.onClose()} />

            <div css={{
                display: 'flex',
                alignItems: 'center',
                marginBottom: '1rem'
            }}>
                <div css={{
                    width: '24px',
                }}>
                    <img src={`data:image/svg+xml;utf8,${integration?.icon}`} alt={integration?.title}/>
                </div>

                <div css={{
                    fontSize: '1.3rem',
                    marginLeft: '0.5rem'
                }}>
                    {integration?.title}
                </div>
            </div>

            <div css={{
                display: 'flex',
                flexGrow: '1',
            }}>
                <Show when={integration} else={<>Loading...</>}>
                    <Show when={integration?.workflows.length} else={
                        <div css={{
                            display: 'flex',
                            flexDirection: 'column'
                        }}>
                            <div css={{
                                fontSize: '1rem',
                                marginBottom: '0.5rem'
                            }}>
                                Overview
                            </div>

                            <div css={{
                                color: '#737C86',
                                fontSize: '0.9rem',
                                lineHeight: '1.3'
                            }}>
                                {integration?.description}
                            </div>
                        </div>
                    }>
                        <TabsList
                            activeTab={'tab1'}
                            tabs={[{id: 'tab1', label: 'Overview'}, {id: 'tab2', label: 'Configuration'}]}
                        >
                            <TabsItem id="tab1">
                                <div css={{
                                    color: '#737C86',
                                    fontSize: '0.9rem',
                                    lineHeight: '1.3'
                                }}>
                                    {integration?.description}
                                </div>
                            </TabsItem>

                            <TabsItem id="tab2">
                                <WorkflowsList workflows={integration?.workflows}/>
                            </TabsItem>
                        </TabsList>
                    </Show>
                </Show>
            </div>

            <ConnectButton/>

            <PoweredBy />
        </div>
    </div>;
}
