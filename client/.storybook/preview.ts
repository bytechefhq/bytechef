import type {Preview} from '@storybook/react-vite';
import '../src/styles/index.css';
import '../src/styles/components.css';

const preview: Preview = {
    parameters: {
        controls: {
            matchers: {
                color: /(background|color)$/i,
                date: /Date$/i,
            },
        },
    },

    tags: ['autodocs'],
};

const style = document.createElement('style');

style.textContent = `
    body {
        overflow: auto !important;
    }
    
    #root {
        overflow: auto !important;
    }
    
    .sb-show-main {
        overflow: auto !important;
    }
`;

document.head.appendChild(style);

export default preview;
