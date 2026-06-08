"use client";

import { useState, useEffect } from "react";
import { Map, Users, Calendar, ArrowRight } from "lucide-react";
import Image from "next/image";

export default function Home() {
  const [trail, setTrail] = useState<{ x: number; y: number; id: number }[]>([]);

  useEffect(() => {
    let idCounter = 0;

    const handleMouseMove = (e: MouseEvent) => {
      idCounter += 1;
      const newPoint = { x: e.clientX, y: e.clientY, id: idCounter };
      
      setTrail((prev) => [...prev.slice(-40), newPoint]);

      setTimeout(() => {
        setTrail((prev) => prev.filter((p) => p.id !== newPoint.id));
      }, 800);
    };

    window.addEventListener("mousemove", handleMouseMove);
    return () => window.removeEventListener("mousemove", handleMouseMove);
  }, []);

  return (
    <div className="min-h-screen bg-slate-50 relative overflow-hidden">
      <style dangerouslySetInnerHTML={{ __html: `
        @keyframes trailFade {
          0% { opacity: 1; }
          100% { opacity: 0; }
        }
      `}} />

      {trail.map((point) => (
        <div
          key={point.id}
          className="pointer-events-none fixed z-0"
          style={{
            left: point.x - 100,
            top: point.y - 100,
            width: 200,
            height: 200,
            backgroundImage: "url('/Landing_page_hidden_background.jpg')",
            backgroundAttachment: "fixed",
            backgroundSize: "cover",
            backgroundPosition: "center",
            WebkitMaskImage: "radial-gradient(circle, rgba(0,0,0,1) 0%, rgba(0,0,0,0) 70%)",
            maskImage: "radial-gradient(circle, rgba(0,0,0,1) 0%, rgba(0,0,0,0) 70%)",
            animation: "trailFade 0.8s ease-out forwards",
          }}
        />
      ))}

      <div className="relative z-10">
        <nav className="bg-white/80 backdrop-blur-sm shadow-sm p-4 sticky top-0">
          <div className="max-w-6xl mx-auto flex justify-between items-center">
            <div className="flex items-center space-x-2 text-blue-600 font-bold text-2xl">
              <Map className="h-8 w-8" />
              <span>Travel Planner</span>
            </div>
            <div>
              <button className="text-slate-600 hover:text-blue-600 font-medium px-4 py-2 transition-colors cursor-pointer">
                Log in
              </button>
              <button className="bg-blue-600 text-white px-5 py-2 rounded-full font-medium hover:bg-blue-700 transition-colors cursor-pointer shadow-md">
                Start planning
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
              <button className="flex items-center bg-blue-600 text-white px-6 py-3 rounded-full font-semibold hover:bg-blue-700 transition-all shadow-lg shadow-blue-200 cursor-pointer">
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
    </div>
  );
}