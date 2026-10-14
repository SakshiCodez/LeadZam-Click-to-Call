import React from 'react';

export default function Header({ backendUrl, onConfigBackend }) {
  return (
    <header className="header-bar">
      <div className="brand">
        <div className="brand-icon">
          📞
        </div>
        <div>
          <h1 className="brand-title">LeadZam</h1>
          <p className="brand-subtitle">Click-to-Call CRM • Android Dialer Integration</p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <button 
          className="btn btn-secondary btn-sm"
          onClick={onConfigBackend}
          title="Configure Spring Boot API base URL"
        >
          ⚙️ API: {backendUrl}
        </button>
      </div>
    </header>
  );
}
