"use client";

import { useState } from "react";
import { Plus, Pencil, Trash2, MapPin, Clock } from "lucide-react";
import { Place, placeService } from "../services/api";
import PlaceFormModal from "./PlaceFormModal";

interface PlaceListProps {
  places: Place[];
  tripId: string;
  tripStartDate: string;
  tripEndDate: string;
  token: string;
  onPlacesChanged: () => void;
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function PlaceList({ places, tripId, tripStartDate, tripEndDate, token, onPlacesChanged }: PlaceListProps) {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingPlace, setEditingPlace] = useState<Place | undefined>(undefined);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const sorted = [...places].sort(
    (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
  );

  const handleAdd = async (data: {
    name: string;
    description?: string;
    lat: number;
    lng: number;
    startDate: string;
    endDate: string;
  }) => {
    await placeService.addPlace(token, tripId, data);
    onPlacesChanged();
  };

  const handleEdit = async (data: {
    name?: string;
    description?: string;
    lat?: number;
    lng?: number;
    startDate?: string;
    endDate?: string;
  }) => {
    if (!editingPlace) return;
    await placeService.updatePlace(token, tripId, editingPlace.id, data);
    onPlacesChanged();
  };

  const handleDelete = async (placeId: string) => {
    setDeletingId(placeId);
    try {
      await placeService.deletePlace(token, tripId, placeId);
      onPlacesChanged();
    } finally {
      setDeletingId(null);
    }
  };

  const openAdd = () => {
    setEditingPlace(undefined);
    setModalOpen(true);
  };

  const openEdit = (place: Place) => {
    setEditingPlace(place);
    setModalOpen(true);
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold text-slate-900">Places</h2>
        <button
          onClick={openAdd}
          className="flex items-center gap-2 bg-blue-600 text-white text-sm font-semibold px-4 py-2 rounded-xl hover:bg-blue-700 transition-colors cursor-pointer"
        >
          <Plus className="h-4 w-4" />
          Add place
        </button>
      </div>

      {sorted.length === 0 ? (
        <div className="text-center py-12 text-slate-400">
          <MapPin className="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p>No places yet. Add your first stop!</p>
        </div>
      ) : (
        <ol className="space-y-3">
          {sorted.map((place, index) => (
            <li key={place.id} className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5 flex gap-4">
              <div className="flex-shrink-0 w-8 h-8 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center font-bold text-sm">
                {index + 1}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold text-slate-900">{place.name}</h3>
                  <div className="flex gap-1 shrink-0">
                    <button
                      onClick={() => openEdit(place)}
                      className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(place.id)}
                      disabled={deletingId === place.id}
                      className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
                {place.description && (
                  <p className="text-sm text-slate-600 mt-1">{place.description}</p>
                )}
                <div className="flex flex-wrap gap-3 mt-2 text-xs text-slate-500">
                  <span className="flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5" />
                    {formatDateTime(place.startDate)} – {formatDateTime(place.endDate)}
                  </span>
                  <span className="flex items-center gap-1">
                    <MapPin className="h-3.5 w-3.5" />
                    {place.lat}, {place.lng}
                  </span>
                </div>
              </div>
            </li>
          ))}
        </ol>
      )}

      <PlaceFormModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSubmit={editingPlace ? handleEdit : handleAdd}
        initialData={editingPlace}
        existingPlaces={sorted}
        tripStartDate={tripStartDate}
        tripEndDate={tripEndDate}
        mode={editingPlace ? "edit" : "create"}
      />
    </div>
  );
}
