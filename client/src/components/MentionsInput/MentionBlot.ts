import {Quill} from 'react-quill';

const Embed = Quill.import('blots/embed');

export default class MentionBlot extends Embed {
    static blotName = 'property-mention';
    static tagName = 'div';
    static className = 'property-mention';

    static create(data: {denotationChar: string; value: string; componentIcon: string}) {
        const node = super.create();
        const iconNode = document.createElement('span');
        const contentNode = document.createElement('span');

        iconNode.innerHTML = data.componentIcon;
        contentNode.innerHTML = data.value;

        node.appendChild(iconNode);
        node.appendChild(contentNode);

        return MentionBlot.setDataValues(node, data);
    }

    static setDataValues(element: HTMLElement, data: {[key: string]: string}) {
        const domNode = element;

        Object.keys(data).forEach((key) => {
            domNode.dataset[key] = data[key];

            if (key === 'componentIcon') {
                domNode.dataset[key] = data[key];
            }
        });

        return domNode;
    }

    static value(domNode: {dataset: object}) {
        return domNode.dataset;
    }
}
