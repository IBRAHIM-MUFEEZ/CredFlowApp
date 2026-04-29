import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyC95wyIN6jS-w_9GCRQZyOy_5G41pkUyOY",
  authDomain: "radafiq-272f9.firebaseapp.com",
  projectId: "radafiq-272f9",
  storageBucket: "radafiq-272f9.firebasestorage.app",
  messagingSenderId: "1036438568871",
  appId: "1:1036438568871:android:e1209715a26d73a659c082"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
googleProvider.addScope('https://www.googleapis.com/auth/drive.appdata');
