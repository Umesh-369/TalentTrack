"use client";

import { useEffect, useRef } from "react";
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

export default function Stats() {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Register scroll trigger on client-side
    gsap.registerPlugin(ScrollTrigger);

    const counters = containerRef.current?.querySelectorAll(".stat-counter");
    if (!counters) return;

    counters.forEach((counter) => {
      const targetVal = parseFloat(counter.getAttribute("data-target") || "0");
      const isPercent = counter.getAttribute("data-percent") === "true";
      const isMins = counter.getAttribute("data-mins") === "true";
      
      const obj = { val: 0 };

      gsap.to(obj, {
        val: targetVal,
        duration: 2,
        ease: "power2.out",
        scrollTrigger: {
          trigger: counter,
          start: "top 85%",
          toggleActions: "play none none none",
        },
        onUpdate: () => {
          if (isPercent) {
            counter.textContent = obj.val.toFixed(1) + "%";
          } else if (isMins) {
            counter.textContent = Math.round(obj.val) + "m";
          } else {
            counter.textContent = Math.round(obj.val).toLocaleString() + "+";
          }
        },
      });
    });
  }, []);

  return (
    <section id="stats" ref={containerRef} className="py-24 border-y border-zinc-200 bg-zinc-50/50 relative">
      <div className="mx-auto max-w-7xl px-6 grid grid-cols-1 md:grid-cols-3 gap-12 text-center">
        {/* Stat 1 */}
        <div className="flex flex-col items-center">
          <span 
            className="stat-counter text-5xl sm:text-6xl font-extrabold text-zinc-900 tracking-tight bg-gradient-to-r from-sky-600 to-cyan-600 bg-clip-text text-transparent mb-2" 
            data-target="15000"
          >
            0+
          </span>
          <span className="text-zinc-600 text-sm font-semibold uppercase tracking-wider">Candidates Applications Processed</span>
        </div>

        {/* Stat 2 */}
        <div className="flex flex-col items-center">
          <span 
            className="stat-counter text-5xl sm:text-6xl font-extrabold text-zinc-900 tracking-tight bg-gradient-to-r from-cyan-600 to-sky-500 bg-clip-text text-transparent mb-2" 
            data-target="100"
            data-percent="true"
          >
            0.0%
          </span>
          <span className="text-zinc-600 text-sm font-semibold uppercase tracking-wider">Tenant Database Isolation</span>
        </div>

        {/* Stat 3 */}
        <div className="flex flex-col items-center">
          <span 
            className="stat-counter text-5xl sm:text-6xl font-extrabold text-zinc-900 tracking-tight bg-gradient-to-r from-sky-500 to-sky-700 bg-clip-text text-transparent mb-2" 
            data-target="15"
            data-mins="true"
          >
            0m
          </span>
          <span className="text-zinc-600 text-sm font-semibold uppercase tracking-wider">Max Token Expiration Guarantee</span>
        </div>
      </div>
    </section>
  );
}
