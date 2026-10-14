import React from 'react';

export default function ActivityLog({ activities, onClear }) {
  if (activities.length === 0) return null;

  return (
    <div className="activity-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 className="section-title">Session Call Activity Log</h3>
        <button className="btn btn-secondary btn-sm" onClick={onClear}>
          Clear Log
        </button>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table className="log-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Contact</th>
              <th>Phone Number</th>
              <th>Status</th>
              <th>Firebase Message ID</th>
            </tr>
          </thead>
          <tbody>
            {activities.map((act) => (
              <tr key={act.id}>
                <td>{act.time}</td>
                <td><strong>{act.contactName}</strong></td>
                <td><code style={{ fontSize: '12px' }}>{act.phoneNumber}</code></td>
                <td>
                  {act.status === 'success' ? (
                    <span style={{ color: '#16A34A', fontWeight: 600 }}>✅ Sent to FCM</span>
                  ) : (
                    <span style={{ color: '#DC2626', fontWeight: 600 }}>❌ Failed</span>
                  )}
                </td>
                <td>
                  <code style={{ fontSize: '11px', color: '#64748B' }}>
                    {act.messageId ? act.messageId.slice(-16) : act.error || 'N/A'}
                  </code>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
