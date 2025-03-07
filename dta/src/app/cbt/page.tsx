import CBTSession from "@/app/cbt/components/CBTSession";

export default function Page() {
    return (
    
    <div className="grid grid-rows-[auto_1fr_auto] h-screen">
    <header className="bg-blue-500 text-white p-3">Header</header>
    <div className="grid grid-cols-12 gap-2 p-4">
        <main className="col-span-12 bg-gray-100 p-6">
        <CBTSession/>
        </main>
    </div>
    <footer className="bg-gray-800 text-white p-3 text-center">Footer</footer>
    </div>
      
    
    
    
    )
  }