"use client";

import { useState, FormEvent, useEffect } from "react";
import dynamic from "next/dynamic";
import { X, MapPin } from "lucide-react";
import { Place } from "../services/api";

const MapPicker = dynamic(() => import("./MapPicker"), { ssr: false });

interface PlaceFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: {
    name: string;
    description?: string;
    lat: number;
    lng: number;
    startDate: string;
    endDate: string;
  }) => Promise<void>;
  initialData?: Place;
  existingPlaces?: Place[];
  tripStartDate?: string;
  tripEndDate?: string;
  mode: "create" | "edit";
}

function detectBoundsError(startLocal: string, endLocal: string, tripStart?: string, tripEnd?: string): string | null {
  if (!startLocal || !endLocal || !tripStart || !tripEnd) return null;
  const placeStartDate = startLocal.slice(0, 10);
  const placeEndDate = endLocal.slice(0, 10);
  if (placeStartDate < tripStart) return `Start must be on or after trip start (${tripStart})`;
  if (placeEndDate > tripEnd) return `End must be on or before trip end (${tripEnd})`;
  return null;
}

function detectOverlap(startLocal: string, endLocal: string, places: Place[], excludeId?: string): string | null {
  if (!startLocal || !endLocal) return null;
  const s = new Date(startLocal).getTime();
  const e = new Date(endLocal).getTime();
  const conflicting = places.find((p) => {
    if (p.id === excludeId) return false;
    const ps = new Date(p.startDate).getTime();
    const pe = new Date(p.endDate).getTime();
    return s < pe && ps < e;
  });
  return conflicting ? `Overlaps with "${conflicting.name}"` : null;
}

function toDatetimeLocal(iso: string) {
  if (!iso) return "";
  return iso.slice(0, 16);
}

function toIso(local: string) {
  if (!local) return "";
  return new Date(local).toISOString();
}

export default function PlaceFormModal({ isOpen, onClose, onSubmit, initialData, existingPlaces = [], tripStartDate, tripEndDate, mode }: PlaceFormModalProps) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isOpen) {
      setName(initialData?.name ?? "");
      setDescription(initialData?.description ?? "");
      setCoords(
        initialData
          ? { lat: Number(initialData.lat), lng: Number(initialData.lng) }
          : null
      );
      setStartDate(initialData ? toDatetimeLocal(initialData.startDate) : "");
      setEndDate(initialData ? toDatetimeLocal(initialData.endDate) : "");
      setError("");
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const boundsError = detectBoundsError(startDate, endDate, tripStartDate, tripEndDate);
  const overlapError = boundsError ? null : detectOverlap(startDate, endDate, existingPlaces, initialData?.id);
  const dateError = boundsError ?? overlapError;

  const isValid =
    name.trim().length > 0 &&
    coords !== null &&
    startDate && endDate && startDate <= endDate &&
    dateError === null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!isValid || !coords) return;
    setIsLoading(true);
    setError("");
    try {
      await onSubmit({
        name: name.trim(),
        description: description.trim() || undefined,
        lat: coords.lat,
        lng: coords.lng,
        startDate: toIso(startDate),
        endDate: toIso(endDate),
      });
      onClose();
    } catch (err: any) {
      setError(err.message || "Something went wrong.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg relative overflow-y-auto max-h-[90vh]">
        <div className="sticky top-0 bg-white px-8 pt-8 pb-4 z-10 border-b border-slate-100">
          <button onClick={onClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 cursor-pointer">
            <X className="h-6 w-6" />
          </button>
          <h2 className="text-2xl font-bold text-slate-900">
            {mode === "create" ? "Add place" : "Edit place"}
          </h2>
        </div>

        <div className="px-8 py-4">
          {error && <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm mb-4">{error}</div>}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Name</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Eiffel Tower"
                className="w-full bg-slate-50 border border-slate-300 text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Description <span className="text-slate-400 font-normal">(optional)</span>
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Notes about this place..."
                rows={2}
                className="w-full bg-slate-50 border border-slate-300 text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors resize-none"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Location <span className="text-slate-400 font-normal">— click the map to pin</span>
              </label>
              <div className="rounded-xl overflow-hidden border border-slate-200">
                <MapPicker value={coords} onChange={setCoords} />
              </div>
              {coords ? (
                <p className="mt-2 text-xs text-slate-500 flex items-center gap-1">
                  <MapPin className="h-3.5 w-3.5 text-blue-500" />
                  {coords.lat.toFixed(5)}, {coords.lng.toFixed(5)}
                </p>
              ) : (
                <p className="mt-2 text-xs text-slate-400">No location selected yet</p>
              )}
            </div>

            <div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Start</label>
                  <input
                    type="datetime-local"
                    required
                    value={startDate}
                    min={tripStartDate ? `${tripStartDate}T00:00` : undefined}
                    max={tripEndDate ? `${tripEndDate}T23:59` : undefined}
                    onChange={(e) => setStartDate(e.target.value)}
                    className={`w-full bg-slate-50 border text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors ${dateError ? "border-red-400" : "border-slate-300"}`}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">End</label>
                  <input
                    type="datetime-local"
                    required
                    value={endDate}
                    min={startDate || (tripStartDate ? `${tripStartDate}T00:00` : undefined)}
                    max={tripEndDate ? `${tripEndDate}T23:59` : undefined}
                    onChange={(e) => setEndDate(e.target.value)}
                    className={`w-full bg-slate-50 border text-slate-900 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors ${dateError ? "border-red-400" : "border-slate-300"}`}
                  />
                </div>
              </div>
              {dateError && (
                <p className="mt-1.5 text-xs text-red-600 flex items-center gap-1">
                  ⚠ {dateError}
                </p>
              )}
            </div>

            <div className="pb-2">
              <button
                type="submit"
                disabled={isLoading || !isValid}
                className="w-full bg-blue-600 text-white rounded-lg px-4 py-3 font-semibold hover:bg-blue-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? "Saving..." : (mode === "create" ? "Add place" : "Save changes")}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
