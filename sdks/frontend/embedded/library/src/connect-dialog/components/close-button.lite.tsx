interface CloseButtonProps {
    onClose?: (e: any) => void;
}

export default function CloseButton(props: CloseButtonProps) {
    return <button css={{
        position: 'absolute',
        backgroundColor: 'transparent',
        borderRadius: '3px',
        cursor: 'pointer',
        fontSize: '0.8rem',
        padding: '0.5rem',
        right: '0.2rem',
        top: '0.4rem',
        outline: 'none',
        border: 'none',
        fontWeight: '500',
        textTransform: 'uppercase'
    }} onClick={(e) => props.onClose && props.onClose(e)}>
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2" strokeL-linejoin="round" className="lucide lucide-x">
            <path d="M18 6 6 18"/>
            <path d="m6 6 12 12"/>
        </svg>
    </button>
}
