import React from 'react';

const STAGE_CLASSES = {
  'Warm Lead': 'stage-warm',
  'Contract Sent': 'stage-contract',
  'Discovery Call': 'stage-discovery',
  'Qualified': 'stage-qualified',
  'Follow-up': 'stage-followup',
};

export default function LeadCard({ lead, onCall, isCalling }) {
  const stageClass = STAGE_CLASSES[lead.stage] || 'stage-discovery';

  return (
    <div className="lead-card">
      <div>
        <div className="lead-card-top">
          <div>
            <h3 className="lead-name">{lead.name}</h3>
            <p className="lead-phone">{lead.phoneNumber}</p>
          </div>
          <span className={`stage-badge ${stageClass}`}>
            {lead.stage}
          </span>
        </div>

        {lead.notes && (
          <p style={{ fontSize: '12px', color: '#64748B', marginBottom: '16px' }}>
            {lead.notes}
          </p>
        )}
      </div>

      <button
        className="btn btn-call"
        onClick={() => onCall(lead)}
        disabled={isCalling}
        title={`Trigger phone dial for ${lead.name}`}
      >
        {isCalling ? (
          <>
            <span className="spinner" />
            Sending to Android Phone...
          </>
        ) : (
          <>
            <span>📞</span>
            Call {lead.name.split(' ')[0]}
          </>
        )}
      </button>
    </div>
  );
}
