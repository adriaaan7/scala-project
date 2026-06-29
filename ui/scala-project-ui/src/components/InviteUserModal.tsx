"use client";

import { useState, FormEvent } from "react";
import { X } from "lucide-react";

interface InviteUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onInvite: (username: string) => Promise<void>;
}

export default function InviteUserModal({ isOpen, onClose, onInvite }: InviteUserModalProps) {
  const [username, setUsername] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  if (!isOpen) return null;

  const handleClose = () => {
    setUsername("");
    setError("");
    setSuccess(false);
    onClose();
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim()) return;
    setIsLoading(true);
    setError("");
    setSuccess(false);
    try {
      await onInvite(username.trim());
      setSuccess(true);
      setUsername("");
    } catch (err: any) {
      setError(err.message || "Failed to send invitation.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-8 relative">
        <button onClick={handleClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 cursor-pointer">
          <X className="h-6 w-6" />
        </button>

        <h2 className="text-2xl font-bold text-slate-900 mb-2">Invite someone</h2>
        <p className="text-slate-500 text-sm mb-6">Enter the username of the person you want to invite to this trip.</p>

        {error && <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm mb-4">{error}</div>}
        {success && <div className="bg-green-50 text-green-700 p-3 rounded-lg text-sm mb-4">Invitation sent!</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Username</label>
            <input
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. travelguru99"
              className="w-full bg-slate-50 border border-slate-300 text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
            />
          </div>
          <button
            type="submit"
            disabled={isLoading || !username.trim()}
            className="w-full bg-blue-600 text-white rounded-lg px-4 py-3 font-semibold hover:bg-blue-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? "Sending..." : "Send invitation"}
          </button>
        </form>
      </div>
    </div>
  );
}
