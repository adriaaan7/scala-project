import config from "../../config.json";

const BASE = config.API_BASE_URL;

function authHeader(token: string) {
  return { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" };
}

async function handleResponse(res: Response) {
  if (res.status === 204 || res.status === 201 && res.headers.get("content-length") === "0") return null;
  if (res.ok) {
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  }
  let msg = `Request failed with status ${res.status}`;
  try {
    const body = await res.json();
    if (body?.message) msg = body.message;
  } catch {}
  throw new Error(msg);
}

// ---------- Auth ----------

export const authService = {
  login: async (username: string, password: string) => {
    const res = await fetch(`${BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    if (res.status === 401) throw new Error("Invalid username or password.");
    if (!res.ok) throw new Error("Login failed. Please try again later.");
    return res.json() as Promise<{ token: string; userId: string; username: string }>;
  },

  register: async (username: string, password: string) => {
    const res = await fetch(`${BASE}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    if (res.status === 409) throw new Error("Username already taken.");
    if (!res.ok) throw new Error("Registration failed. Please try again later.");
    return res.json();
  },
};

// ---------- Trips ----------

export interface Trip {
  id: string;
  title: string;
  startDate: string;
  endDate: string;
  ownerId: string;
}

export interface Place {
  id: string;
  tripId: string;
  name: string;
  description?: string;
  lat: number;
  lng: number;
  startDate: string;
  endDate: string;
}

export interface TripDetails extends Trip {
  isOwner: boolean;
  places: Place[];
}

export const tripService = {
  listTrips: async (token: string): Promise<Trip[]> => {
    const res = await fetch(`${BASE}/trips`, { headers: authHeader(token) });
    if (res.status === 401) throw new Error("Unauthorized");
    return handleResponse(res);
  },

  createTrip: async (token: string, data: { title: string; startDate: string; endDate: string }): Promise<Trip> => {
    const res = await fetch(`${BASE}/trips`, {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify(data),
    });
    if (res.status === 401) throw new Error("Unauthorized");
    return handleResponse(res);
  },

  getTripDetails: async (token: string, tripId: string): Promise<TripDetails> => {
    const res = await fetch(`${BASE}/trips/${tripId}`, { headers: authHeader(token) });
    if (res.status === 401) throw new Error("Unauthorized");
    if (res.status === 404) throw new Error("Trip not found");
    return handleResponse(res);
  },

  updateTrip: async (token: string, tripId: string, data: { title?: string; startDate?: string; endDate?: string }): Promise<void> => {
    const res = await fetch(`${BASE}/trips/${tripId}`, {
      method: "PATCH",
      headers: authHeader(token),
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Failed to update trip");
  },

  deleteTrip: async (token: string, tripId: string): Promise<void> => {
    const res = await fetch(`${BASE}/trips/${tripId}`, {
      method: "DELETE",
      headers: authHeader(token),
    });
    if (!res.ok) throw new Error("Failed to delete trip");
  },
};

// ---------- Places ----------

export const placeService = {
  addPlace: async (token: string, tripId: string, data: {
    name: string;
    description?: string;
    lat: number;
    lng: number;
    startDate: string;
    endDate: string;
  }): Promise<Place> => {
    const res = await fetch(`${BASE}/trips/${tripId}/places`, {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify(data),
    });
    return handleResponse(res);
  },

  updatePlace: async (token: string, tripId: string, placeId: string, data: {
    name?: string;
    description?: string;
    lat?: number;
    lng?: number;
    startDate?: string;
    endDate?: string;
  }): Promise<void> => {
    const res = await fetch(`${BASE}/trips/${tripId}/places/${placeId}`, {
      method: "PATCH",
      headers: authHeader(token),
      body: JSON.stringify(data),
    });
    await handleResponse(res);
  },

  deletePlace: async (token: string, tripId: string, placeId: string): Promise<void> => {
    const res = await fetch(`${BASE}/trips/${tripId}/places/${placeId}`, {
      method: "DELETE",
      headers: authHeader(token),
    });
    if (!res.ok) throw new Error("Failed to delete place");
  },
};

// ---------- Invitations ----------

export interface Invitation {
  id: string;
  title: string;
  senderUsername: string;
}

export const invitationService = {
  listInvitations: async (token: string): Promise<Invitation[]> => {
    const res = await fetch(`${BASE}/invitations`, { headers: authHeader(token) });
    if (!res.ok) throw new Error("Failed to load invitations");
    return handleResponse(res);
  },

  respondToInvitation: async (token: string, invitationId: string, status: "accepted" | "declined"): Promise<void> => {
    const res = await fetch(`${BASE}/invitations/${invitationId}`, {
      method: "PATCH",
      headers: authHeader(token),
      body: JSON.stringify({ status }),
    });
    await handleResponse(res);
  },

  inviteUser: async (token: string, tripId: string, username: string): Promise<void> => {
    const res = await fetch(`${BASE}/trips/${tripId}/invite`, {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify({ username }),
    });
    if (res.ok) return;
    await handleResponse(res);
  },
};
