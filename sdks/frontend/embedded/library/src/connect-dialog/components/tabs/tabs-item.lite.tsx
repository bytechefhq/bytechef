import { Show, useContext } from '@builder.io/mitosis';
import Context from "./tabs-list.context.lite";

export interface TabsItemProps {
    id: string;
    children: any;
}

export default function TabsItem(props: TabsItemProps) {
    const context = useContext(Context);

    return <Show when={context.activeTab === props.id}>
        <div
            css={{
                marginTop: '0.7rem',
                marginBottom: '0.7rem',
                paddingTop: '0.5rem',
                paddingBottom: '0.5rem',
                borderRadius: '0.5rem'
            }}
            role="tabpanel"
            aria-labelledby={`tab-${props.id}`}
            id={`panel-${props.id}`}
        >
            {props.children}
        </div>
    </Show>
};

