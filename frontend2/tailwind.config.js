/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#1890ff',
        'primary-dark': '#096dd9',
        'primary-light': '#40a9ff',
        'sidebar-dark': '#001529',
        'bg-gray': '#f0f2f5',
        'card-bg': '#ffffff',
        'text-main': 'rgba(0, 0, 0, 0.85)',
        'text-secondary': 'rgba(0, 0, 0, 0.45)',
        'border-color': '#f0f0f0',
      },
      screens: {
        'xs': '480px',
        'sm': '576px',
        'md': '768px',
        'lg': '992px',
        'xl': '1200px',
        '2xl': '1600px',
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
      boxShadow: {
        'sm': '0 1px 2px rgba(0, 0, 0, 0.06)',
        'md': '0 4px 12px rgba(0, 0, 0, 0.08)',
        'lg': '0 8px 24px rgba(0, 0, 0, 0.12)',
      },
      borderRadius: {
        'sm': '6px',
        'md': '8px',
        'lg': '12px',
      }
    }
  },
  plugins: []
}
