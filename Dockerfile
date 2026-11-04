# Imagen base con Node.js 20 (Alpine Linux)
FROM node:20-alpine

# Firebase Emulators requieren Java Runtime Environment (JRE)
RUN apk add --no-cache openjdk17-jre bash curl

# Instalar Firebase CLI globalmente
RUN npm install -g firebase-tools

# Directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Firebase
COPY firebase.json .firebaserc ./

# Puertos expuestos para Firebase Local Emulator Suite
# 4000: Emulator Suite UI
# 8080: Firestore Emulator
# 9099: Authentication Emulator
# 9000: Realtime Database Emulator
# 9199: Cloud Storage Emulator
EXPOSE 4000 8080 9099 9000 9199

# Comando por defecto para iniciar los emuladores
# Importa datos de ./firebase-data si existen y los guarda al detener el contenedor
CMD ["firebase", "emulators:start", "--project", "demo-mobile-app", "--import=./firebase-data", "--export-on-exit=./firebase-data"]
