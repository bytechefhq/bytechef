import { Show } from '@builder.io/mitosis';

export interface TabsListButtonProps {
    activeTab: string;
    id: string;
    label: string;
    onClick: (tabId: string) => void;
}

export default function TabsListButton(props: TabsListButtonProps) {
    return <button
        key={`tab-btn-${props.id}`}
        role="tab"
        id={`tab-${props.id}`}
        aria-controls={`panel-${props.id}`}
        aria-selected={props.activeTab === props.id}
        onClick={() => props.onClick(props.id)}
        css={{
            fontSize: '0.9rem',
            padding: '0.75em 1em',
            backgroundColor: 'transparent',
            border: '2px solid #ddd',
            borderWidth: '0 0 2px',
            cursor: 'pointer',
            whiteSpace: 'nowrap'
        }}
        style={{
            color: props.activeTab === props.id ? '#2563EB' : 'inherit',
            borderColor: props.activeTab === props.id ? '#2563EB' : 'transparent',
        }}
    >
        {props.label}
    </button>
}
