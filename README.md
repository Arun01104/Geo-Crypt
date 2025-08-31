# 🌍 Geo-Crypt (GeoLockedFileBackend)

**Geo-Crypt** is a **location-based file encryption system** built with Spring Boot.  
Files are encrypted and can only be decrypted when the user is physically near the **original coordinates** where encryption took place.  
This ensures **geo-locking** of sensitive data, adding an extra layer of security beyond passwords.

---

## ✨ Features
- 🔑 **JWT-based Authentication** (Register/Login).
- 📂 **File Encryption & Decryption** tied to user’s **GPS coordinates**.
- 📦 **MinIO Object Storage** integration for secure file storage.
- 🗄 **MySQL Database** for user & file metadata management.
- 🛰 **Coordinate Tolerance System** → allows small deviations in GPS accuracy.
- 📜 **File Logs** → Track encryption & decryption attempts.
- 🧑‍💻 **Postman Collection** included for testing all APIs.

---

## 🛠 Tech Stack
- **Backend:** Spring Boot 3.5.4, Java 21  
- **Database:** MySQL  
- **Storage:** MinIO (S3-compatible)  
- **Security:** Spring Security + JWT  
- **Build Tool:** Maven  

---

## ⚙️ Setup Instructions

### 1️⃣ Clone the Repository
```bash
git clone https://github.com/your-username/Geo-Crypt.git
cd Geo-Crypt
```

### 2️⃣ Configure Database & MinIO
- Update `application.properties` with:
  - MySQL username, password, DB name
  - MinIO endpoint, access key, secret key

Example:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/geocrypt
spring.datasource.username=root
spring.datasource.password=yourpassword

minio.url=http://localhost:9000
minio.access.key=minioadmin
minio.secret.key=minioadmin
minio.bucket=geocrypt-files
```

### 3️⃣ Run MinIO Server
```bash
.\minio.exe server C:\minio\data --console-address ":9001"
```
- Access MinIO console at: [http://localhost:9001](http://localhost:9001)

### 4️⃣ Run the Application
```bash
mvn spring-boot:run
```
App runs at → [http://localhost:8080](http://localhost:8080)

---

## 🚀 API Endpoints

### 🔐 Authentication
- `POST /api/auth/register` → Register new user
- `POST /api/auth/login` → Login and receive JWT token

### 📂 File Operations
- `POST /api/files/encrypt` → Encrypt & upload file (requires location)
- `POST /api/files/decrypt` → Decrypt file (only if location matches)

### 📜 Logs
- `GET /api/files/logs` → Fetch all encryption/decryption logs

> Import the **Postman Collection** from `/postman/GeoCrypt.postman_collection.json` to test all endpoints.

---

## 🗺 How It Works
1. User registers & logs in → receives JWT token.  
2. While encrypting, the user uploads a file + their **current latitude/longitude**.  
3. System generates an **AES key from coordinates** and encrypts the file.  
4. File is stored in **MinIO**, metadata saved in **MySQL**.  
5. To decrypt, the user must be at (or near) the **original location**.  
6. If coordinates match (within tolerance), decryption succeeds.  

---

## 📌 Future Enhancements
- 🗺 **Map-based coordinate selection (frontend integration).**
- 📱 **Mobile app integration** for real-world usage.
- 🛰 **Radius-based decryption tolerance** instead of fixed decimals.
- 👨‍💻 **Admin dashboard** for monitoring users & logs.
- 🌐 **Multi-language support** (English, Tamil, etc.).

---

## 📜 License
This project is licensed under the **MIT License** – feel free to use, modify, and distribute.

---

🔥 With **Geo-Crypt**, your files are secure not just by password, but also by **where you are**.  
