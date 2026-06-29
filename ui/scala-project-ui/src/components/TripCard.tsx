"use client";

import { useRouter } from "next/navigation";
import { Calendar, Pencil, Trash2 } from "lucide-react";
import { Trip } from "../services/api";

interface TripCardProps {
  trip: Trip;
  isOwner: boolean;
  onEdit: (trip: Trip) => void;
  onDelete: (tripId: string) => void;
}

function formatDate(dateStr: string) {
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export default function TripCard({ trip, isOwner, onEdit, onDelete }: TripCardProps) {
  const router = useRouter();

  return (
    <div className="bg-white rounded-2xl shadow-md border border-slate-100 p-6 flex flex-col gap-4 hover:shadow-lg transition-shadow">
      <div className="flex items-start justify-between gap-2">
        <h3 className="text-lg font-bold text-slate-900 leading-tight">{trip.title}</h3>
        {isOwner && (
          <div className="flex gap-1 shrink-0">
            <button
              onClick={() => onEdit(trip)}
              className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
              aria-label="Edit trip"
            >
              <Pencil className="h-4 w-4" />
            </button>
            <button
              onClick={() => onDelete(trip.id)}
              className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
              aria-label="Delete trip"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        )}
      </div>

      <div className="flex items-center gap-2 text-sm text-slate-500">
        <Calendar className="h-4 w-4 shrink-0" />
        <span>{formatDate(trip.startDate)} – {formatDate(trip.endDate)}</span>
      </div>

      {!isOwner && (
        <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full w-fit">
          Shared with you
        </span>
      )}

      <button
        onClick={() => router.push(`/trips/${trip.id}`)}
        className="mt-auto w-full bg-blue-600 text-white rounded-xl px-4 py-2 text-sm font-semibold hover:bg-blue-700 transition-colors cursor-pointer"
      >
        View trip
      </button>
    </div>
  );
}
