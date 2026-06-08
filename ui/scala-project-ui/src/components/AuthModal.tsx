"use client";

import { useState, FormEvent, useEffect } from "react";
import { X, Eye, EyeOff } from "lucide-react";
// ZAKTUALIZOWANA ŚCIEŻKA:
import { authService } from "../services/api";

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialView: "login" | "register";
}

export default function AuthModal({ isOpen, onClose, initialView }: AuthModalProps) {
  const [isLoginView, setIsLoginView] = useState(initialView === "login");
  
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    setIsLoginView(initialView === "login");
    setUsername("");
    setPassword("");
    setError("");
    setShowPassword(false);
  }, [isOpen, initialView]);

  if (!isOpen) return null;

  const isFormValid = username.trim().length > 0 && password.trim().length > 0;

  const switchView = () => {
    setIsLoginView(!isLoginView);
    setError("");
    setUsername("");
    setPassword("");
    setShowPassword(false);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!isFormValid) return;
    
    setIsLoading(true);
    setError("");

    try {
      if (isLoginView) {
        await authService.login(username, password);
        console.log("Success (Logged in)");
      } else {
        const data = await authService.register(username, password);
        console.log("Success (Registered):", data);
        switchView();
        return; 
      }
      onClose();
    } catch (err: any) {
      setError(err.message || "Something went wrong.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 relative animate-in fade-in zoom-in duration-200">
        <button 
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 cursor-pointer"
        >
          <X className="h-6 w-6" />
        </button>
        
        <h2 className="text-3xl font-bold text-slate-900 mb-2">
          {isLoginView ? "Welcome back" : "Create an account"}
        </h2>
        <p className="text-slate-500 mb-6">
          {isLoginView ? "Enter your credentials to access your trips." : "Start planning your next adventure today."}
        </p>

        {error && (
          <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Username</label>
            <input 
              type="text" 
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 text-slate-900 placeholder-slate-400 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
              placeholder="e.g. travelguru99"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Password</label>
            <div className="relative">
              <input 
                type={showPassword ? "text" : "password"}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 text-slate-900 placeholder-slate-400 rounded-lg pl-4 pr-10 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
                placeholder="••••••••"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer"
              >
                {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
          </div>

          <button 
            type="submit" 
            disabled={isLoading || !isFormValid}
            className="w-full bg-blue-600 text-white rounded-lg px-4 py-3 font-semibold hover:bg-blue-700 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed mt-2"
          >
            {isLoading ? "Processing..." : (isLoginView ? "Log in" : "Sign up")}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-slate-500">
          {isLoginView ? "Don't have an account? " : "Already have an account? "}
          <button 
            onClick={switchView}
            className="text-blue-600 font-semibold hover:underline cursor-pointer"
          >
            {isLoginView ? "Sign up" : "Log in"}
          </button>
        </div>
      </div>
    </div>
  );
}