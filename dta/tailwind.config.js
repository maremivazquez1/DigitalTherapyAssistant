/** @type {import('tailwindcss').Config} */
const daisyui = require("daisyui");

// tailwind.config.js (Optional in Tailwind v4)
module.exports = {
  content: [
    "./src/app/**/*.{js,ts,jsx,tsx}",
    "./src/components/**/*.{js,ts,jsx,tsx}",
    "./src/lib/**/*.{js,ts,jsx,tsx}",
    "./src/ui/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // Extend Tailwind’s default theme if needed
    }
  },
  plugins: [daisyui],
};

