export const Description: React.FC<{description: string}> = ({description}) => {
    return (
        <span className="truncate">
            {description.length > 135
                ? description.substring(0, 130) + '...'
                : description}
        </span>
    );
};
