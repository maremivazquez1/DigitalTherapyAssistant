/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
      "./src/**/*.{js,jsx,ts,tsx}",  // Ensures Tailwind scans all files in your src folder
    ],
    theme: {
      extend: {},
    },
    plugins: [
      require('daisyui'),  // Add DaisyUI as a plugin
    ],
  };