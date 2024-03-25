-keep class com.google.android.material.** {*;}
#-keep class androidx.** {*;}//why?
#-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**