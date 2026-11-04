/**
 * Script de inicialización de datos para Firebase Local Emulator Suite.
 * Puedes ejecutar este script con `node seed.js` para cargar datos iniciales.
 */

const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getAuth } = require('firebase-admin/auth');

// Configurar variables de entorno para conectarse a los Emuladores Locales
process.env.FIRESTORE_EMULATOR_HOST = 'localhost:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = 'localhost:9099';

initializeApp({ projectId: 'demo-mobile-app' });

const db = getFirestore();
const auth = getAuth();

async function seed() {
  console.log('🚀 Sembrando datos de prueba en Firebase Emulators...');

  try {
    // 1. Crear Usuario de Prueba
    const user = await auth.createUser({
      uid: 'user-demo-123',
      email: 'usuario@ejemplo.com',
      password: 'password123',
      displayName: 'Usuario Móvil Demo',
    });
    console.log(`✅ Usuario creado: ${user.email} (UID: ${user.uid})`);

    // 2. Crear Documentos en Firestore (Colección de Perfiles y Productos)
    await db.collection('users').doc(user.uid).set({
      nombre: 'Usuario Móvil Demo',
      email: 'usuario@ejemplo.com',
      rol: 'cliente',
      fechaRegistro: new Date().toISOString(),
    });

    const productosRef = db.collection('products');
    await productosRef.doc('prod-001').set({
      nombre: 'Smartphone Pro Max',
      precio: 999.99,
      categoria: 'Electrónica',
      stock: 50,
      destacado: true,
    });

    await productosRef.doc('prod-002').set({
      nombre: 'Auriculares Inalámbricos',
      precio: 149.50,
      categoria: 'Accesorios',
      stock: 120,
      destacado: false,
    });

    console.log('✅ Colecciones "users" y "products" sembradas exitosamente en Firestore.');
    console.log('🎉 Inicialización completada.');
  } catch (error) {
    console.error('❌ Error al sembrar datos:', error);
  }
}

seed();
