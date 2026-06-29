"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import dynamic from "next/dynamic";
import { ArrowLeft, Map, Pencil, Trash2, UserPlus, LogOut } from "lucide-react";
import { useAuth } from "../../../contexts/AuthContext";
import { tripService, invitationService, TripDetails } from "../../../services/api";
import PlaceList from "../../../components/PlaceList";
import TripFormModal from "../../../components/TripFormModal";
import InviteUserModal from "../../../components/InviteUserModal";

const TripMap = dynamic(() => import("../../../components/TripMap"), { ssr: false });

function formatDate(dateStr: string) {
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
  });
}

export default function TripDetailPage() {
  const auth = useAuth();
  const router = useRouter();
  const params = useParams();
  const tripId = params.tripId as string;

  const [trip, setTrip] = useState<TripDetails | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [inviteModalOpen, setInviteModalOpen] = useState(false);

  const loadTrip = useCallback(async () => {
    if (!auth.user) return;
    try {
      const data = await tripService.getTripDetails(auth.user.token, tripId);
      setTrip(data);
    } catch (err: any) {
      setError(err.message || "Failed to load trip.");
    } finally {
      setIsLoading(false);
    }
  }, [auth.user, tripId]);

  useEffect(() => {
    if (!auth.user) {
      router.replace("/");
      return;
    }
    loadTrip();
  }, [auth.user, router, loadTrip]);

  if (!auth.user) return null;

  const token = auth.user.token;

  const handleUpdateTrip = async (data: { title: string; startDate: string; endDate: string }) => {
    await tripService.updateTrip(token, tripId, data);
    setTrip((prev) => prev ? { ...prev, ...data } : prev);
  };

  const handleDeleteTrip = async () => {
    await tripService.deleteTrip(token, tripId);
    router.push("/dashboard");
  };

  const handleInvite = async (username: string) => {
    await invitationService.inviteUser(token, tripId, username);
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <nav className="bg-white shadow-sm px-4 py-3 sticky top-0 z-40">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <button
            onClick={() => router.push("/dashboard")}
            className="flex items-center gap-2 text-slate-500 hover:text-slate-800 transition-colors cursor-pointer text-sm font-medium"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to trips
          </button>
          <div className="flex items-center gap-2 text-blue-600 font-bold text-lg">
            <Map className="h-5 w-5" />
            <span>Travel Planner</span>
          </div>
          <button
            onClick={auth.logout}
            className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800 transition-colors cursor-pointer"
          >
            <LogOut className="h-4 w-4" />
            Log out
          </button>
        </div>
      </nav>

      <main className="max-w-4xl mx-auto px-4 py-8">
        {isLoading ? (
          <div className="space-y-4">
            <div className="h-10 bg-slate-200 rounded-xl w-64 animate-pulse" />
            <div className="h-5 bg-slate-100 rounded-xl w-48 animate-pulse" />
          </div>
        ) : error ? (
          <div className="text-center py-20 text-red-500">
            <p>{error}</p>
          </div>
        ) : trip ? (
          <>
            <div className="bg-white rounded-2xl shadow-md border border-slate-100 p-6 mb-6">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-3 mb-1">
                    <h1 className="text-2xl font-bold text-slate-900">{trip.title}</h1>
                    {!trip.isOwner && (
                      <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full">
                        Shared
                      </span>
                    )}
                  </div>
                  <p className="text-slate-500 text-sm">
                    {formatDate(trip.startDate)} – {formatDate(trip.endDate)}
                  </p>
                </div>

                {trip.isOwner && (
                  <div className="flex gap-2 shrink-0">
                    <button
                      onClick={() => setInviteModalOpen(true)}
                      className="flex items-center gap-1.5 text-sm font-semibold text-blue-600 border border-blue-200 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
                    >
                      <UserPlus className="h-4 w-4" />
                      Invite
                    </button>
                    <button
                      onClick={() => setEditModalOpen(true)}
                      className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition-colors cursor-pointer"
                      aria-label="Edit trip"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      onClick={handleDeleteTrip}
                      className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors cursor-pointer"
                      aria-label="Delete trip"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                )}
              </div>
            </div>

            {trip.places.length > 0 && (
              <div className="bg-white rounded-2xl shadow-md border border-slate-100 overflow-hidden mb-6">
                <TripMap places={trip.places} />
              </div>
            )}

            <PlaceList
              places={trip.places}
              tripId={trip.id}
              tripStartDate={trip.startDate}
              tripEndDate={trip.endDate}
              token={token}
              onPlacesChanged={loadTrip}
            />
          </>
        ) : null}
      </main>

      {trip && (
        <>
          <TripFormModal
            isOpen={editModalOpen}
            onClose={() => setEditModalOpen(false)}
            onSubmit={handleUpdateTrip}
            initialData={trip}
            mode="edit"
          />
          <InviteUserModal
            isOpen={inviteModalOpen}
            onClose={() => setInviteModalOpen(false)}
            onInvite={handleInvite}
          />
        </>
      )}
    </div>
  );
}
