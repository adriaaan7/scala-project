"use client";

import { useEffect } from "react";
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// Fix Leaflet's broken default icon URLs under webpack/Next.js
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

interface Coords {
  lat: number;
  lng: number;
}

interface MapPickerProps {
  value: Coords | null;
  onChange: (coords: Coords) => void;
}

function ClickHandler({ onChange }: { onChange: (coords: Coords) => void }) {
  useMapEvents({
    click(e) {
      onChange({ lat: e.latlng.lat, lng: e.latlng.lng });
    },
  });
  return null;
}

function RecenterOnValue({ value }: { value: Coords | null }) {
  const map = useMap();
  useEffect(() => {
    if (value) map.setView([value.lat, value.lng], Math.max(map.getZoom(), 8));
  }, [value, map]);
  return null;
}

export default function MapPicker({ value, onChange }: MapPickerProps) {
  const center: [number, number] = value ? [value.lat, value.lng] : [20, 0];

  return (
    <MapContainer
      center={center}
      zoom={value ? 10 : 2}
      style={{ height: "260px", width: "100%", borderRadius: "0.5rem", cursor: "crosshair" }}
      className="z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <ClickHandler onChange={onChange} />
      <RecenterOnValue value={value} />
      {value && <Marker position={[value.lat, value.lng]} />}
    </MapContainer>
  );
}
