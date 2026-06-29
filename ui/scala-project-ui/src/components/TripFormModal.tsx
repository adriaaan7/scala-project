"use client";

import { useState, FormEvent, useEffect } from "react";
import { X } from "lucide-react";
import { Trip } from "../services/api";

interface TripFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { title: string; startDate: string; endDate: string }) => Promise<void>;
  initialData?: Trip;
  mode: "create" | "edit";
}

export default function TripFormModal({ isOpen, onClose, onSubmit, initialData, mode }: TripFormModalProps) {
  const [title, setTitle] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isOpen) {
      setTitle(initialData?.title ?? "");
      setStartDate(initialData?.startDate ?? "");
      setEndDate(initialData?.endDate ?? "");
      setError("");
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const isValid = title.trim().length > 0 && startDate && endDate && startDate <= endDate;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!isValid) return;
    setIsLoading(true);
    setError("");
    try {
      await onSubmit({ title: title.trim(), startDate, endDate });
      onClose();
    } catch (err: any) {
      setError(err.message || "Something went wrong.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 relative">
        <button onClick={onClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 cursor-pointer">
          <X className="h-6 w-6" />
        </button>

        <h2 className="text-2xl font-bold text-slate-900 mb-6">
          {mode === "create" ? "New trip" : "Edit trip"}
        </h2>

        {error && (
          <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm mb-4">{error}</div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Title</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Summer in Paris"
              className="w-full bg-slate-50 border border-slate-300 text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Start date</label>
              <input
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">End date</label>
              <input
                type="date"
                required
                value={endDate}
                min={startDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading || !isValid}
            className="w-full bg-blue-600 text-white rounded-lg px-4 py-3 font-semibold hover:bg-blue-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed mt-2"
          >
            {isLoading ? "Saving..." : (mode === "create" ? "Create trip" : "Save changes")}
          </button>
        </form>
      </div>
    </div>
  );
}
