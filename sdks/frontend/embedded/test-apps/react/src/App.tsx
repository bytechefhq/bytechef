import useConnectionDialog from '@bytechef-embedded/react';
import React from 'react';

function App() {
  const {openDialog} = useConnectionDialog({});

  return (
    <div className="fixed inset-0 flex items-center justify-center">
      <button
          className="rounded-md p-3 font-medium transition-all hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-slate-300"
          onClick={openDialog}>
          Connect
      </button>
    </div>
  );
}

export default App;
