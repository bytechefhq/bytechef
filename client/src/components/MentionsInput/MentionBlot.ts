import {Quill} from 'react-quill';

const Embed = Quill.import('blots/embed');

export default class MentionBlot extends Embed {
    static blotName = 'bytechef-mention';
    static tagName = 'div';
    static className = 'bytechef-mention';

    static create(data: {denotationChar: string; value: string; icon: string}) {
        const node = super.create();
        const iconNode = document.createElement('span');
        const contentNode = document.createElement('span');

        iconNode.innerHTML = data.icon;
        contentNode.innerHTML = data.value;

        node.appendChild(iconNode);
        node.appendChild(contentNode);

        return MentionBlot.setDataValues(node, data);
    }

    static setDataValues(element: HTMLElement, data: {[key: string]: string}) {
        const domNode = element;

        Object.keys(data).forEach((key) => {
            domNode.dataset[key] = data[key];

            if (key === 'component') {
                domNode.dataset[key] = JSON.stringify(data[key]);
            }
        });

        return domNode;
    }

    static value(domNode: {dataset: object}) {
        return domNode.dataset;
    }
}
