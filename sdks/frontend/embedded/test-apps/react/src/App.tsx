import useConnectionDialog from '@bytechef-embedded/react';
import React from 'react';

function App() {
  const {openDialog} = useConnectionDialog({});

  return (
    <div>
      <button
          onClick={openDialog}>
          Connect
      </button>
    </div>
  );
}

export default App;
