interface CloseButtonProps {
    onClose?: (e: any) => void;
}

export default function ConnectButton(props: CloseButtonProps) {
    return <button css={{
        backgroundColor: '#000',
        borderRadius: '10px',
        cursor: 'pointer',
        fontSize: '1rem',
        padding: '0.7rem',
        right: '0.4rem',
        top: '0.4rem',
        outline: 'none',
        border: 'none',
        fontWeight: '500',
        color: '#fff',
        width: '100%',
    }} onClick={(e) => props.onClose && props.onClose(e)}>
        Connect
    </button>
}
