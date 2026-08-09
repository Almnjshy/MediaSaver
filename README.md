# MediaSaver

![Android CI](https://github.com/YOUR_USERNAME/MediaSaver/actions/workflows/android-ci.yml/badge.svg)

تطبيق Android (Kotlin + Jetpack Compose) لتحميل الفيديو/الصوت من روابط عامة (يوتيوب، تيك توك، تويتر/X، فيسبوك، ريديت، ساوندكلاود...) — **يعمل بالكامل على الجهاز**، بدون أي خادم وسيط أو اشتراك.

## كيف يعمل تقنياً
- محرك الاستخراج: [`youtubedl-android`](https://github.com/yausername/youtubedl-android)، تغليف Android لأداة `yt-dlp` مفتوحة المصدر (رخصة Unlicense، مجانية بالكامل).
- عند أول تشغيل، `YoutubeDL.init()` يفكّ ضغط ثنائيات `yt-dlp` و`ffmpeg` المرفقة داخل الـ APK نفسه محلياً على الهاتف.
- كل تحليل/تحميل بعد ذلك = اتصال مباشر من هاتف المستخدم إلى الرابط الأصلي فقط. لا يوجد أي API أو سيرفر تابع لنا في المنتصف.
- التنزيلات تعمل في الخلفية عبر WorkManager، وتُحفظ في المعرض العام عبر MediaStore (بدون أذونات تخزين خطرة على Android 10+).
- مشاركة رابط من داخل تطبيق إنستغرام/تيك توك مباشرة إلى MediaSaver مدعومة عبر Share Intent.

## فتح المشروع
1. افتح المجلد في Android Studio (Koala أو أحدث).
2. اتركه يزامن Gradle تلقائياً (يحتاج اتصال إنترنت أول مرة لتحميل التبعيات فقط، ليس بعد ذلك).
3. شغّل على جهاز حقيقي أو محاكي بـ API 24+.

## ملاحظات مهمة وصادقة
- بعض المنصات (خصوصاً إنستغرام وفيسبوك) تراقب وتحظر الاستخراج بشكل نشط وتغيّر بنيتها التقنية باستمرار؛ عند تعطّل منصة معينة، الحل هو تحديث مكتبة `youtubedl-android` إلى أحدث إصدار (تُحدَّث بشكل شبه أسبوعي من المجتمع).
- نشر هذا النوع من التطبيقات على Google Play غير مضمون القبول، لأنه يخالف سياسات Play بخصوص تحميل المحتوى من منصات أخرى. يعمل بشكل موثوق كـ APK مباشر (sideload) أو عبر متجر بديل مثل F-Droid/Aptoide/GitHub Releases.
- استخدم التطبيق فقط لتنزيل محتوى تملك حقوقه أو مسموح لك قانونياً بتنزيله.

## رفعه على GitHub

```bash
cd MediaSaver
git init
git add .
git commit -m "Initial commit: MediaSaver Android app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/MediaSaver.git
git push -u origin main
```

استبدل `YOUR_USERNAME` باسمك على GitHub (وفي رابط الـ badge بالأعلى أيضاً).

## ورك فلو GitHub Actions المرفق

المشروع يحتوي على مسارين جاهزين تحت `.github/workflows/`:

### 1. `android-ci.yml` — بناء واختبار تلقائي
يعمل تلقائياً عند كل `push` أو `pull request` على `main`:
- يشغّل Lint واختبارات الوحدة
- يبني نسخة Debug APK
- يرفعها كـ **Artifact** يمكنك تحميله من تبويب Actions في المستودع (بدون الحاجة لإصدار رسمي)

### 2. `android-release.yml` — إصدار رسمي
يعمل تلقائياً عند دفع Tag بصيغة إصدار، مثلاً:
```bash
git tag v1.0.0
git push origin v1.0.0
```
يبني نسخة Release APK وينشئ **GitHub Release** تلقائياً مع إرفاق ملف الـ APK جاهز للتحميل المباشر.

**لتوقيع الإصدار رسمياً (اختياري):** أضف هذه الأسرار (Secrets) في إعدادات المستودع
(`Settings → Secrets and variables → Actions`):
- `KEYSTORE_BASE64` — ملف الـ keystore الخاص بك مُحوَّل إلى base64 (`base64 -w0 release.keystore`)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

بدون هذه الأسرار، الورك فلو يبني الـ APK موقّعاً بتوقيع Debug تلقائياً (يعمل للتجربة، لكن غير مناسب للنشر الرسمي على المتاجر).

## حجم التطبيق

مكتبة `youtubedl-android` بتشحن مفسّر Python كامل + ثنائيات yt-dlp وFFmpeg — لازم نسخة منفصلة لكل معمارية معالج (ABI)، وده بيضخّم الحجم بسرعة لو مش متحكّم فيه. المشروع مضبوط حالياً على:
- **معماريتين بس** (`armeabi-v7a`, `arm64-v8a`) — بيغطوا الغالبية العظمى من الأجهزة الحقيقية؛ استبعدت `x86`/`x86_64` لأنهم غالباً للمحاكيات فقط.
- **حزم منفصلة لكل معمارية** (`splits.abi`) — بدل APK واحد ضخم فيه كل المعماريات، هتلاقي في المخرجات ملف APK صغير لكل معمارية + نسخة "شاملة" (universal) للمشاركة المباشرة.
- **تعبئة حديثة مضغوطة** للمكتبات الأصلية (بدون `useLegacyPackaging`) بدل التخزين غير المضغوط.

لو محتاج تقلل الحجم أكتر: انشر عبر **Android App Bundle** (`./gradlew bundleRelease`) بدل APK مباشر — جوجل بلاي وقتها بيولّد لكل جهاز نسخة تحتوي بس على المعمارية بتاعته تلقائياً.

## هيكل المشروع
```
app/src/main/java/com/mediasaver/app/
├── App.kt                     # تهيئة yt-dlp/ffmpeg محلياً
├── MainActivity.kt            # نقطة الدخول + استقبال روابط المشاركة
├── data/DownloadDatabase.kt   # Room: سجل التنزيلات محلياً
├── domain/MediaExtractor.kt   # محرك الاستخراج/التنزيل (كله on-device)
├── worker/DownloadWorker.kt   # تنزيل بالخلفية عبر WorkManager
├── util/UrlDetector.kt        # كشف المنصة من الرابط
├── util/MediaStoreSaver.kt    # حفظ في المعرض العام
├── di/AppModule.kt            # حقن التبعيات (Hilt)
└── ui/home/                   # واجهة Compose (إدخال الرابط + اختيار الجودة + السجل)
```
