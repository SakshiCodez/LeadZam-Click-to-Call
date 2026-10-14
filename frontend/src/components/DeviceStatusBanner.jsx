import React, { useState } from 'react';

export default function DeviceStatusBanner({ deviceStatus, onRefresh, manualToken, onManualTokenChange }) {
  const [showTokenInput, setShowTokenInput] = useState(false);

  const isOnline = deviceStatus?.registered || (manualToken && manualToken.trim().length > 0);

  return (
    <div className="device-banner">
      <div className="device-info">
        <span className={`status-dot ${isOnline ? 'online' : 'offline'}`} />
        <div>
          <div className="device-meta-title">
            Target Android Device: {isOnline ? (deviceStatus?.deviceName || 'Custom Token Device') : 'No Device Registered'}
          </div>
          <div className="device-meta-sub">
            {manualToken ? (
              <span>Manual Token: {manualToken.slice(0, 10)}...{manualToken.slice(-6)} (Option A)</span>
            ) : deviceStatus?.registered ? (
              <span>Registered Token: {deviceStatus.latestTokenMasked} (Option B auto-sync)</span>
            ) : (
              <span>Waiting for Android app registration or token paste</span>
            )}
          </div>
        </div>
      </div>

      <div className="token-config-group">
        <button 
          className="btn btn-secondary btn-sm"
          onClick={onRefresh}
          title="Refresh device status from backend"
        >
          🔄 Refresh
        </button>

        <button
          className="btn btn-secondary btn-sm"
          onClick={() => setShowTokenInput(!showTokenInput)}
        >
          {showTokenInput ? 'Hide Token Field' : 'Paste Device Token (Option A)'}
        </button>

        {showTokenInput && (
          <input
            type="text"
            className="input-token"
            placeholder="Paste FCM token here..."
            value={manualToken}
            onChange={(e) => onManualTokenChange(e.target.value)}
          />
        )}
      </div>
    </div>
  );
}
