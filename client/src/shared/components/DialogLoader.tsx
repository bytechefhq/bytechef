const DialogLoader = () => (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/80 backdrop-blur-sm">
        <div className="flex flex-col items-center justify-center gap-4">
            <div className="flex animate-pulse space-x-2">
                <div className="size-4 rounded-full bg-gray-400"></div>

                <div className="size-4 rounded-full bg-gray-400"></div>

                <div className="size-4 rounded-full bg-gray-400"></div>

                <div className="size-4 rounded-full bg-gray-400"></div>
            </div>
        </div>
    </div>
);

export default DialogLoader;
