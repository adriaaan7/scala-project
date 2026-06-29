"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Map, Users, Calendar, ArrowRight } from "lucide-react";
import Image from "next/image";
import AuthModal from "../components/AuthModal";
import CursorTrail from "../components/CursorTrail";
import { useAuth } from "../contexts/AuthContext";

export default function Home() {
  const auth = useAuth();
  const router = useRouter();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalView, setAuthModalView] = useState<"login" | "register">("login");

  useEffect(() => {
    if (auth.user) router.replace("/dashboard");
  }, [auth.user, router]);

  const openAuthModal = (view: "login" | "register") => {
    setAuthModalView(view);
    setIsAuthModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-slate-50 relative overflow-hidden">
      <CursorTrail />

      <div className="relative z-10">
        <nav className="bg-white/80 backdrop-blur-sm shadow-sm p-4 sticky top-0 z-40">
          <div className="max-w-6xl mx-auto flex justify-between items-center">
            <div className="flex items-center space-x-2 text-blue-600 font-bold text-2xl">
              <Map className="h-8 w-8" />
              <span>Travel Planner</span>
            </div>
            <div>
              <button 
                onClick={() => openAuthModal("login")}
                className="text-slate-600 hover:text-blue-600 font-medium px-4 py-2 transition-colors cursor-pointer"
              >
                Log in
              </button>
              <button 
                onClick={() => openAuthModal("register")}
                className="bg-blue-600 text-white px-5 py-2 rounded-full font-medium hover:bg-blue-700 transition-colors cursor-pointer shadow-md"
              >
                Sign up
              </button>
            </div>
          </div>
        </nav>

        <main className="max-w-6xl mx-auto px-4 pt-20 pb-16 flex flex-col md:flex-row items-center">
          <div className="md:w-1/2 pr-8">
            <h1 className="text-5xl font-extrabold text-slate-900 leading-tight mb-6">
              Your perfect trip, <br />
              <span className="text-blue-600">planned together.</span>
            </h1>
            <p className="text-lg text-slate-600 mb-8">
              Build your ultimate itinerary with friends and family in real-time. Travel Planner is your collaborative workspace that turns chaotic group chats into a beautifully organized journey.
            </p>
            <div className="flex space-x-4">
              <button 
                onClick={() => openAuthModal("register")}
                className="flex items-center bg-blue-600 text-white px-6 py-3 rounded-full font-semibold hover:bg-blue-700 transition-all shadow-lg shadow-blue-200 cursor-pointer"
              >
                Create a shared plan
                <ArrowRight className="ml-2 h-5 w-5" />
              </button>
            </div>

            <div className="mt-12 flex flex-wrap gap-8 text-slate-500">
              <div className="flex items-center">
                <Users className="h-5 w-5 mr-2 text-blue-500" />
                <span>Real-time collaboration</span>
              </div>
              <div className="flex items-center">
                <Calendar className="h-5 w-5 mr-2 text-blue-500" />
                <span>Shared itineraries</span>
              </div>
              <div className="flex items-center">
                <Map className="h-5 w-5 mr-2 text-blue-500" />
                <span>Interactive maps</span>
              </div>
            </div>
          </div>

          <div className="md:w-1/2 mt-12 md:mt-0 w-full">
            <div className="bg-white p-2 rounded-2xl shadow-2xl border border-slate-100 transform md:rotate-2 hover:rotate-0 transition duration-500 overflow-hidden">
              <div className="relative h-[400px] w-full">
                <Image
                  src="/Landing_page_image.jpg"
                  alt="Travel Planner Collaborative App Mockup"
                  fill
                  sizes="(max-width: 768px) 100vw, 50vw"
                  className="rounded-xl object-cover"
                  priority
                />
              </div>
            </div>
          </div>
        </main>
      </div>

      <AuthModal 
        isOpen={isAuthModalOpen} 
        onClose={() => setIsAuthModalOpen(false)} 
        initialView={authModalView}
      />
    </div>
  );
}