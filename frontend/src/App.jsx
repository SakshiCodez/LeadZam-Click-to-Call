import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import DeviceStatusBanner from './components/DeviceStatusBanner';
import LeadCard from './components/LeadCard';
import AddLeadModal from './components/AddLeadModal';
import ActivityLog from './components/ActivityLog';
import { sendDialRequest, fetchDeviceStatus, getApiBaseUrl, setApiBaseUrl } from './services/api';

const INITIAL_LEADS = [
  {
    id: 'lead-1',
    name: 'Rahul Sharma',
    phoneNumber: '+919876543210',
    stage: 'Warm Lead',
    notes: 'Requested product walkthrough for sales team.',
  },
  {
    id: 'lead-2',
    name: 'Priya Verma',
    phoneNumber: '+919876543211',
    stage: 'Contract Sent',
    notes: 'Awaiting contract review signature.',
  },
  {
    id: 'lead-3',
    name: 'Amit Patel',
    phoneNumber: '+919876543212',
    stage: 'Discovery Call',
    notes: 'Initial inbound inquiry from website form.',
  },
  {
    id: 'lead-4',
    name: 'Sneha Reddy',
    phoneNumber: '+919876543213',
    stage: 'Qualified',
    notes: 'Budget approved for Q4 deployment.',
  },
  {
    id: 'lead-5',
    name: 'Ananya Roy',
    phoneNumber: '+919876543214',
    stage: 'Follow-up',
    notes: 'Requested follow-up call regarding API pricing.',
  },
];

export default function App() {
  const [leads, setLeads] = useState(INITIAL_LEADS);
  const [callingLeadId, setCallingLeadId] = useState(null);
  const [deviceStatus, setDeviceStatus] = useState(null);
  const [manualToken, setManualToken] = useState(() => localStorage.getItem('leadzam_manual_token') || '');
  const [backendUrl, setBackendUrlState] = useState(getApiBaseUrl());
  const [statusNotification, setStatusNotification] = useState(null);
  const [activities, setActivities] = useState([]);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  // Poll or fetch device status on mount
  useEffect(() => {
    loadDeviceStatus();
    const interval = setInterval(loadDeviceStatus, 15000);
    return () => clearInterval(interval);
  }, []);

  const loadDeviceStatus = async () => {
    const status = await fetchDeviceStatus();
    setDeviceStatus(status);
  };

  const handleManualTokenChange = (token) => {
    setManualToken(token);
    localStorage.setItem('leadzam_manual_token', token);
  };

  const handleConfigBackend = () => {
    const current = getApiBaseUrl();
    const nextUrl = window.prompt('Enter Spring Boot backend base URL:', current);
    if (nextUrl && nextUrl.trim()) {
      const saved = setApiBaseUrl(nextUrl);
      setBackendUrlState(saved);
      loadDeviceStatus();
      showToast('success', `Backend URL updated to ${saved}`);
    }
  };

  const showToast = (type, message, details = null) => {
    setStatusNotification({ type, message, details });
  };

  const handleCall = async (lead) => {
    setCallingLeadId(lead.id);
    showToast('loading', `Sending dial request for ${lead.name} (${lead.phoneNumber}) to backend...`);

    try {
      const response = await sendDialRequest({
        phoneNumber: lead.phoneNumber,
        contactName: lead.name,
        deviceToken: manualToken || undefined,
      });

      const messageIdShort = response.messageId ? response.messageId.slice(-16) : 'msg-ok';
      showToast(
        'success',
        `Request accepted by Firebase! Pre-filling ${lead.name}'s number on the Android dialer.`,
        `Message ID: ${response.messageId || 'Queued'}`
      );

      // Record in session activity log
      setActivities((prev) => [
        {
          id: Date.now().toString(),
          time: new Date().toLocaleTimeString(),
          contactName: lead.name,
          phoneNumber: lead.phoneNumber,
          status: 'success',
          messageId: response.messageId,
        },
        ...prev,
      ]);
    } catch (err) {
      showToast('error', `Failed to send call request: ${err.message}`);

      setActivities((prev) => [
        {
          id: Date.now().toString(),
          time: new Date().toLocaleTimeString(),
          contactName: lead.name,
          phoneNumber: lead.phoneNumber,
          status: 'error',
          error: err.message,
        },
        ...prev,
      ]);
    } finally {
      setCallingLeadId(null);
    }
  };

  const handleAddLead = (newLead) => {
    setLeads((prev) => [newLead, ...prev]);
    showToast('success', `Lead "${newLead.name}" added successfully.`);
  };

  return (
    <div className="app-container">
      <Header backendUrl={backendUrl} onConfigBackend={handleConfigBackend} />

      <DeviceStatusBanner
        deviceStatus={deviceStatus}
        onRefresh={loadDeviceStatus}
        manualToken={manualToken}
        onManualTokenChange={handleManualTokenChange}
      />

      {statusNotification && (
        <div className={`status-toast ${statusNotification.type}`}>
          <div>
            <strong>{statusNotification.message}</strong>
            {statusNotification.details && (
              <div style={{ fontSize: '12px', marginTop: '2px', opacity: 0.9 }}>
                {statusNotification.details}
              </div>
            )}
          </div>
          <button
            onClick={() => setStatusNotification(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px' }}
          >
            ✕
          </button>
        </div>
      )}

      <div className="leads-header">
        <div>
          <h2 className="section-title">Sales Pipeline Leads</h2>
          <p style={{ fontSize: '13px', color: '#64748B' }}>
            Click Call to push the phone number to your registered Android dialer via FCM.
          </p>
        </div>

        <button className="btn btn-primary" onClick={() => setIsAddModalOpen(true)}>
          ➕ Add Lead
        </button>
      </div>

      <div className="leads-grid">
        {leads.map((lead) => (
          <LeadCard
            key={lead.id}
            lead={lead}
            onCall={handleCall}
            isCalling={callingLeadId === lead.id}
          />
        ))}
      </div>

      <ActivityLog activities={activities} onClear={() => setActivities([])} />

      <AddLeadModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onAddLead={handleAddLead}
      />
    </div>
  );
}
