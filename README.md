
# 🌍 **UniversityMap (MapMate)**  
An Android application built in Java to help users navigate easily within the university.  

---

## ✨ **Features**  

- 📍 **University Navigation:** Effortlessly locate and navigate through the university premises.  
- 🗺️ **Interactive Map:** Displays key areas of the university.  
- 🔒 **Secure Coordinates Access:** Uses secure secrets to manage sensitive information like university coordinates.  

---

## 📥 **APK Download**  

👉 You can download the APK file for this project here:  
[**Download MapMate APK**](https://github.com/srihari-976/UniversityMap/blob/main/app/build/outputs/apk/androidTest/debug/MapMate.apk)

---

## 🔑 **Accessing Secrets**  

The app fetches university coordinates securely using GitHub Codespaces secrets.  

### 🔧 **Code Snippet to Access Secrets**  

#### 🖥️ _In Java_  
```java
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch university coordinates from environment variables
        String universityCoordinates = System.getenv("CO_ORDINATES__OF__UNIVERSITY__FOR__MAP__FRAGMENT");
        if (universityCoordinates != null) {
            Log.d("UniversityMap", "Coordinates: " + universityCoordinates);
        } else {
            Log.d("UniversityMap", "Coordinates not found!");
        }
    }
}
```

---

### 📋 **Steps to Add and Access Secrets**  

1. **Add Secrets:**  
   - Navigate to your repository on **GitHub**.  
   - Go to **Settings** > **Secrets and variables** > **Codespaces**.  
   - Click **New secret** and add a secret:  
     - Name: `CO_ORDINATES__OF__UNIVERSITY__FOR__MAP__FRAGMENT`  
     - Value: *Your university's coordinates.*  

2. **Access Secrets in Codespaces:**  
   - Open a Codespace for your repository.  
   - The secret will be available as an environment variable. Verify it with:  
     ```bash
     echo $CO_ORDINATES__OF__UNIVERSITY__FOR__MAP__FRAGMENT
     ```

3. **Integrate Secrets in Your Code:**  
   - Use the Java snippet above to securely access the coordinates.

---

## 🛠️ **How to Build and Run**  

1. Clone the repository:  
   ```bash
   git clone https://github.com/srihari-976/UniversityMap.git
   ```
2. Open the project in **Android Studio**.  
3. Build and run the app on an Android device or emulator.

---

## 🤝 **Contributions**  

🎉 Contributions are always welcome! Feel free to:  
- Fork the repository  
- Submit pull requests  
- Share your suggestions and improvements!  

---
