import ComboBox from './ComboBox';
import {comboBoxItemsMock} from './ComboBox.mock';

import type {Meta, StoryObj} from '@storybook/react-vite';

const meta: Meta<typeof ComboBox> = {
    component: ComboBox,
};

export default meta;

type StoryType = StoryObj<typeof ComboBox>;

export const Primary: StoryType = {
    args: {
        disabled: false,
        items: comboBoxItemsMock,
        name: 'Test',
        onBlur: () => {},
        onChange: () => {},
        value: comboBoxItemsMock[0].value,
    },
};
