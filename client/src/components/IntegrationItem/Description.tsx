export const Description: React.FC<{description?: string}> = ({
    description,
}) => {
    return (
        <>
            {description && description.length > 135
                ? description.substring(0, 130) + '...'
                : description}
        </>
    );
};
