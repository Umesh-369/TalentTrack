import Navbar from "@/components/Navbar";
import Hero from "@/components/Hero";
import CompanyMarquee from "@/components/CompanyMarquee";
import Stats from "@/components/Stats";
import Features from "@/components/Features";
import Footer from "@/components/Footer";

export default function Home() {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <main className="flex-grow">
        <Hero />
        <CompanyMarquee />
        <Stats />
        <Features />
      </main>
      <Footer />
    </div>
  );
}
