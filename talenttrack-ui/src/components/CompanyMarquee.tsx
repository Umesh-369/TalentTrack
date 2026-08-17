"use client";

import { Cpu, Globe, Rocket, Compass, Anchor, Terminal } from "lucide-react";

export default function CompanyMarquee() {
  const brands = [
    { icon: <Cpu className="h-6 w-6" />, name: "ApexCore" },
    { icon: <Globe className="h-6 w-6" />, name: "NovaLink" },
    { icon: <Rocket className="h-6 w-6" />, name: "StellarWeb" },
    { icon: <Compass className="h-6 w-6" />, name: "Vanguard" },
    { icon: <Anchor className="h-6 w-6" />, name: "Portside" },
    { icon: <Terminal className="h-6 w-6" />, name: "DevLogic" },
  ];

  // Duplicate for seamless looping
  const doubleBrands = [...brands, ...brands, ...brands, ...brands];

  return (
    <div className="w-full py-12 bg-zinc-50 border-b border-zinc-200 overflow-hidden relative">
      <div className="absolute inset-y-0 left-0 w-24 bg-gradient-to-r from-zinc-50 to-transparent z-10 pointer-events-none" />
      <div className="absolute inset-y-0 right-0 w-24 bg-gradient-to-l from-zinc-50 to-transparent z-10 pointer-events-none" />
      
      <p className="text-center text-xs font-semibold uppercase tracking-widest text-zinc-500 mb-8">
        Hiring Teams at Top Engineering Companies Trust TalentTrack
      </p>

      {/* Marquee Container */}
      <div className="relative flex overflow-x-hidden">
        <div className="flex marquee-track gap-12 items-center">
          {doubleBrands.map((brand, idx) => (
            <div 
              key={idx} 
              className="flex items-center gap-3 text-zinc-550 hover:text-zinc-900 transition-colors duration-300 font-medium px-4"
            >
              {brand.icon}
              <span className="text-sm tracking-wider font-semibold">{brand.name}</span>
            </div>
          ))}
        </div>
      </div>

      <style jsx global>{`
        @keyframes marquee {
          0% { transform: translateX(0); }
          100% { transform: translateX(-50%); }
        }
        .marquee-track {
          display: flex;
          width: max-content;
          animation: marquee 30s linear infinite;
        }
        .marquee-track:hover {
          animation-play-state: paused;
        }
      `}</style>
    </div>
  );
}
