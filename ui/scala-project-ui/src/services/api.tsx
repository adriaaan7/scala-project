import config from "../../config.json";

export const authService = {
  login: async (username: string, password: string) => {
    const response = await fetch(`${config.API_BASE_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    if (response.status === 401) throw new Error("Invalid username or password.");
    if (!response.ok) throw new Error("Login failed. Please try again later.");
    
    return response.status === 204 ? null : response.json();
  },

  register: async (username: string, password: string) => {
    const response = await fetch(`${config.API_BASE_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    if (response.status === 409) throw new Error("Username already taken.");
    if (!response.ok) throw new Error("Registration failed. Please try again later.");
    
    return response.json();
  }
};