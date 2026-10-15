# Okul Zili Widget Uygulamasi

Bu klasor Android Studio veya GitHub Actions ile dogrudan APK'ya donusturulebilecek tam proje yapisini icerir.

## Yontem 1: Bilgisayara Hicbir Program Kurmadan (GitHub ile 2 Dakikada APK Alma)
1. [github.com](https://github.com) adresinde ucretsiz bir hesap acin ve **New Repository** diyerek bos bir proje olusturun.
2. **Add file -> Upload files** secenegine tiklayip bu ZIP icindeki tum dosya ve klasorleri surukleyip yukleyin (**Commit changes** deyin).
3. Ust menuden **Actions** sekmesine tiklayin. Otomatik olarak `Android APK Olustur` islemi baslayacaktir.
4. Yaklasik 1-2 dakika sonra islem yesil tik oldugunda icine girip en alttaki **Artifacts -> OkulZili-APK** kismina tiklayarak hazir `.apk` dosyanizi indirebilirsiniz!

## Yontem 2: Android Studio ile APK Alma
1. Bu ZIP dosyasini bir klasore cikarin.
2. Android Studio'da **Open** diyerek cikardiginiz klasoru secin.
3. Ust menuden **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** adimina tiklayin.
