// LeadZam Backend API client service

const DEFAULT_API_BASE = 'http://localhost:8080';

export function getApiBaseUrl() {
  return localStorage.getItem('leadzam_backend_url') || DEFAULT_API_BASE;
}

export function setApiBaseUrl(url) {
  const sanitized = url.trim().replace(/\/+$/, '');
  localStorage.setItem('leadzam_backend_url', sanitized);
  return sanitized;
}

/**
 * Initiates click-to-call request for a selected lead.
 */
export async function sendDialRequest({ phoneNumber, contactName, deviceToken }) {
  const baseUrl = getApiBaseUrl();
  const endpoint = `${baseUrl}/api/dial`;

  const payload = {
    phoneNumber: phoneNumber.trim(),
    contactName: contactName.trim(),
  };

  if (deviceToken && deviceToken.trim().length > 0) {
    payload.deviceToken = deviceToken.trim();
  }

  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      const errorMsg = data.message || (data.details && Object.values(data.details).join(', ')) || `Backend HTTP ${response.status}`;
      throw new Error(errorMsg);
    }

    return data;
  } catch (err) {
    if (err.name === 'TypeError' && err.message.includes('fetch')) {
      throw new Error(`Cannot reach backend at ${baseUrl}. Ensure Spring Boot backend is running on port 8080.`);
    }
    throw err;
  }
}

/**
 * Checks target Android device registration status.
 */
export async function fetchDeviceStatus() {
  const baseUrl = getApiBaseUrl();
  const endpoint = `${baseUrl}/api/device-status`;

  try {
    const response = await fetch(endpoint);
    if (!response.ok) {
      throw new Error(`Status check returned HTTP ${response.status}`);
    }
    return await response.json();
  } catch (err) {
    return {
      registered: false,
      deviceCount: 0,
      latestTokenMasked: 'Backend offline',
      deviceName: null,
      error: err.message,
    };
  }
}
