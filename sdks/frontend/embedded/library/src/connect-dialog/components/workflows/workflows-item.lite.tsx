import {Workflow} from '../../../middleware/model/Workflow';
import SwitchButton from './switch-button.lite';

interface WorkflowsItemProps {
    workflow?: Workflow;
}

export default function WorkflowsItem(props: WorkflowsItemProps) {
    return  <div css={{
        display: 'flex',
        paddingTop: '0.8rem',
        paddingBottom: '0.8rem',
        alignItems: 'center',
        ':first-child': {
            paddingTop: '0'
        },
        ':last-child': {
            paddingTop: '0'
        }
    }}>
        <div css={{
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'between',
            flexGrow: '1'
        }}>
            <div css={{
                fontSize: '0.9rem',
                paddingBottom: '0.3rem'
            }}>
                {props.workflow?.label}
            </div>

            <div css={{
                color: '#737C86',
                fontSize: '0.9rem',
                lineHeight: '1.3'
            }}>
                {props.workflow?.description}
            </div>
        </div>

        <div css={{
            display: 'flex',
            justifyContent: 'end',
            marginLeft: '1rem'
        }}>
            <SwitchButton />
        </div>
    </div>
}
