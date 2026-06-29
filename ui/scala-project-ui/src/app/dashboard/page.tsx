"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Map, Plus, LogOut } from "lucide-react";
import { useAuth } from "../../contexts/AuthContext";
import { tripService, invitationService, Trip, Invitation } from "../../services/api";
import TripCard from "../../components/TripCard";
import TripFormModal from "../../components/TripFormModal";
import InvitationPanel from "../../components/InvitationPanel";

export default function DashboardPage() {
  const auth = useAuth();
  const router = useRouter();

  const [trips, setTrips] = useState<Trip[]>([]);
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [tripModalOpen, setTripModalOpen] = useState(false);
  const [editingTrip, setEditingTrip] = useState<Trip | undefined>(undefined);

  const loadData = useCallback(async () => {
    if (!auth.user) return;
    const token = auth.user.token;
    try {
      const [tripsData, invitationsData] = await Promise.all([
        tripService.listTrips(token),
        invitationService.listInvitations(token),
      ]);
      setTrips(tripsData);
      setInvitations(invitationsData);
    } catch {
      // Silently handled — user sees empty state
    } finally {
      setIsLoading(false);
    }
  }, [auth.user]);

  useEffect(() => {
    if (!auth.user) {
      router.replace("/");
      return;
    }
    loadData();
  }, [auth.user, router, loadData]);

  if (!auth.user) return null;

  const token = auth.user.token;
  const userId = auth.user.userId;

  const handleCreateTrip = async (data: { title: string; startDate: string; endDate: string }) => {
    const newTrip = await tripService.createTrip(token, data);
    setTrips((prev) => [newTrip, ...prev]);
  };

  const handleUpdateTrip = async (data: { title: string; startDate: string; endDate: string }) => {
    if (!editingTrip) return;
    await tripService.updateTrip(token, editingTrip.id, data);
    setTrips((prev) => prev.map((t) => (t.id === editingTrip.id ? { ...t, ...data } : t)));
  };

  const handleDeleteTrip = async (tripId: string) => {
    await tripService.deleteTrip(token, tripId);
    setTrips((prev) => prev.filter((t) => t.id !== tripId));
  };

  const handleRespond = async (id: string, status: "accepted" | "declined") => {
    await invitationService.respondToInvitation(token, id, status);
    setInvitations((prev) => prev.filter((inv) => inv.id !== id));
    if (status === "accepted") loadData();
  };

  const openCreate = () => {
    setEditingTrip(undefined);
    setTripModalOpen(true);
  };

  const openEdit = (trip: Trip) => {
    setEditingTrip(trip);
    setTripModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <nav className="bg-white shadow-sm px-4 py-3 sticky top-0 z-40">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2 text-blue-600 font-bold text-xl">
            <Map className="h-6 w-6" />
            <span>Travel Planner</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm text-slate-600 font-medium">@{auth.user.username}</span>
            <button
              onClick={auth.logout}
              className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800 transition-colors cursor-pointer"
            >
              <LogOut className="h-4 w-4" />
              Log out
            </button>
          </div>
        </div>
      </nav>

      <main className="max-w-6xl mx-auto px-4 py-8">
        {invitations.length > 0 && (
          <div className="mb-8">
            <InvitationPanel invitations={invitations} onRespond={handleRespond} />
          </div>
        )}

        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-slate-900">My trips</h1>
          <button
            onClick={openCreate}
            className="flex items-center gap-2 bg-blue-600 text-white font-semibold px-5 py-2 rounded-full hover:bg-blue-700 transition-colors cursor-pointer shadow-md"
          >
            <Plus className="h-4 w-4" />
            New trip
          </button>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((n) => (
              <div key={n} className="bg-white rounded-2xl shadow-md border border-slate-100 p-6 h-40 animate-pulse" />
            ))}
          </div>
        ) : trips.length === 0 ? (
          <div className="text-center py-20 text-slate-400">
            <Map className="h-12 w-12 mx-auto mb-4 opacity-30" />
            <p className="text-lg font-medium">No trips yet</p>
            <p className="text-sm mt-1">Create your first trip to get started!</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {trips.map((trip) => (
              <TripCard
                key={trip.id}
                trip={trip}
                isOwner={trip.ownerId === userId}
                onEdit={openEdit}
                onDelete={handleDeleteTrip}
              />
            ))}
          </div>
        )}
      </main>

      <TripFormModal
        isOpen={tripModalOpen}
        onClose={() => setTripModalOpen(false)}
        onSubmit={editingTrip ? handleUpdateTrip : handleCreateTrip}
        initialData={editingTrip}
        mode={editingTrip ? "edit" : "create"}
      />
    </div>
  );
}
