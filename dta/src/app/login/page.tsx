import Link from 'next/link';

export default function Page() {
    return (
// a welcoming login page
    <div className="flex h-screen flex-col items-center justify-center gap-4 bg-gray-100 p-4">
      <h1 className="text-4xl font-bold text-blue-600">Welcome to DTA</h1>
      <p className="text-lg text-gray-700">Your personal mental health companion</p>
      <div className="w-full max-w-md p-6 bg-white rounded-lg shadow-md">
        <h2 className="text-2xl font-bold text-center text-gray-800">Login</h2>
        <form className="space-y-6">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700">Email</label>
            <input
              type="email"
              id="email"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter your email"
              required
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700">Password</label>
            <input
              type="password"
              id="password"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter your password"
              required
            />
          </div>
          <div>
            <button
              type="submit"
              className="w-full py-2 px-4 bg-blue-500 text-white font-semibold rounded-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Login
            </button>
          </div>
        </form>
        <p className="text-sm text-center text-gray-600">
          {`Don't have an account?`} <Link href="/register" className="text-blue-500 hover:underline">Register</Link>
        </p>
      </div>
    </div>


    )
  }