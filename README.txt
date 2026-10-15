CARA MEMBUAT APK TANPA ANDROID STUDIO (GitHub Actions)
1. Buat akun di github.com, lalu buat repository baru (New repository).
2. Unggah SEMUA isi folder ini ke repository (Add file > Upload files).
   Pastikan folder .github ikut terunggah. Jika tidak bisa, buat manual:
   Add file > Create new file, nama: .github/workflows/build-apk.yml
   lalu tempel isi file build-apk.yml.
3. Ubah BASE_URL di app/src/main/java/com/nfclink/app/MainActivity.java
   menjadi alamat HTTPS halaman profilmu.
4. Buka tab Actions > Build APK > Run workflow (atau otomatis berjalan saat unggah).
5. Tunggu +- 3-5 menit sampai centang hijau, buka run tersebut,
   lalu unduh "NFC-Link-APK" di bagian Artifacts. Ekstrak ZIP-nya, hasilnya app-debug.apk.
6. Kirim ke HP dan install (izinkan "install dari sumber tidak dikenal").

CARA LAIN: buka folder ini di Android Studio > Build > Build APK(s).
