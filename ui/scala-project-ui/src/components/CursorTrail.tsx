"use client";

import { useState, useEffect } from "react";

export default function CursorTrail() {
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
    <>
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
    </>
  );
}