import type { Meta, StoryObj } from '@storybook/react';

import ComboBox from './ComboBox';
import { comboBoxItemsMock } from './ComboBox.mock';

const meta: Meta<typeof ComboBox> = {
  component: ComboBox,
};

export default meta;

type Story = StoryObj<typeof ComboBox>;

export const Primary: Story = {
    args: {
        disabled: false,
        items: comboBoxItemsMock,
        name: 'Test',
        onBlur: () => {},
        onChange: () => {},
        value: comboBoxItemsMock[0].value,
    },
  };