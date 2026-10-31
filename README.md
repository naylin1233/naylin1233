# Remix Recap MM (မြန်မာအသံနှင့် စာတန်းထိုး Video Recap အက်ပ်)

မြန်မာဘာသာဖြင့် Video များ၊ SRT Subtitle ဖိုင်များကို AI အသုံးပြု၍ အကျဉ်းချုပ် (Recap) ပြုလုပ်ပေးပြီး သဘာဝကျသော မြန်မာအသံထွက်ဖြင့် အသံသွင်း/ပြန်လည်ဖတ်ပြပေးနိုင်သည့် ခေတ်မီ Android Jetpack Compose Application ဖြစ်ပါသည်။

---

## 🌟 အဓိက လုပ်ဆောင်ချက်များ (Key Features)

1. **AI Video Recap & Script Generator (Gemini AI Logic)**:
   - ဗီဒီယိုဇာတ်လမ်း သို့မဟုတ် SRT စာတန်းထိုးဖိုင်များကို မြန်မာဘာသာဖြင့် စိတ်ဝင်စားဖွယ် ဇာတ်ညွှန်း (Narrative Script) နှင့် အကျဉ်းချုပ်အဖြစ် ဖန်တီးပေးခြင်း။
2. **Myanmar Speech & Dubbing Engine (အသံထွက် ချိန်ညှိမှုစနစ်)**:
   - အသံနေအထား (Voice Tones): ဇာတ်လမ်းပြောသူ (Storyteller)၊ အမျိုးသား/အမျိုးသမီး အသံ (Male/Female Narrator)၊ သတင်းဖတ်သူ (News Anchor) စသည်ဖြင့် စိတ်ကြိုက်ရွေးချယ်နိုင်ခြင်း။
   - အသံအမြန်နှုန်း (Speed) နှင့် အသံမြင့်/နိမ့် (Pitch) အသေးစိတ်ချိန်ညှိနိုင်ခြင်း။
3. **Synchronized Video Player & SRT Editor**:
   - အသံနှင့် စာတန်းထိုးချိန်ကို တစ်ပြိုင်နက်ကြည့်ရှုနိုင်သော ဗီဒီယို Player။
   - SRT Cue များကို အချိန်နှင့်တပြေးညီ တည်းဖြတ်ပြင်ဆင်နိုင်သော SRT Editor။
4. **Cloud Database & Multi-device Sync (Firebase Firestore)**:
   - သိမ်းဆည်းထားသော Recap များကို မည်သည့်စက်ပစ္စည်းမှမဆို ပြန်လည်ရယူနိုင်ခြင်း။
   - Row-level zero-trust Security Rules ဖြင့် လုံခြုံစွာ ကာကွယ်ထားခြင်း။
5. **Secure Authentication**:
   - Google Sign-In (Android Jetpack Credential Manager) ဖြင့် လွယ်ကူလျင်မြန်စွာ အကောင့်ဝင်ရောက်နိုင်ခြင်း။

---

## 📁 Source Code ဖွဲ့စည်းပုံ (Project Structure)

```text
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml              # အက်ပ်ခွင့်ပြုချက်များနှင့် Activity ဖွဲ့စည်းပုံ
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt               # Root Activity (App Check & Auth Flow)
│   │   │   ├── ai/
│   │   │   │   └── FirebaseAiService.kt     # Gemini AI Logic API ဆက်သွယ်မှု
│   │   │   ├── data/
│   │   │   │   ├── model/                    # Data Models (RecapItem, SrtCue)
│   │   │   │   ├── repository/               # Firestore Data Layer (RecapRepository)
│   │   │   │   └── local/                    # Room Database (Offline Cache)
│   │   │   ├── ui/
│   │   │   │   ├── MainViewModel.kt          # UI State Management (MVVM)
│   │   │   │   ├── auth/                     # Google Sign-In Credential Manager
│   │   │   │   ├── screens/                  # Compose UI Screens (HomeScreen)
│   │   │   │   ├── components/               # Player, SRT Viewer, Dialogs
│   │   │   │   └── theme/                    # Material 3 Design System (Colors, Typography)
│   │   │   └── util/                         # MyanmarSpeechEngine, SrtParser, AppCheckHelper
│   │   └── res/                              # Drawables, Strings, Adaptive App Launcher Icons
│   └── build.gradle.kts                      # App-level dependencies & build config
├── firestore.rules                           # Firebase Firestore လုံခြုံရေး စည်းမျဉ်းများ
├── firebase.json                             # Firebase CLI & Emulator ပြင်ဆင်ချက်
├── firestore.test.js                         # JavaScript Rules Validation Unit Tests
├── settings.gradle.kts                       # Root project settings
└── build.gradle.kts                          # Project-level plugins & repositories
```

---

## 🚀 GitHub တင်ရန် နှင့် Local Android Studio တွင် Run ရန် လမ်းညွှန်

### နည်းလမ်း ၁ - Google AI Studio မှ တိုက်ရိုက် GitHub သို့ Push လုပ်ခြင်း (အလွယ်ဆုံးနည်း)
1. AI Studio ညာဘက်အပေါ်ထောင့်ရှိ **Menu (သို့မဟုတ် Project Settings/Export)** ကိုနှိပ်ပါ။
2. **"Push to GitHub"** (သို့မဟုတ် **"Export to GitHub"**) ကို ရွေးချယ်ပြီး သင်၏ GitHub Repository သို့ တိုက်ရိုက်တင်နိုင်ပါသည်။
3. အကယ်၍ ZIP ဖိုင်အဖြစ် ဒေါင်းလုဒ်ဆွဲလိုပါက **"Download as ZIP"** ကို နှိပ်၍ မိမိကွန်ပျူတာထဲသို့ ရယူနိုင်ပါသည်။

### နည်းလမ်း ၂ - Git CLI ဖြင့် GitHub သို့ တင်ခြင်း
```bash
git init
git add .
git commit -m "Initial commit of Remix Recap MM"
git branch -M main
git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPOSITORY_NAME>.git
git push -u origin main
```

---

## 💻 Android Studio တွင် ဖွင့်လှစ် အသုံးပြုခြင်း

1. **Android Studio** (Hedgehog 2023.1.1 သို့မဟုတ် နောက်ဆုံးထွက်ဗားရှင်း) တွင် `Open Project` နှိပ်၍ ဤ Folder ကို ရွေးချယ်ပါ။
2. **JDK Version**: Project Structure တွင် JDK 17 သို့မဟုတ် JDK 21 ကို သတ်မှတ်ပေးပါ။
3. **Gradle Sync**: `Sync Project with Gradle Files` ကို နှိပ်ပြီး dependencies များ ဒေါင်းလုဒ်ပြီးသည်အထိ စောင့်ပါ။
4. **Run Application**: Emulator သို့မဟုတ် USB Debugging ဖွင့်ထားသော Android Device (API 24+) ပေါ်တွင် `Run 'app'` (Shift + F10) နှိပ်၍ စမ်းသပ်နိုင်ပါသည်။

---

## 🧪 စမ်းသပ်မှုများ (Unit & Integration Tests)

ဤပရောဂျက်တွင် Robolectric Test Suite နှင့် Firestore Security Rules Unit Tests များ အပြည့်အစုံပါဝင်သည်-

```bash
# Kotlin Unit & Robolectric Tests များ Run ရန်:
gradle :app:testDebugUnitTest

# Firebase Firestore Security Rules Test များ Run ရန်:
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
