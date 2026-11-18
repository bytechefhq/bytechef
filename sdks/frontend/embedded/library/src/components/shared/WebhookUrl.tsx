import {useEffect, useId, useRef, useState} from 'react';
import CheckIcon from './assets/check.svg';
import CopyIcon from './assets/copy.svg';
import InfoIcon from './assets/info.svg';
import styles from './dialog.module.css';

interface WebhookUrlProps {
    subject?: 'automation' | 'workflow';
    webhookUrl: string;
}

const WebhookUrl = ({subject = 'workflow', webhookUrl}: WebhookUrlProps) => {
    const [copied, setCopied] = useState(false);

    const webhookUrlTooltipId = useId();

    const copiedTimeoutRef = useRef<ReturnType<typeof setTimeout>>(undefined);

    const handleCopyClick = async () => {
        try {
            await navigator.clipboard.writeText(webhookUrl);
        } catch (error) {
            console.error('Failed to copy the webhook URL:', error);

            return;
        }

        setCopied(true);

        clearTimeout(copiedTimeoutRef.current);

        copiedTimeoutRef.current = setTimeout(() => setCopied(false), 2000);
    };

    useEffect(() => () => clearTimeout(copiedTimeoutRef.current), []);

    return (
        <div className={styles.workflowWebhookUrlContainer}>
            <div className={styles.workflowWebhookUrlLabel}>
                <span>WEBHOOK URL</span>

                <span className={styles.tooltipContainer}>
                    <button
                        aria-describedby={webhookUrlTooltipId}
                        aria-label="About the webhook URL"
                        className={styles.infoButton}
                        type="button"
                    >
                        <img alt="" src={InfoIcon} />
                    </button>

                    <span
                        className={`${styles.tooltip} ${styles.tooltipStart}`}
                        id={webhookUrlTooltipId}
                        role="tooltip"
                    >
                        Send an HTTP request to this URL from any app or service to start this {subject}. Anyone with
                        the URL can start it, so share it only with services you trust.
                    </span>
                </span>
            </div>

            <div className={styles.workflowWebhookUrlValue}>
                <code title={webhookUrl}>{webhookUrl}</code>

                <span className={styles.tooltipContainer}>
                    <button
                        aria-label={copied ? 'Copied' : 'Copy webhook URL'}
                        className={styles.copyButton}
                        onClick={handleCopyClick}
                        type="button"
                    >
                        <img alt="" src={copied ? CheckIcon : CopyIcon} />
                    </button>

                    <span aria-hidden="true" className={`${styles.tooltip} ${styles.tooltipEnd}`}>
                        {copied ? 'Copied!' : 'Copy to clipboard'}
                    </span>
                </span>
            </div>
        </div>
    );
};

export default WebhookUrl;
