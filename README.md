# 📱 TubeBackground - YouTube Screen-off Background Player

একটি আধুনিক অ্যান্ড্রয়েড অ্যাপ্লিকেশন যা মোবাইলের স্ক্রিন অফ (Screen off) থাকা অবস্থাতেও অথবা ব্যাকগ্রাউন্ডে ইউটিউব অডিও ও ভিডিও নিরবচ্ছিন্নভাবে চালু রাখে।

---

## ✨ ফিচারসমূহ (Features)

1. **Screen-Off Playback:** মোবাইলের স্ক্রিন লক বা অফ করলেও ইউটিউব অডিও ব্যাকগ্রাউন্ডে চলবে।
2. **Foreground Media Service:** অ্যান্ড্রয়েডের নিজস্ব MediaSession ও Foreground Service ব্যবহার করা হয়েছে।
3. **Lock Screen Controls:** ফোনের লক স্ক্রিন ও নোটিফিকেশন বার থেকে Play, Pause এবং Stop কন্ট্রোল করার সুবিধা।
4. **YouTube Visibility Bypass:** স্বয়ংক্রিয় জাভাস্ক্রিপ্ট ইনজেকশনের মাধ্যমে ইউটিউবের অটোপজ (Auto-pause) বাইপাস করা হয়।
5. **No Heavy Setup Needed:** GitHub Actions-এর মাধ্যমে সরাসরি ক্লাউডে APK বিল্ড হবে, নিজের কম্পিউটারে অ্যান্ড্রয়েড স্টুডিও ইনস্টল করার প্রয়োজন নেই।

---

## 🚀 কীভাবে GitHub-এ পুশ করবেন এবং APK ডাউনলোড করবেন

আপনার কম্পিউটারে Git অলরেডি ইনস্টল করা আছে। নিচের সহজ ধাপগুলো অনুসরণ করুন:

### ধাপ ১: GitHub-এ একটি নতুন Repository তৈরি করুন
1. [GitHub.com](https://github.com)-এ যান এবং আপনার অ্যাকাউন্টে লগইন করুন।
2. একটি নতুন রিপোজিটরি (New Repository) তৈরি করুন (যেমন নাম দিন: `TubeBackground`)।
3. রিপোজিটরিটি **Public** বা **Private** রাখতে পারেন (কোনো README বা .gitignore টিক দেওয়ার দরকার নেই)।

### ধাপ ২: টার্মিনাল/পাওয়ারশেলে কোড পুশ করুন
আপনার এই ফোল্ডারে টার্মিনাল বা PowerShell ওপেন করে নিচের কমান্ডগুলো রান করুন:

```bash
git init
git add .
git commit -m "Initial commit of TubeBackground"
git branch -M main
git remote add origin https://github.com/আপনার_ইউজারনেম/TubeBackground.git
git push -u origin main
```
*(লক্ষ্য করুন: `আপনার_ইউজারনেম` এর জায়গায় আপনার GitHub ইউজারনেম দিন)*

---

### ধাপ ৩: GitHub থেকে রেডি APK ডাউনলোড করুন
1. কোড পুশ করার সাথে সাথে GitHub-এ আপনার রিপোজিটরির **Actions** ট্যাবে যান।
2. দেখতে পাবেন **"Build TubeBackground APK"** স্বয়ংক্রিয়ভাবে রান হচ্ছে (২-৩ মিনিট সময় লাগতে পারে)।
3. বিল্ড শেষ হলে (সবুজ টিক চিহ্ন আসলে), ওই বিল্ডটিতে ক্লিক করুন।
4. পৃষ্ঠার নিচে **Artifacts** সেকশনে **`TubeBackground-APK`** নামের ফাইলটি পেয়ে যাবেন।
5. সেটিতে ক্লিক করে জিপ ফাইলটি ডাউনলোড করে নিন। আনজিপ করলেই পেয়ে যাবেন আপনার কাঙ্ক্ষিত **`TubeBackground-v1.0.0.apk`**!

---

## 🛠️ টেকনিক্যাল আর্কিটেকচার (Technical Stack)
- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0+)
- **Target SDK:** 34 (Android 14+)
- **Background Engine:** `PowerManager.WakeLock` + `ForegroundService (mediaPlayback)` + `MediaSessionCompat`
- **UI:** Custom Hardware-Accelerated WebView + Dark Theme UI
