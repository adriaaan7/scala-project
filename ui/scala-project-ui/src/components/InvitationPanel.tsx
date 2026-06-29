"use client";

import { useState } from "react";
import { Check, X } from "lucide-react";
import { Invitation } from "../services/api";

interface InvitationPanelProps {
  invitations: Invitation[];
  onRespond: (id: string, status: "accepted" | "declined") => Promise<void>;
}

export default function InvitationPanel({ invitations, onRespond }: InvitationPanelProps) {
  const [pending, setPending] = useState<Record<string, "accepted" | "declined" | null>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});

  if (invitations.length === 0) return null;

  const handleClick = async (id: string, status: "accepted" | "declined") => {
    if (pending[id]) return;
    setPending((p) => ({ ...p, [id]: status }));
    setErrors((e) => ({ ...e, [id]: "" }));
    try {
      await onRespond(id, status);
    } catch (err: any) {
      setErrors((e) => ({ ...e, [id]: err.message || "Something went wrong" }));
      setPending((p) => ({ ...p, [id]: null }));
    }
  };

  return (
    <div className="bg-white rounded-2xl shadow-md border border-slate-100 p-6">
      <h2 className="text-base font-bold text-slate-900 mb-4">
        Pending invitations ({invitations.length})
      </h2>
      <ul className="space-y-3">
        {invitations.map((inv) => (
          <li key={inv.id}>
            <div className="flex items-center justify-between gap-3 bg-slate-50 rounded-xl px-4 py-3">
              <div className="min-w-0">
                <p className="font-medium text-slate-900 text-sm truncate">{inv.title}</p>
                <p className="text-xs text-slate-500">from @{inv.senderUsername}</p>
              </div>
              <div className="flex gap-2 shrink-0">
                <button
                  onClick={() => handleClick(inv.id, "accepted")}
                  disabled={!!pending[inv.id]}
                  className="flex items-center gap-1 bg-blue-600 text-white text-xs font-semibold px-3 py-1.5 rounded-lg hover:bg-blue-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Check className="h-3.5 w-3.5" />
                  {pending[inv.id] === "accepted" ? "Accepting…" : "Accept"}
                </button>
                <button
                  onClick={() => handleClick(inv.id, "declined")}
                  disabled={!!pending[inv.id]}
                  className="flex items-center gap-1 bg-slate-200 text-slate-700 text-xs font-semibold px-3 py-1.5 rounded-lg hover:bg-slate-300 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <X className="h-3.5 w-3.5" />
                  {pending[inv.id] === "declined" ? "Declining…" : "Decline"}
                </button>
              </div>
            </div>
            {errors[inv.id] && (
              <p className="mt-1 px-4 text-xs text-red-600">{errors[inv.id]}</p>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
