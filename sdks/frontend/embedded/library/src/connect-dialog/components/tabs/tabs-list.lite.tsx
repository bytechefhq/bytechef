import { useState } from '@builder.io/mitosis';
import Context from './tabs-list.context.lite';
import TabsListButton from "./tabs-list-button.lite";

export interface TabsListProps {
    activeTab: string;
    tabs: {id: string, label: string}[];
    children: any;
}

export default function TabsList(props: TabsListProps) {
    const [activeTab, setActiveTab] = useState(props.activeTab);

    return (
        <Context.Provider value={{ activeTab: activeTab }}>
            <div css={{
                width: '100%',
            }}>
                <nav css={{
                    overflowX: 'scroll',
                    scrollbarWidth: 'none',
                    overflowScrolling: 'touch',
                    '-webkit-overflow-scrolling': 'touch'
                }}>
                    <ul css={{
                        width: 'fit-content',
                        minWidth: '100%',
                        color: '#57606f',
                        display: 'flex',
                        gap: '0.5em',
                        borderBottom: '2px solid #ddd',
                        margin: '0',
                        padding: '0'
                    }} role="tablist" aria-orientation="horizontal">
                        {props.tabs.map((tab, index) => {
                            return <li css={{
                                display: 'block',
                                marginBottom: '-2px'
                            }} key={`tab-${index}`}>
                                <TabsListButton
                                    activeTab={activeTab}
                                    id={tab.id}
                                    label={tab.label}
                                    onClick={(tabId) => setActiveTab(tabId)}
                                />
                            </li>
                        })}
                    </ul>
                </nav>

                {props.children}
            </div>
        </Context.Provider>
    );
};
