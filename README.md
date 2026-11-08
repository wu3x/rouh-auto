<div align="center">

<img src="5914CB83-92AF-4E02-968C-9AAF7449CA41.jpeg" width="760" alt="ROUH AUTO — banner" />

### مشغل لانشر لشاشات السيارات بنظام أندرويد البديلة

**A modern, landscape-first Android car launcher**

</div>

<div align="center">

[![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Arabic%20%2F%20RTL-1787D4)](#-features--المميزات)
[![Build](https://github.com/wu3x/rouh-auto/actions/workflows/build.yml/badge.svg)](https://github.com/wu3x/rouh-auto/actions/workflows/build.yml)
[![Status](https://img.shields.io/badge/Status-Active%20development-F5A623)](#-project-status--حالة-المشروع)
[![Instagram](https://img.shields.io/badge/Instagram-%40wu3x-E4405F?logo=instagram&logoColor=white)](https://www.instagram.com/wu3x/)

<p align="center">
  <a href="https://github.com/wu3x/rouh-auto/releases/download/latest-signed/rouh-auto-release.apk"><img alt="Download ROUH Auto APK" src="https://img.shields.io/badge/⬇_DOWNLOAD-ROUH_AUTO_APK-238636?style=for-the-badge&logo=android&logoColor=white" /></a>
</p>

[**🛠️ حالة البناء**](https://github.com/wu3x/rouh-auto/actions/workflows/build.yml) ·
[**📂 الملفات**](app/src/main/java/org/wpfy/carlauncher)

</div>

<div align="center">

<img src="ROUH_Auto_Preview.gif" width="760" alt="ROUH Auto — animated launcher preview" />

</div>

---

## ✨ About / عن التطبيق

**ROUH Auto** واجهة رئيسية مصممة لشاشات السيارات بنظام أندرويد البديلة، مع تركيز على سهولة اللمس، الاتجاه الأفقي، والتجربة العربية.

ROUH Auto is a native Android home-screen replacement built with **Kotlin + Jetpack Compose** for aftermarket head units. It is optimized for landscape layouts and dashboard-friendly controls.

> [!IMPORTANT]
> **تطبيق تجريبي قيد التطوير.** بعض الوظائف تعتمد على نوع الشاشة وصلاحيات أندرويد وتطبيقات المصنع. التطبيق لا يقرأ أعطال السيارة أو بيانات CANBUS تلقائيًا دون واجهة متوافقة.

## 🚘 Features / المميزات

| الميزة | الوصف |
| :--- | :--- |
| 🏠 **لوحة رئيسية** | واجهة أفقية مناسبة لشاشة السيارة، مع عناصر الحالة والموسيقى والخريطة المصغّرة |
| 🎵 **مشغل الوسائط** | يعرض جلسات الوسائط المتاحة ومعلومات الأغاني وأزرار التحكم عند دعم التطبيق المصدر |
| 📱 **مكتبة التطبيقات** | بحث، فتح سريع، عرض التطبيقات، وتثبيت المفضلة |
| 🗺️ **خريطة مصغّرة** | عرض موقع وخريطة داخل الواجهة؛ يتطلب توفر GPS واتصال مناسب |
| 🔔 **الإشعارات** | عرض إشعارات التطبيقات بعد منح صلاحية الوصول |
| ⚙️ **التخصيص** | إعدادات المظهر والواجهة والتحية وأسلوب الاستخدام |

## 📲 Installation / التثبيت

1. افتح [**تحميل ROUH Auto APK الموقّع مباشرة**](https://github.com/wu3x/rouh-auto/releases/download/latest-signed/rouh-auto-release.apk).
2. سيبدأ تنزيل ملف **rouh-auto-release.apk** مباشرة.
3. ثبّته على شاشة أندرويد، واسمح بالتثبيت من المصدر عند الحاجة.
4. امنح الأذونات المطلوبة للميزات التي ترغب باستخدامها، واختر **ROUH Auto** كتطبيق الشاشة الرئيسية إذا رغبت.

**Compatibility / التوافق:** Android **9+** (API 28+). صُمم أساسًا للشاشات الأفقية؛ بعض خصائص البلوتوث وMCU وCANBUS تختلف حسب المصنع.

> [!NOTE]
> **Bluetooth audio:** عرض اسم الأغنية لا يعني بالضرورة تفعيل مسار الصوت. بعض الشاشات تتطلب تطبيق «موسيقى البلوتوث» الأصلي لتشغيل الصوت عبر MCU.

## 🧰 Tech stack / التقنيات

| Component | Technology |
| :--- | :--- |
| Language | Kotlin |
| UI | Jetpack Compose / Material 3 |
| Platform | Android, minSdk 28 |
| Persistence | Android DataStore / local preferences |
| Background tasks | Kotlin Coroutines |
| Build | Gradle + GitHub Actions |
| Distribution | GitHub Releases (signed release APK + debug test APK) |

## 🛠️ Build from source / بناء المشروع

Requirements: **JDK 17**, Android SDK with the configured compile SDK, and Gradle wrapper.

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/`

> [!WARNING]
> The GitHub Actions build obtains **Thmanyah Sans** font resources from a separate private release. Those font binaries are **not included in the repository**. A local build may require compatible font resources or adjustments to the font configuration.

## 🧭 Project status / حالة المشروع

- **In development:** UI, media integration, and launcher customization are evolving.
- **Testing:** Validate on the actual head unit; an Android emulator cannot reproduce manufacturer MCU integrations.


---

<div align="center">

**ROUH Auto — Built for the road. Designed for your dashboard.**

<sub>Made with Kotlin • Jetpack Compose • Android</sub>

</div>
