import useConnectionDialog from '@bytechef-embedded/react';
import React, { useState } from 'react';

function App() {
  const {DialogComponent, openDialog} = useConnectionDialog({});

  return (
    <div>
      <button onClick={openDialog}>Connect</button>

        <DialogComponent />
    </div>
  );
}

export default App;
