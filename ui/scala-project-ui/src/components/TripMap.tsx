"use client";

import { useEffect } from "react";
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { Place } from "../services/api";

// Fix webpack/Next.js default icon paths
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

function numberedIcon(n: number) {
  return L.divIcon({
    className: "",
    html: `<div style="
      width:32px;height:32px;border-radius:50%;
      background:#2563eb;color:#fff;
      font-size:13px;font-weight:700;
      display:flex;align-items:center;justify-content:center;
      box-shadow:0 2px 6px rgba(0,0,0,0.35);
      border:2px solid #fff;
    ">${n}</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
    popupAnchor: [0, -18],
  });
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function FitBounds({ places }: { places: Place[] }) {
  const map = useMap();
  useEffect(() => {
    if (places.length === 0) return;
    if (places.length === 1) {
      map.setView([Number(places[0].lat), Number(places[0].lng)], 12);
      return;
    }
    const bounds = L.latLngBounds(places.map((p) => [Number(p.lat), Number(p.lng)]));
    map.fitBounds(bounds, { padding: [48, 48], maxZoom: 14 });
  }, [places, map]);
  return null;
}

interface TripMapProps {
  places: Place[];
}

export default function TripMap({ places }: TripMapProps) {
  const sorted = [...places].sort(
    (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
  );

  const polylinePositions: [number, number][] = sorted.map((p) => [Number(p.lat), Number(p.lng)]);
  const defaultCenter: [number, number] = sorted.length > 0
    ? [Number(sorted[0].lat), Number(sorted[0].lng)]
    : [20, 0];

  return (
    <MapContainer
      center={defaultCenter}
      zoom={sorted.length > 0 ? 6 : 2}
      style={{ height: "420px", width: "100%" }}
      className="z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <FitBounds places={sorted} />

      {sorted.length >= 2 && (
        <Polyline
          positions={polylinePositions}
          pathOptions={{ color: "#2563eb", weight: 2.5, dashArray: "6 6", opacity: 0.7 }}
        />
      )}

      {sorted.map((place, index) => (
        <Marker
          key={place.id}
          position={[Number(place.lat), Number(place.lng)]}
          icon={numberedIcon(index + 1)}
        >
          <Popup maxWidth={240}>
            <div style={{ fontFamily: "inherit" }}>
              <p style={{ fontWeight: 700, fontSize: "14px", marginBottom: "4px" }}>
                {index + 1}. {place.name}
              </p>
              {place.description && (
                <p style={{ fontSize: "12px", color: "#475569", marginBottom: "6px" }}>
                  {place.description}
                </p>
              )}
              <p style={{ fontSize: "11px", color: "#64748b" }}>
                {formatDateTime(place.startDate)} – {formatDateTime(place.endDate)}
              </p>
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
