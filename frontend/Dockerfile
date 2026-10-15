# Stage 1: build
FROM node:20-alpine AS build

WORKDIR /app

# Cache de dependencias: se re-descarga solo si cambia package-lock.json
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npx ng build --configuration production

# Stage 2: runtime
FROM nginx:alpine AS runtime

COPY --from=build /app/dist/consultorio-odontologico/browser /usr/share/nginx/html

# index.html sale del webroot como template: 40-clinic-meta.sh lo regenera en cada arranque
# con título y og:* de la instancia (CLINIC_NOMBRE / CLINIC_TAGLINE). La imagen es única.
COPY docker/40-clinic-meta.sh /docker-entrypoint.d/40-clinic-meta.sh
RUN mv /usr/share/nginx/html/index.html /usr/share/nginx/index.html.template \
    && chmod 755 /docker-entrypoint.d/40-clinic-meta.sh
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
