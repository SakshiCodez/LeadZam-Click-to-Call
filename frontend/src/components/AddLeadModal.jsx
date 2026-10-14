import React, { useState } from 'react';

export default function AddLeadModal({ isOpen, onClose, onAddLead }) {
  const [name, setName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('+91');
  const [stage, setStage] = useState('Discovery Call');
  const [notes, setNotes] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim() || !phoneNumber.trim()) return;

    onAddLead({
      id: Date.now().toString(),
      name: name.trim(),
      phoneNumber: phoneNumber.trim(),
      stage,
      notes: notes.trim(),
    });

    setName('');
    setPhoneNumber('+91');
    setNotes('');
    onClose();
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2 className="modal-title">Add New CRM Lead</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Full Name</label>
            <input
              type="text"
              placeholder="e.g. Ramesh Patel"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>Phone Number (E.164 format)</label>
            <input
              type="text"
              placeholder="+919876543210"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>Lead Stage</label>
            <select value={stage} onChange={(e) => setStage(e.target.value)}>
              <option value="Discovery Call">Discovery Call</option>
              <option value="Warm Lead">Warm Lead</option>
              <option value="Qualified">Qualified</option>
              <option value="Contract Sent">Contract Sent</option>
              <option value="Follow-up">Follow-up</option>
            </select>
          </div>

          <div className="form-group">
            <label>Notes (optional)</label>
            <input
              type="text"
              placeholder="e.g. Inquired about Android solution"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Add Lead
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
