import { defineConfig } from 'vite';
import laravel from 'laravel-vite-plugin';

export default defineConfig({
    plugins: [
        laravel({
            input: ['resources/css/styles.css', 'resources/css/styles.min.css', 'resources/js/bootstrap.js', 'resources/js/app.js', 'resources/js/bootstrap.js', 'resources/js/libs/apexcharts/dist/apexcharts.min.js', 'resources/js/libs/js/app.min.js', 'resources/js/libs/js/dashboard.js', 'resources/js/libs/js/sidebarmenu.js', 'resources/js/libs/jquery/dist/jquery.min.js'],
            refresh: true,
        }),
    ],
});
